package app.aaps.implementation.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.RM
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.di.ApplicationScope
import app.aaps.core.interfaces.iob.IobCobCalculator
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.defs.determineCorrectBolusStepSize
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.objects.constraints.ConstraintObject
import app.aaps.core.objects.runningMode.PumpCommandGate
import app.aaps.core.objects.runningMode.RunningModeGuard
import app.aaps.core.objects.wizard.QuickWizard
import app.aaps.core.objects.wizard.QuickWizardEntry
import app.aaps.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * [WizardBolusExecutor] implementation. The `BolusWizard`/`QuickWizardEntry` compute objects stay
 * **internal** here (`:implementation` sees `core:objects`); the interface exposes only the primitive
 * result, so it can live in `core:interfaces` with no `core:objects` dependency.
 */
@Singleton
class WizardBolusExecutorImpl @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val rh: ResourceHelper,
    private val quickWizard: QuickWizard,
    private val profileFunction: ProfileFunction,
    private val iobCobCalculator: IobCobCalculator,
    private val constraintChecker: ConstraintsChecker,
    private val activePlugin: ActivePlugin,
    private val runningModeGuard: RunningModeGuard,
    private val commandQueue: CommandQueue,
    private val persistenceLayer: PersistenceLayer,
    private val uel: UserEntryLogger,
    private val loop: Loop,
    private val dateUtil: DateUtil,
    @ApplicationScope private val appScope: CoroutineScope
) : WizardBolusExecutor {

    // Field-injected (mirrors DataHandlerMobile) to stay clear of the Automation DI cycle.
    @Inject lateinit var automation: Automation

    /** The consumed-once slot. [entry] is non-null only for a quick-wizard prepare (carb timing / super-bolus). */
    private data class PendingBolus(val insulin: Double, val carbs: Int, val bcr: BCR?, val bolusId: Long, val entry: QuickWizardEntry?)

    private var pending: PendingBolus? = null

    override fun setPending(insulin: Double, carbs: Int, bolusCalculatorResult: BCR?, bolusId: Long) {
        pending = PendingBolus(insulin, carbs, bolusCalculatorResult, bolusId, entry = null)
    }

    override fun clearPending() {
        pending = null
    }

    override suspend fun prepareQuickWizard(guid: String): WizardBolusExecutor.PrepareResult {
        runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let { return WizardBolusExecutor.PrepareResult.Error(it) }
        val actualBg = iobCobCalculator.ads.actualBg()
        val profile = profileFunction.getProfile()
        val profileName = profileFunction.getProfileName()
        val entry = quickWizard.get(guid) ?: return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.quick_wizard_not_available))
        if (actualBg == null) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_actual_bg))
        if (profile == null) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_active_profile))
        val cobInfo = iobCobCalculator.getCobInfo("QuickWizard")
        if (cobInfo.displayCob == null) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_cob))
        val pump = activePlugin.activePump
        if (!pump.isInitialized()) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_pump_not_available))

        val wizard = entry.doCalc(profile, profileName, actualBg)

        val carbsAfterConstraints = constraintChecker.applyCarbsConstraints(ConstraintObject(entry.carbs(), aapsLogger)).value()
        if (carbsAfterConstraints != entry.carbs()) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_carbs_constraint))
        val insulinAfterConstraints = wizard.insulinAfterConstraints
        val minStep = pump.pumpDescription.pumpType.determineCorrectBolusStepSize(insulinAfterConstraints)
        if (abs(insulinAfterConstraints - wizard.calculatedTotalInsulin) >= minStep)
            return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_constraint_bolus_size, wizard.calculatedTotalInsulin))

        pending = PendingBolus(wizard.calculatedTotalInsulin, wizard.carbs, wizard.createBolusCalculatorResult(), wizard.timeStamp, entry)
        return WizardBolusExecutor.PrepareResult.Preview(wizard.calculatedTotalInsulin, wizard.carbs, wizard.explainShort(), wizard.timeStamp)
    }

    override suspend fun confirm(bolusId: Long, source: Sources, onError: (String) -> Unit): WizardBolusExecutor.ConfirmResult {
        val p = pending.also { pending = null } ?: return WizardBolusExecutor.ConfirmResult.NoPending
        if (p.bolusId != bolusId) return WizardBolusExecutor.ConfirmResult.NoPending

        var carbTime: Long? = null
        var carbTimeOffset = 0L
        var useAlarm = false
        val currentTime = dateUtil.now()
        var eventTime = currentTime
        var carbs2 = 0
        var duration = 0
        var notes: String? = null
        p.entry?.let { qwe ->
            carbTimeOffset = qwe.carbTime().toLong()
            carbTime = currentTime + (carbTimeOffset * 60000)
            useAlarm = qwe.useAlarm() == QuickWizardEntry.YES
            notes = qwe.buttonText()
            if (qwe.useEcarbs() == QuickWizardEntry.YES) {
                eventTime += (qwe.time() * 60000)
                carbs2 = qwe.carbs2()
                duration = qwe.duration()
            }
        }
        deliver(p.insulin, p.carbs, carbTime, 0, p.bcr, notes, source, onError)
        deliverECarbs(carbs2, eventTime, duration, notes, source, onError)
        if (useAlarm && p.carbs > 0 && carbTimeOffset > 0)
            automation.scheduleTimeToEatReminder(T.mins(carbTimeOffset).secs().toInt())
        p.entry?.markAsUsed()
        return WizardBolusExecutor.ConfirmResult.Delivered
    }

    override suspend fun deliver(
        amount: Double,
        carbs: Int,
        carbsTime: Long?,
        carbsDuration: Int,
        bolusCalculatorResult: BCR?,
        notes: String?,
        source: Sources,
        onError: (String) -> Unit
    ) {
        val detailedBolusInfo = DetailedBolusInfo()
        detailedBolusInfo.insulin = amount
        detailedBolusInfo.carbs = carbs.toDouble()
        detailedBolusInfo.bolusType = BS.Type.NORMAL
        detailedBolusInfo.carbsTimestamp = carbsTime
        detailedBolusInfo.carbsDuration = T.hours(carbsDuration.toLong()).msecs()
        detailedBolusInfo.notes = notes
        if (detailedBolusInfo.insulin > 0 || detailedBolusInfo.carbs != 0.0) {

            if (detailedBolusInfo.insulin > 0) {
                runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let {
                    onError(it)
                    return
                }
            }
            val action = when {
                amount == 0.0     -> Action.CARBS
                carbs == 0        -> Action.BOLUS
                carbsDuration > 0 -> Action.EXTENDED_CARBS
                else              -> Action.TREATMENT
            }
            uel.log(
                action = action, source = source,
                listValues = listOfNotNull(
                    ValueWithUnit.Insulin(amount).takeIf { amount != 0.0 },
                    ValueWithUnit.Gram(carbs).takeIf { carbs != 0 },
                    ValueWithUnit.Hour(carbsDuration).takeIf { carbsDuration != 0 }
                )
            )
            appScope.launch {
                val result = commandQueue.bolus(detailedBolusInfo)
                if (!result.success)
                    onError(rh.gs(R.string.treatmentdeliveryerror) + "\n" + result.comment)
            }
            bolusCalculatorResult?.let { persistenceLayer.insertOrUpdateBolusCalculatorResult(it) }
            // Super-bolus reads the consumed slot's entry, which `confirm` has already nulled before
            // calling here — inert on the confirm path, preserving the existing wear behaviour.
            pending?.entry?.let { entry ->
                if (entry.useSuperBolus() == QuickWizardEntry.YES) {
                    val profile = profileFunction.getProfile() ?: return
                    loop.handleRunningModeChange(
                        newRM = RM.Mode.SUPER_BOLUS,
                        action = Action.SUPERBOLUS_TBR,
                        source = source,
                        durationInMinutes = 2 * 60,
                        profile = profile
                    )
                }
            }
        }
    }

    override suspend fun deliverFillBolus(amount: Double, source: Sources, onError: (String) -> Unit) {
        runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let {
            onError(it)
            return
        }
        val detailedBolusInfo = DetailedBolusInfo()
        detailedBolusInfo.insulin = amount
        detailedBolusInfo.bolusType = BS.Type.PRIMING
        uel.log(
            action = Action.PRIME_BOLUS, source = source,
            listValues = listOfNotNull(ValueWithUnit.Insulin(amount).takeIf { amount != 0.0 })
        )
        appScope.launch {
            val result = commandQueue.bolus(detailedBolusInfo)
            if (!result.success)
                onError(rh.gs(R.string.treatmentdeliveryerror) + "\n" + result.comment)
        }
    }

    override suspend fun deliverECarbs(carbs: Int, carbsTime: Long, duration: Int, notes: String?, source: Sources, onError: (String) -> Unit) {
        uel.log(
            action = if (duration == 0) Action.CARBS else Action.EXTENDED_CARBS,
            source = source,
            listValues = listOfNotNull(
                ValueWithUnit.Timestamp(carbsTime),
                ValueWithUnit.Gram(carbs),
                ValueWithUnit.Hour(duration).takeIf { duration != 0 }
            )
        )
        deliver(0.0, carbs, carbsTime, duration, null, notes, source, onError)
    }
}
