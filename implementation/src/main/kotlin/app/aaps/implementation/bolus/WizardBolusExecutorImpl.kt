package app.aaps.implementation.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TE
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
    private val automation: Automation,
    @ApplicationScope private val appScope: CoroutineScope
) : WizardBolusExecutor {

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

        var carbTimeOffset = 0L
        var useAlarm = false
        val currentTime = dateUtil.now()
        var eventTime = currentTime
        var carbs2 = 0
        var duration = 0
        var notes: String? = null
        p.entry?.let { qwe ->
            carbTimeOffset = qwe.carbTime().toLong()
            useAlarm = qwe.useAlarm() == QuickWizardEntry.YES
            notes = qwe.buttonText()
            if (qwe.useEcarbs() == QuickWizardEntry.YES) {
                eventTime += (qwe.time() * 60000)
                carbs2 = qwe.carbs2()
                duration = qwe.duration()
            }
        }
        // Super-bolus (a quick-wizard with useSuperBolus): write the SUPER_BOLUS mode change before the
        // bolus, driven off the CONSUMED entry — never the shared `pending` slot, which a stale unconfirmed
        // wear prepare could leak into an unrelated bolus. Mirrors the phone's executeNormal.
        if (p.entry?.useSuperBolus() == QuickWizardEntry.YES) {
            profileFunction.getProfile()?.let { profile ->
                if (loop.allowedNextModes().contains(RM.Mode.SUPER_BOLUS))
                    loop.handleRunningModeChange(
                        durationInMinutes = 2 * 60,
                        profile = profile,
                        newRM = RM.Mode.SUPER_BOLUS,
                        action = Action.SUPERBOLUS_TBR,
                        source = source
                    )
            }
        }
        // Canonical: a wear wizard bolus now records identically to the phone (BOLUS_WIZARD + mgdlGlucose).
        // The mg/dL BG comes from the BCR's glucoseValue (== profileUtil.convertToMgdl(bg, units)).
        deliverWizardBolus(p.insulin, p.carbs, carbTimeOffset.toInt(), p.bcr?.glucoseValue, p.bcr, notes, source, onError)
        if (carbs2 > 0) deliverECarbs(carbs2, eventTime, duration, notes, source, onError)
        if (useAlarm && p.carbs > 0 && carbTimeOffset > 0)
            automation.scheduleTimeToEatReminder(T.mins(carbTimeOffset).secs().toInt())
        p.entry?.markAsUsed()
        return WizardBolusExecutor.ConfirmResult.Delivered
    }

    override suspend fun deliverWizardBolus(
        insulin: Double,
        carbs: Int,
        carbTimeMinutes: Int,
        mgdlGlucose: Double?,
        bolusCalculatorResult: BCR?,
        notes: String?,
        source: Sources,
        onError: (String) -> Unit
    ) {
        // Type-specific entry point: build the canonical BOLUS_WIZARD end state from the wizard inputs,
        // then funnel into the shared core. Phone (WizardDialog) and watch (QuickWizard) differ only in
        // [source] — the recorded treatment is identical.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.BOLUS_WIZARD
            this.insulin = insulin
            this.carbs = carbs.toDouble()
            bolusType = BS.Type.NORMAL
            mgdlGlucose?.let { this.mgdlGlucose = it; glucoseType = TE.MeterType.MANUAL }
            carbsTimestamp = dateUtil.now() + T.mins(carbTimeMinutes.toLong()).msecs()
            this.notes = notes
            bolusCalculatorResult?.let { this.bolusCalculatorResult = it }
        }
        if (insulin > 0 || carbs != 0) {
            val action = when {
                insulin == 0.0 -> Action.CARBS
                carbs == 0     -> Action.BOLUS
                else           -> Action.TREATMENT
            }
            val uelValues = listOfNotNull(
                ValueWithUnit.TEType(TE.Type.BOLUS_WIZARD),
                ValueWithUnit.Insulin(insulin).takeIf { insulin != 0.0 },
                ValueWithUnit.Gram(carbs).takeIf { carbs != 0 },
                ValueWithUnit.Minute(carbTimeMinutes).takeIf { carbTimeMinutes != 0 }
            )
            executeBolus(detailedBolusInfo, action, uelValues, notes, bolusCalculatorResult, source, onError)
        }
    }

    override suspend fun deliverBolusAdvisor(
        insulin: Double,
        mgdlGlucose: Double?,
        bolusCalculatorResult: BCR?,
        notes: String?,
        source: Sources,
        onError: (String) -> Unit
    ) {
        // Correction-only advisor bolus (BG high, carbs imminent): canonical CORRECTION_BOLUS end state,
        // and the eat reminder is scheduled on delivery success. The BCR rides the DBI but is not persisted
        // to the BCR table (matching both legacy advisor paths).
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.CORRECTION_BOLUS
            this.insulin = insulin
            carbs = 0.0
            mgdlGlucose?.let { this.mgdlGlucose = it; glucoseType = TE.MeterType.MANUAL }
            bolusCalculatorResult?.let { this.bolusCalculatorResult = it }
            this.notes = notes
        }
        if (insulin > 0) {
            val uelValues = listOf(
                ValueWithUnit.TEType(TE.Type.CORRECTION_BOLUS),
                ValueWithUnit.Insulin(insulin)
            )
            executeBolus(
                detailedBolusInfo, Action.BOLUS_ADVISOR, uelValues, notes,
                bolusCalculatorResult = null,
                source, onError,
                onSuccess = { automation.scheduleAutomationEventEatReminder() }
            )
        }
    }

    override suspend fun deliverInsulin(insulin: Double, note: String?, source: Sources, onError: (String) -> Unit) {
        // Fixed-amount correction insulin (QuickWizard INSULIN button): CORRECTION_BOLUS + BOLUS user entry,
        // note logged on the user entry only (the legacy path left the treatment notes unset).
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.CORRECTION_BOLUS
            this.insulin = insulin
        }
        if (insulin > 0) {
            executeBolus(
                detailedBolusInfo,
                Action.BOLUS,
                listOf(ValueWithUnit.Insulin(insulin)),
                note = note,
                bolusCalculatorResult = null,
                source = source,
                onError = onError
            )
        }
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
        val detailedBolusInfo = DetailedBolusInfo().apply {
            insulin = amount
            this.carbs = carbs.toDouble()
            bolusType = BS.Type.NORMAL
            carbsTimestamp = carbsTime
            this.carbsDuration = T.hours(carbsDuration.toLong()).msecs()
            this.notes = notes
        }
        if (amount > 0 || carbs != 0) {
            val action = when {
                amount == 0.0 -> Action.CARBS
                carbs == 0 -> Action.BOLUS
                carbsDuration > 0 -> Action.EXTENDED_CARBS
                else -> Action.TREATMENT
            }
            val uelValues = listOfNotNull(
                ValueWithUnit.Insulin(amount).takeIf { amount != 0.0 },
                ValueWithUnit.Gram(carbs).takeIf { carbs != 0 },
                ValueWithUnit.Hour(carbsDuration).takeIf { carbsDuration != 0 }
            )
            executeBolus(detailedBolusInfo, action, uelValues, note = null, bolusCalculatorResult, source, onError)
        }
    }

    /**
     * Shared core every bolus entry point funnels into: gate → log → queue → persist BCR → super-bolus.
     * The caller has already built the canonical [detailedBolusInfo] and its [uelValues]; this is the one
     * audited path to the pump.
     */
    private suspend fun executeBolus(
        detailedBolusInfo: DetailedBolusInfo,
        action: Action,
        uelValues: List<ValueWithUnit>,
        note: String?,
        bolusCalculatorResult: BCR?,
        source: Sources,
        onError: (String) -> Unit,
        onSuccess: () -> Unit = {}
    ) {
        if (detailedBolusInfo.insulin > 0) {
            runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let {
                onError(it)
                return
            }
        }
        uel.log(action = action, source = source, note = note, listValues = uelValues)
        appScope.launch {
            val result = commandQueue.bolus(detailedBolusInfo)
            if (!result.success)
                onError(rh.gs(R.string.treatmentdeliveryerror) + "\n" + result.comment)
            else
                onSuccess()
        }
        bolusCalculatorResult?.let { persistenceLayer.insertOrUpdateBolusCalculatorResult(it) }
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
        // Funnel straight through the shared core (not via deliver) so the UEL is logged exactly once with
        // the eCarbs Timestamp; routing via deliver would re-log [Gram, Hour] as a second, duplicate row.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            this.carbs = carbs.toDouble()
            carbsTimestamp = carbsTime
            carbsDuration = T.hours(duration.toLong()).msecs()
            this.notes = notes
        }
        if (carbs != 0) {
            val action = if (duration == 0) Action.CARBS else Action.EXTENDED_CARBS
            val uelValues = listOfNotNull(
                ValueWithUnit.Timestamp(carbsTime),
                ValueWithUnit.Gram(carbs),
                ValueWithUnit.Hour(duration).takeIf { duration != 0 }
            )
            executeBolus(detailedBolusInfo, action, uelValues, note = null, bolusCalculatorResult = null, source, onError)
        }
    }
}
