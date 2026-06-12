package app.aaps.implementation.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.ICfg
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TE
import app.aaps.core.data.model.TT
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.data.ui.ConfirmationLine
import app.aaps.core.data.ui.ConfirmationRole
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.bolus.BatchAction
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.di.ApplicationScope
import app.aaps.core.interfaces.iob.IobCobCalculator
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.profile.ProfileUtil
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.defs.determineCorrectBolusStepSize
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.objects.constraints.ConstraintObject
import app.aaps.core.objects.runningMode.PumpCommandGate
import app.aaps.core.objects.runningMode.RunningModeGuard
import app.aaps.core.objects.wizard.BolusWizard
import app.aaps.core.objects.wizard.QuickWizard
import app.aaps.core.objects.wizard.QuickWizardEntry
import app.aaps.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ceil

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
    private val bolusWizardProvider: Provider<BolusWizard>,
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
    private val decimalFormatter: DecimalFormatter,
    private val profileUtil: ProfileUtil,
    private val automation: Automation,
    @ApplicationScope private val appScope: CoroutineScope
) : WizardBolusExecutor {

    /**
     * The consumed-once slot. [entry] is non-null only for a quick-wizard prepare (carb timing / super-bolus /
     * eCarbs). [carbTimeMinutes]/[notes] are carried so a manual-wizard prepare (no entry) delivers identically.
     */
    private enum class BolusMode {

        WIZARD, FIXED
    }

    private data class PendingBolus(
        val insulin: Double,
        val carbs: Int,
        val bcr: BCR?,
        val bolusId: Long,
        val entry: QuickWizardEntry?,
        val carbTimeMinutes: Int = 0,
        val notes: String? = null,
        val mode: BolusMode = BolusMode.WIZARD,
        val eventType: TE.Type? = null,
        val carbsDurationHours: Int = 0,
        val eCarbsGrams: Int = 0,
        val eCarbsDelayMinutes: Int = 0,
        val eCarbsDurationHours: Int = 0,
        // Batch extension: a FIXED prepare may carry a temp target + a record-only flag/insulin/back-date,
        // delivered together in [confirm] (decision-B order). Null/false for a plain fixed or wizard prepare.
        val tempTarget: BatchAction.TempTarget? = null,
        val recordOnly: Boolean = false,
        val iCfg: ICfg? = null,
        val bolusTimestamp: Long? = null
    )

    // Consume-once slots keyed by bolusId — a map, NOT a single var, so prepares from different actors (the
    // master's own dialogs, the watch, and EVERY paired client) don't clobber each other's parked dose, and a
    // commit only ever consumes the dose with the MATCHING id. remove(bolusId) is atomic, so two concurrent
    // commits of the same id can't both deliver — the loser gets null → NoPending (no double bolus).
    private val pending = ConcurrentHashMap<Long, PendingBolus>()
    private val pendingTtlMs = 10 * 60_000L // a parked dose unconfirmed this long is abandoned; trimmed on the next prepare

    // bolusId is a timestamp, so a key older than the TTL is an abandoned prepare — drop it so the map can't grow.
    private fun evictStalePending() {
        val cutoff = dateUtil.now() - pendingTtlMs
        pending.keys.removeIf { it < cutoff }
    }

    override fun setPending(insulin: Double, carbs: Int, bolusCalculatorResult: BCR?, bolusId: Long) {
        evictStalePending()
        pending[bolusId] = PendingBolus(insulin, carbs, bolusCalculatorResult, bolusId, entry = null)
    }

    override fun clearPending() {
        // Per-id keying already prevents a stale prepare from leaking into an unrelated commit; just trim stale ids.
        evictStalePending()
    }

    override suspend fun prepareQuickWizard(guid: String): WizardBolusExecutor.PrepareResult {
        runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let { return WizardBolusExecutor.PrepareResult.Error(it) }
        val actualBg = iobCobCalculator.ads.actualBg()
        val profile = profileFunction.getProfile()
        val profileName = profileFunction.getProfileName()
        val entry = quickWizard.get(guid) ?: return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.quick_wizard_not_available))
        if (actualBg == null) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_actual_bg))
        if (profile == null) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_active_profile))
        // No COB gate: QuickWizardEntry.doCalc treats a null COB as 0 (only ever LOWERS the dose = safe),
        // so requiring COB here was stricter than the local QuickWizard and the real "missing data" mismatch.
        val pump = activePlugin.activePump
        if (!pump.isInitialized()) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_pump_not_available))

        val wizard = entry.doCalc(profile, profileName, actualBg)

        val carbsAfterConstraints = constraintChecker.applyCarbsConstraints(ConstraintObject(entry.carbs(), aapsLogger)).value()
        if (carbsAfterConstraints != entry.carbs()) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_carbs_constraint))
        val insulinAfterConstraints = wizard.insulinAfterConstraints
        val minStep = pump.pumpDescription.pumpType.determineCorrectBolusStepSize(insulinAfterConstraints)
        if (abs(insulinAfterConstraints - wizard.calculatedTotalInsulin) >= minStep)
            return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_constraint_bolus_size, wizard.calculatedTotalInsulin))

        evictStalePending()
        pending[wizard.timeStamp] = PendingBolus(wizard.calculatedTotalInsulin, wizard.carbs, wizard.createBolusCalculatorResult(), wizard.timeStamp, entry, carbTimeMinutes = entry.carbTime(), notes = entry.buttonText())
        // Build the master's color-coded confirmation lines here so the client renders the master's EXACT
        // wizard confirmation (shared builder). advisorApplies offers the high-BG "correct now, eat later" fork.
        val advisorApplies = wizard.needsBolusAdvisor()
        return WizardBolusExecutor.PrepareResult.Preview(
            insulin = wizard.calculatedTotalInsulin,
            carbs = wizard.carbs,
            explanation = wizard.explainShort(),
            bolusId = wizard.timeStamp,
            lines = wizard.buildConfirmationLines(advisor = false, quickWizardEntry = entry),
            advisorApplies = advisorApplies,
            advisorLines = if (advisorApplies) wizard.buildConfirmationLines(advisor = true, quickWizardEntry = entry) else emptyList()
        )
    }

    override suspend fun prepareWizard(inputs: WizardBolusExecutor.WizardInputs): WizardBolusExecutor.PrepareResult {
        runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let { return WizardBolusExecutor.PrepareResult.Error(it) }
        val profile = profileFunction.getProfile() ?: return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_no_active_profile))
        val profileName = profileFunction.getProfileName()
        val pump = activePlugin.activePump
        if (!pump.isInitialized()) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_pump_not_available))
        // Recompute on the MASTER's live state (active profile + temp target + COB) using the client's inputs.
        // The stored-profile selection isn't honored yet — the master uses its active profile (a refinement).
        val tempTarget = persistenceLayer.getTemporaryTargetActiveAt(dateUtil.now())
        val cob = if (inputs.useCob) iobCobCalculator.getCobInfo("WizardPrepare").displayCob ?: 0.0 else 0.0
        val carbsAfterConstraints = constraintChecker.applyCarbsConstraints(ConstraintObject(inputs.carbs, aapsLogger)).value()
        if (carbsAfterConstraints != inputs.carbs) return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_carbs_constraint))
        val wizard = bolusWizardProvider.get().doCalc(
            profile, profileName, tempTarget, carbsAfterConstraints, cob, inputs.bg, inputs.directCorrection, inputs.percentage,
            inputs.useBg, inputs.useCob, inputs.useIob, inputs.useIob, false, inputs.useTt, inputs.useTrend, inputs.alarm, inputs.notes, inputs.carbTime
        )
        val insulinAfterConstraints = wizard.insulinAfterConstraints
        val minStep = pump.pumpDescription.pumpType.determineCorrectBolusStepSize(insulinAfterConstraints)
        if (abs(insulinAfterConstraints - wizard.calculatedTotalInsulin) >= minStep)
            return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_constraint_bolus_size, wizard.calculatedTotalInsulin))
        evictStalePending()
        pending[wizard.timeStamp] =
            PendingBolus(
                wizard.calculatedTotalInsulin,
                wizard.carbs,
                wizard.createBolusCalculatorResult(),
                wizard.timeStamp,
                entry = null,
                carbTimeMinutes = inputs.carbTime,
                notes = inputs.notes,
                eCarbsGrams = inputs.eCarbsGrams,
                eCarbsDelayMinutes = inputs.eCarbsDelayMinutes,
                eCarbsDurationHours = inputs.eCarbsDurationHours
            )
        val advisorApplies = wizard.needsBolusAdvisor()
        return WizardBolusExecutor.PrepareResult.Preview(
            insulin = wizard.calculatedTotalInsulin,
            carbs = wizard.carbs,
            explanation = wizard.explainShort(),
            bolusId = wizard.timeStamp,
            lines = wizard.buildConfirmationLines(advisor = false),
            advisorApplies = advisorApplies,
            advisorLines = if (advisorApplies) wizard.buildConfirmationLines(advisor = true) else emptyList()
        )
    }

    override suspend fun prepareBatch(actions: List<BatchAction>): WizardBolusExecutor.PrepareResult {
        val bolus = actions.filterIsInstance<BatchAction.Bolus>().firstOrNull()
        val tt = actions.filterIsInstance<BatchAction.TempTarget>().firstOrNull() // ≤1 by construction
        val recordOnly = bolus?.recordOnly == true
        // Gate + pump-init only for an actual INSULIN delivery — carbs-only, a record-only log, and a TT-only batch
        // are always allowed (mirrors executeBolus, which gates only when insulin > 0).
        if (bolus != null && !recordOnly && bolus.insulin > 0.0) {
            runningModeGuard.rejectionMessage(PumpCommandGate.CommandKind.BOLUS)?.let { return WizardBolusExecutor.PrepareResult.Error(it) }
            if (!activePlugin.activePump.isInitialized())
                return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.wizard_pump_not_available))
        }
        // Cap a delivery (caps only reduce = safe); a record-only is persisted as given (a record must not be altered).
        val (insulin, carbs, eventType) = when {
            bolus == null -> Triple(0.0, 0, TE.Type.CORRECTION_BOLUS)
            recordOnly    -> Triple(bolus.insulin, bolus.carbs, TE.Type.CORRECTION_BOLUS)
            else          -> capFixed(bolus.insulin, bolus.carbs).let { (i, c, t) -> Triple(i, clampNegativeCarbs(c, bolus.carbsTimeOffsetMinutes), t) }
        }
        if (insulin <= 0.0 && carbs <= 0 && tt == null)
            return WizardBolusExecutor.PrepareResult.Error(rh.gs(R.string.no_action_selected))
        val bolusId = dateUtil.now()
        evictStalePending()
        pending[bolusId] = PendingBolus(
            insulin = insulin, carbs = carbs, bcr = null, bolusId = bolusId, entry = null,
            carbTimeMinutes = bolus?.carbsTimeOffsetMinutes ?: 0, notes = bolus?.notes, mode = BolusMode.FIXED,
            eventType = eventType, carbsDurationHours = bolus?.carbsDurationHours ?: 0,
            tempTarget = tt, recordOnly = recordOnly, iCfg = bolus?.iCfg, bolusTimestamp = bolus?.timestamp?.takeIf { it > 0L }
        )
        // The master is the SOLE author of the confirmation: build the MERGED lines for the whole batch here, so the
        // client renders the master's exact string and a master-local dialog renders the identical one (decision 1).
        val lines = buildFixedLines(bolus, insulin, carbs, recordOnly) + listOfNotNull(tt?.let { buildTtLine(it) })
        return WizardBolusExecutor.PrepareResult.Preview(insulin, carbs, "", bolusId, lines = lines, advisorApplies = false, advisorLines = emptyList())
    }

    override suspend fun confirm(bolusId: Long, source: Sources, onError: (String) -> Unit, asAdvisor: Boolean): WizardBolusExecutor.ConfirmResult {
        // Atomic consume-once: remove(bolusId) returns the parked dose and removes it in one step, so two
        // concurrent commits of the same id can't both deliver (the loser gets null → NoPending). A non-matching
        // id removes nothing, leaving other actors' parked doses intact.
        val p = pending.remove(bolusId) ?: return WizardBolusExecutor.ConfirmResult.NoPending

        val notes = p.notes

        // Fixed-amount bolus/carbs (Insulin / Treatment / Carbs dialogs): deliver the parked amounts as-is through
        // the shared core — no wizard recompute, no super-bolus/eCarbs/advisor (all wizard-only).
        if (p.mode == BolusMode.FIXED) {
            // A FIXED entry may carry a batch TempTarget. Decision-B order: a target-RAISING TT (hypo/activity) is
            // applied FIRST + unconditional; a target-LOWERING (eating-soon) TT applies only if the bolus was accepted.
            val tt = p.tempTarget
            val raising = tt != null && (tt.reason == TT.Reason.HYPOGLYCEMIA.text || tt.reason == TT.Reason.ACTIVITY.text)
            if (tt != null && raising) applyTempTarget(tt, source)
            var accepted = true
            val wrapped: (String) -> Unit = { accepted = false; onError(it) }
            when {
                p.recordOnly                    -> {
                    // Record-only (a pen bolus, or a master that can't deliver): persist insulin AND carbs as given,
                    // NOT capped, optionally back-dated. deliver() (not deliverInsulin) so a Treatment record keeps its carbs.
                    val carbsTime = if (p.carbs > 0) dateUtil.now() + T.mins(p.carbTimeMinutes.toLong()).msecs() else null
                    deliver(
                        p.insulin,
                        p.carbs,
                        carbsTime = carbsTime,
                        carbsDuration = p.carbsDurationHours,
                        bolusCalculatorResult = null,
                        notes = notes,
                        source = source,
                        onError = wrapped,
                        eventType = p.eventType,
                        recordOnly = true,
                        iCfg = p.iCfg,
                        timestamp = p.bolusTimestamp
                    )
                }

                p.insulin == 0.0 && p.carbs > 0 ->
                    // Carbs-only: funnel through the SAME eCarbs path the local Carbs dialog uses, so a client carb
                    // entry and a master carb entry are identical (TE.Type, duration, delay) — not the generic deliver()
                    // which would tag it CARBS_CORRECTION instead of deliverECarbs's CORRECTION_BOLUS convention.
                    deliverECarbs(p.carbs, dateUtil.now() + T.mins(p.carbTimeMinutes.toLong()).msecs(), p.carbsDurationHours, p.carbTimeMinutes, notes, source, wrapped)

                else                            -> {
                    // Insulin (± carbs): deliver the parked amounts as-is. carbsTime = now + offset; carbsDuration in hours.
                    val carbsTime = if (p.carbs > 0) dateUtil.now() + T.mins(p.carbTimeMinutes.toLong()).msecs() else null
                    deliver(p.insulin, p.carbs, carbsTime = carbsTime, carbsDuration = p.carbsDurationHours, bolusCalculatorResult = p.bcr, notes = notes, source = source, onError = wrapped, eventType = p.eventType)
                }
            }
            if (tt != null && !raising && accepted) applyTempTarget(tt, source)
            return WizardBolusExecutor.ConfirmResult.Delivered
        }

        // High-BG advisor branch (user chose "correct now, eat later"): a correction-only CORRECTION_BOLUS —
        // no carbs, no eCarbs, no super-bolus, no eat reminder. mg/dL BG comes from the BCR's glucoseValue.
        if (asAdvisor) {
            deliverBolusAdvisor(p.insulin, p.bcr?.glucoseValue, p.bcr, notes, source, onError)
            p.entry?.markAsUsed()
            return WizardBolusExecutor.ConfirmResult.Delivered
        }

        val carbTimeOffset = p.carbTimeMinutes.toLong()
        var useAlarm = false
        val currentTime = dateUtil.now()
        var eventTime = currentTime
        var carbs2 = 0
        var duration = 0
        var eCarbsDelay = 0
        val qwe = p.entry
        if (qwe != null) {
            useAlarm = qwe.useAlarm() == QuickWizardEntry.YES
            if (qwe.useEcarbs() == QuickWizardEntry.YES) {
                eCarbsDelay = qwe.time()
                eventTime += (eCarbsDelay * 60000)
                carbs2 = qwe.carbs2()
                duration = qwe.duration()
            }
        } else if (p.eCarbsGrams > 0) {
            // Manual wizard from a client (no QuickWizardEntry): the eCarbs split travels in PendingBolus so the
            // extended-carbs portion is still scheduled on the master — matching the phone's executeNormal.
            eCarbsDelay = p.eCarbsDelayMinutes
            eventTime += (eCarbsDelay * 60000)
            carbs2 = p.eCarbsGrams
            duration = p.eCarbsDurationHours
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
        if (carbs2 > 0) deliverECarbs(carbs2, eventTime, duration, eCarbsDelay, notes, source, onError)
        if (useAlarm && p.carbs > 0 && carbTimeOffset > 0)
            automation.scheduleTimeToEatReminder(T.mins(carbTimeOffset).secs().toInt())
        p.entry?.markAsUsed()
        return WizardBolusExecutor.ConfirmResult.Delivered
    }

    /** Cap + classify a FIXED bolus/carbs (delivery only — a record-only entry must NOT be capped). */
    private fun capFixed(insulin: Double, carbs: Int): Triple<Double, Int, TE.Type> {
        val cappedInsulin = constraintChecker.applyBolusConstraints(ConstraintObject(insulin, aapsLogger)).value()
        val cappedCarbs = constraintChecker.applyCarbsConstraints(ConstraintObject(carbs, aapsLogger)).value()
        val eventType = when {
            cappedInsulin > 0.0 && cappedCarbs > 0 -> TE.Type.MEAL_BOLUS
            cappedInsulin > 0.0                    -> TE.Type.CORRECTION_BOLUS
            else                                   -> TE.Type.CARBS_CORRECTION
        }
        return Triple(cappedInsulin, cappedCarbs, eventType)
    }

    /** Negative carbs = COB removal: clamp so we never remove more than the master's CURRENT COB, and drop a back-dated removal (matches the Carbs dialog, now on the master). */
    private fun clampNegativeCarbs(carbs: Int, carbsTimeOffsetMinutes: Int): Int {
        if (carbs >= 0) return carbs
        val cob = iobCobCalculator.ads.getLastAutosensData("prepareBatch", aapsLogger, dateUtil)?.cob ?: 0.0
        var c = carbs
        if (c < -cob) c = ceil(-cob).toInt()
        if (carbsTimeOffsetMinutes != 0) c = 0
        return c
    }

    /** Apply a batch TempTarget via the dialog-free domain path (mirrors onVerifiedTempTargetSet). */
    private suspend fun applyTempTarget(tt: BatchAction.TempTarget, source: Sources) {
        val reason = TT.Reason.fromString(tt.reason)
        persistenceLayer.insertAndCancelCurrentTemporaryTarget(
            temporaryTarget = TT(
                timestamp = dateUtil.now() + T.mins(tt.startOffsetMinutes.toLong()).msecs(),
                duration = T.mins(tt.durationMinutes.toLong()).msecs(),
                reason = reason,
                lowTarget = tt.lowMgdl,
                highTarget = tt.highMgdl
            ),
            action = Action.TT,
            source = source,
            note = null,
            listValues = listOf(ValueWithUnit.Mgdl(tt.lowMgdl), ValueWithUnit.Minute(tt.durationMinutes))
        )
    }

    /** The MERGED confirmation lines for a FIXED batch bolus/carbs — the master is the sole author (client renders these verbatim). */
    private fun buildFixedLines(bolus: BatchAction.Bolus?, insulin: Double, carbs: Int, recordOnly: Boolean): List<ConfirmationLine> {
        bolus ?: return emptyList()
        val pumpDescription = activePlugin.activePump.pumpDescription
        val out = mutableListOf<ConfirmationLine>()
        if (insulin > 0.0) {
            out += ConfirmationLine(ConfirmationRole.BOLUS, rh.gs(R.string.confirmation_line, rh.gs(R.string.bolus), decimalFormatter.toPumpSupportedBolusWithUnits(insulin, pumpDescription.bolusStep)))
            if (recordOnly) {
                out += ConfirmationLine(ConfirmationRole.WARNING, rh.gs(R.string.bolus_recorded_only))
                bolus.iCfg?.let { out += ConfirmationLine(ConfirmationRole.NORMAL, rh.gs(R.string.selected_insulin, it.insulinLabel)) }
            } else if (abs(insulin - bolus.insulin) > pumpDescription.pumpType.determineCorrectBolusStepSize(insulin)) {
                out += ConfirmationLine(ConfirmationRole.WARNING, rh.gs(R.string.bolus_constraint_applied_warn, bolus.insulin, insulin))
            }
        }
        if (carbs > 0) {
            out += ConfirmationLine(ConfirmationRole.CARBS, rh.gs(R.string.confirmation_line, rh.gs(R.string.carbs), rh.gs(R.string.format_carbs, carbs)))
            if (!recordOnly && carbs != bolus.carbs)
                out += ConfirmationLine(ConfirmationRole.WARNING, rh.gs(R.string.constraint_applied))
            if (bolus.carbsDurationHours > 0)
                out += ConfirmationLine(ConfirmationRole.NORMAL, rh.gs(R.string.confirmation_line, rh.gs(R.string.duration), rh.gs(R.string.value_with_unit, bolus.carbsDurationHours.toString(), rh.gs(app.aaps.core.interfaces.R.string.shorthour))))
        }
        if (bolus.notes.isNotEmpty())
            out += ConfirmationLine(ConfirmationRole.NORMAL, rh.gs(R.string.confirmation_line, rh.gs(R.string.notes_label), bolus.notes))
        return out
    }

    /** The TT confirmation line for a batch — target in the user's units (+ duration), the same shape the dialogs show. */
    private fun buildTtLine(tt: BatchAction.TempTarget): ConfirmationLine {
        val units = profileFunction.getUnits()
        val unitLabel = if (units == GlucoseUnit.MMOL) rh.gs(R.string.mmol) else rh.gs(R.string.mgdl)
        val target = decimalFormatter.to1Decimal(profileUtil.fromMgdlToUnits(tt.lowMgdl, units))
        return ConfirmationLine(
            ConfirmationRole.NORMAL,
            rh.gs(R.string.confirmation_line, rh.gs(R.string.temporary_target), rh.gs(R.string.value_with_unit, target, unitLabel) + " (" + rh.gs(R.string.format_mins, tt.durationMinutes) + ")")
        )
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

    override suspend fun deliverInsulin(
        insulin: Double,
        note: String?,
        source: Sources,
        onError: (String) -> Unit,
        timestamp: Long?,
        treatmentNote: String?,
        recordOnly: Boolean,
        iCfg: ICfg?,
        onSuccess: () -> Unit
    ) {
        // Fixed-amount correction insulin: CORRECTION_BOLUS + BOLUS user entry. [note] is logged on the user
        // entry; [treatmentNote] (when given) is stored on the bolus treatment; [timestamp] back/forward-dates
        // it. [recordOnly] persists directly (needs [iCfg]) instead of delivering.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.CORRECTION_BOLUS
            this.insulin = insulin
            this.notes = treatmentNote
            timestamp?.let { this.timestamp = it }
        }
        if (insulin > 0) {
            executeBolus(
                detailedBolusInfo,
                Action.BOLUS,
                listOf(ValueWithUnit.Insulin(insulin)),
                note = note,
                bolusCalculatorResult = null,
                source = source,
                onError = onError,
                onSuccess = onSuccess,
                recordOnly = recordOnly,
                iCfg = iCfg
            )
        }
    }

    override suspend fun deliverCarbs(carbs: Int, note: String?, source: Sources, onError: (String) -> Unit, onSuccess: () -> Unit) {
        // Instant carbs at now (zero insulin): CARBS_CORRECTION + CARBS user entry; note on the entry only.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.CARBS_CORRECTION
            this.carbs = carbs.toDouble()
            carbsTimestamp = dateUtil.now()
        }
        if (carbs != 0) {
            executeBolus(
                detailedBolusInfo,
                Action.CARBS,
                listOf(ValueWithUnit.Gram(carbs)),
                note = note,
                bolusCalculatorResult = null,
                source = source,
                onError = onError,
                onSuccess = onSuccess
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
        onError: (String) -> Unit,
        eventType: TE.Type?,
        recordOnly: Boolean,
        iCfg: ICfg?,
        timestamp: Long?,
        onSuccess: () -> Unit
    ) {
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType?.let { this.eventType = it }
            insulin = amount
            this.carbs = carbs.toDouble()
            bolusType = BS.Type.NORMAL
            carbsTimestamp = carbsTime
            this.carbsDuration = T.hours(carbsDuration.toLong()).msecs()
            this.notes = notes
            timestamp?.let { this.timestamp = it }
        }
        if (amount > 0 || carbs != 0) {
            val action = when {
                amount == 0.0     -> Action.CARBS
                carbs == 0        -> Action.BOLUS
                carbsDuration > 0 -> Action.EXTENDED_CARBS
                else              -> Action.TREATMENT
            }
            val uelValues = listOfNotNull(
                ValueWithUnit.Insulin(amount).takeIf { amount != 0.0 },
                ValueWithUnit.Gram(carbs).takeIf { carbs != 0 },
                ValueWithUnit.Hour(carbsDuration).takeIf { carbsDuration != 0 }
            )
            executeBolus(detailedBolusInfo, action, uelValues, note = null, bolusCalculatorResult, source, onError, onSuccess, recordOnly, iCfg)
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
        onSuccess: () -> Unit = {},
        recordOnly: Boolean = false,
        iCfg: ICfg? = null
    ) {
        if (recordOnly) {
            // No pump command (AAPSCLIENT / uninitialized pump / loop forbids a bolus): persist the
            // treatment(s) directly. The persistence layer emits the user entry, so there's no uel.log
            // and no running-mode gate here. The user's own notes ride [detailedBolusInfo.notes] onto the
            // BS/CA; the user entry gets the uniform "record" marker (no fragile "Record: notes" concat).
            val recordNote = rh.gs(R.string.record)
            appScope.launch {
                if (detailedBolusInfo.insulin > 0) {
                    val cfg = iCfg ?: profileFunction.getProfile()?.iCfg
                    if (cfg != null)
                        persistenceLayer.insertOrUpdateBolus(detailedBolusInfo.createBolus(cfg), action = action, source = source, note = recordNote)
                    else
                        aapsLogger.error(LTag.UI, "record-only bolus skipped: no insulin config available")
                }
                if (detailedBolusInfo.carbs != 0.0)
                    persistenceLayer.insertOrUpdateCarbs(detailedBolusInfo.createCarbs(), action = action, source = source, note = recordNote)
                onSuccess()
            }
            bolusCalculatorResult?.let { persistenceLayer.insertOrUpdateBolusCalculatorResult(it) }
            return
        }
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

    override suspend fun deliverFillBolus(amount: Double, notes: String?, source: Sources, onError: (String) -> Unit, onSuccess: () -> Unit) {
        // Prime/fill now rides the shared core (one audited path). PRIMING type keeps it out of IOB/TDD.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            insulin = amount
            bolusType = BS.Type.PRIMING
            this.notes = notes
        }
        if (amount > 0) {
            executeBolus(
                detailedBolusInfo,
                Action.PRIME_BOLUS,
                listOf(ValueWithUnit.Insulin(amount)),
                note = notes,
                bolusCalculatorResult = null,
                source = source,
                onError = onError,
                onSuccess = onSuccess
            )
        }
    }

    override suspend fun deliverECarbs(carbs: Int, carbsTime: Long, duration: Int, delayMinutes: Int, notes: String?, source: Sources, onError: (String) -> Unit, onSuccess: () -> Unit) {
        // Funnel straight through the shared core (not via deliver) so the UEL is logged exactly once with
        // the eCarbs Timestamp; routing via deliver would re-log [Gram, Hour] as a second, duplicate row.
        val detailedBolusInfo = DetailedBolusInfo().apply {
            eventType = TE.Type.CORRECTION_BOLUS
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
                ValueWithUnit.Minute(delayMinutes).takeIf { delayMinutes != 0 },
                ValueWithUnit.Hour(duration).takeIf { duration != 0 }
            )
            executeBolus(detailedBolusInfo, action, uelValues, note = notes, bolusCalculatorResult = null, source, onError, onSuccess)
        }
    }
}
