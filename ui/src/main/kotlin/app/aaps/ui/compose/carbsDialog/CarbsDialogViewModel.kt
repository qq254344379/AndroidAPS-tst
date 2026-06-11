package app.aaps.ui.compose.carbsDialog

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.configuration.Constants
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.TT
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.data.ui.ConfirmationLine
import app.aaps.core.data.ui.ConfirmationRole
import app.aaps.core.data.ui.confirmationLines
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.di.ApplicationScope
import app.aaps.core.interfaces.iob.GlucoseStatusProvider
import app.aaps.core.interfaces.iob.IobCobCalculator
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.profile.ProfileUtil
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventShowDialog
import app.aaps.core.interfaces.tempTargets.ttDurationMinutes
import app.aaps.core.interfaces.tempTargets.ttTargetMgdl
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.interfaces.utils.HardLimits
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.IntKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.constraints.ConstraintObject
import app.aaps.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max

@HiltViewModel
@Stable
class CarbsDialogViewModel @Inject constructor(
    private val constraintChecker: ConstraintsChecker,
    private val profileUtil: ProfileUtil,
    private val iobCobCalculator: IobCobCalculator,
    private val glucoseStatusProvider: GlucoseStatusProvider,
    private val automation: Automation,
    private val wizardBolusExecutor: WizardBolusExecutor,
    private val persistenceLayer: PersistenceLayer,
    val preferences: Preferences,
    val config: Config,
    private val decimalFormatter: DecimalFormatter,
    val rh: ResourceHelper,
    val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger,
    private val clientControlDispatcher: ClientControlActionDispatcher,
    private val rxBus: RxBus,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarbsDialogUiState())
    val uiState: StateFlow<CarbsDialogUiState> = _uiState.asStateFlow()

    sealed class SideEffect {
        data class ShowDeliveryError(val comment: String) : SideEffect()
        data object ShowNoActionDialog : SideEffect()
    }

    private val _sideEffect = MutableSharedFlow<SideEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sideEffect: SharedFlow<SideEffect> = _sideEffect.asSharedFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        val now = dateUtil.now()
        val maxCarbs = constraintChecker.getMaxCarbsAllowed().value()
        val units = profileUtil.units

        // Bolus reminder: visible when preference enabled AND predicted BG is low
        val showBolusReminder = if (preferences.get(BooleanKey.OverviewUseBolusReminder)) {
            val glucoseStatus = glucoseStatusProvider.glucoseStatusData
            glucoseStatus != null && glucoseStatus.glucose + 3 * glucoseStatus.delta < 70.0
        } else false

        // Auto-detect hypo condition
        val autoHypo = detectAutoHypo(now)

        _uiState.update {
            CarbsDialogUiState(
                carbs = 0,
                timeOffsetMinutes = 0,
                durationHours = 0,
                hypoTtChecked = autoHypo,
                eatingSoonTtChecked = false,
                activityTtChecked = false,
                alarmChecked = false,
                bolusReminderChecked = false,
                notes = "",
                eventTime = now,
                eventTimeOriginal = now,
                maxCarbs = maxCarbs,
                carbsButtonIncrement1 = preferences.get(IntKey.OverviewCarbsButtonIncrement1),
                carbsButtonIncrement2 = preferences.get(IntKey.OverviewCarbsButtonIncrement2),
                carbsButtonIncrement3 = preferences.get(IntKey.OverviewCarbsButtonIncrement3),
                units = units,
                showNotesFromPreferences = preferences.get(BooleanKey.OverviewShowNotesInDialogs),
                showBolusReminder = showBolusReminder,
                hypoTtTarget = profileUtil.fromMgdlToUnits(preferences.ttTargetMgdl(TT.Reason.HYPOGLYCEMIA), units),
                hypoTtDuration = preferences.ttDurationMinutes(TT.Reason.HYPOGLYCEMIA),
                eatingSoonTtTarget = profileUtil.fromMgdlToUnits(preferences.ttTargetMgdl(TT.Reason.EATING_SOON), units),
                eatingSoonTtDuration = preferences.ttDurationMinutes(TT.Reason.EATING_SOON),
                activityTtTarget = profileUtil.fromMgdlToUnits(preferences.ttTargetMgdl(TT.Reason.ACTIVITY), units),
                activityTtDuration = preferences.ttDurationMinutes(TT.Reason.ACTIVITY),
                maxCarbsDurationHours = HardLimits.MAX_CARBS_DURATION_HOURS,
                simpleMode = preferences.get(BooleanKey.GeneralSimpleMode)
            )
        }
    }

    private suspend fun detectAutoHypo(now: Long): Boolean {
        val bgReading = iobCobCalculator.ads.actualBg() ?: return false
        if (bgReading.recalculated >= 72) return false

        val hypoTTDuration = preferences.ttDurationMinutes(TT.Reason.HYPOGLYCEMIA)

        val activeTT = try {
            persistenceLayer.getTemporaryTargetActiveAt(now)
        } catch (_: Exception) {
            null
        }

        if (activeTT != null) {
            val activeTarget = activeTT.highTarget
            val remainingDurationMin = ((activeTT.timestamp + activeTT.duration) - now) / 60000
            if (activeTarget > Constants.NORMAL_TARGET_MGDL && remainingDurationMin > hypoTTDuration) {
                return false
            }
        }

        return true
    }

    fun refreshCarbsButtons() {
        _uiState.update {
            it.copy(
                carbsButtonIncrement1 = preferences.get(IntKey.OverviewCarbsButtonIncrement1),
                carbsButtonIncrement2 = preferences.get(IntKey.OverviewCarbsButtonIncrement2),
                carbsButtonIncrement3 = preferences.get(IntKey.OverviewCarbsButtonIncrement3)
            )
        }
    }

    fun updateCarbs(value: Int) {
        val state = uiState.value
        val clamped = value.coerceIn(-state.maxCarbs, state.maxCarbs)
        _uiState.update { it.copy(carbs = clamped) }
    }

    fun addCarbs(increment: Int) {
        val state = uiState.value
        val newValue = max(0, state.carbs + increment).coerceAtMost(state.maxCarbs)
        _uiState.update { it.copy(carbs = newValue) }
    }

    fun updateTimeOffset(minutes: Int) {
        val clamped = minutes.coerceIn(-7 * 24 * 60, 12 * 60)
        val state = uiState.value
        val newEventTime = state.eventTimeOriginal + clamped.toLong() * 60 * 1000
        _uiState.update {
            it.copy(
                timeOffsetMinutes = clamped,
                eventTime = newEventTime
            )
        }
    }

    fun updateDuration(hours: Int) {
        val clamped = hours.coerceIn(0, uiState.value.maxCarbsDurationHours.toInt())
        _uiState.update { it.copy(durationHours = clamped) }
    }

    fun updateHypoTt(checked: Boolean) {
        _uiState.update {
            it.copy(
                hypoTtChecked = checked,
                eatingSoonTtChecked = if (checked) false else it.eatingSoonTtChecked,
                activityTtChecked = if (checked) false else it.activityTtChecked
            )
        }
    }

    fun updateEatingSoonTt(checked: Boolean) {
        _uiState.update {
            it.copy(
                eatingSoonTtChecked = checked,
                hypoTtChecked = if (checked) false else it.hypoTtChecked,
                activityTtChecked = if (checked) false else it.activityTtChecked
            )
        }
    }

    fun updateActivityTt(checked: Boolean) {
        _uiState.update {
            it.copy(
                activityTtChecked = checked,
                hypoTtChecked = if (checked) false else it.hypoTtChecked,
                eatingSoonTtChecked = if (checked) false else it.eatingSoonTtChecked
            )
        }
    }

    fun updateAlarm(checked: Boolean) {
        _uiState.update { it.copy(alarmChecked = checked) }
    }

    fun updateBolusReminder(checked: Boolean) {
        _uiState.update { it.copy(bolusReminderChecked = checked) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun updateEventTime(timeMillis: Long) {
        val state = uiState.value
        val newOffset = ((timeMillis - state.eventTimeOriginal) / (1000 * 60)).toInt()
        _uiState.update {
            it.copy(
                eventTime = timeMillis,
                timeOffsetMinutes = newOffset
            )
        }
    }

    private var confirmedState: CarbsDialogUiState? = null

    fun buildConfirmationSummary(): List<ConfirmationLine> {
        val state = uiState.value
        confirmedState = state
        val unitLabel = if (state.units == GlucoseUnit.MMOL) rh.gs(app.aaps.core.ui.R.string.mmol) else rh.gs(app.aaps.core.ui.R.string.mgdl)

        return confirmationLines {
            // Temp target info
            if (state.activityTtChecked) {
                line(
                    ConfirmationRole.NORMAL,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(R.string.temp_target_short),
                        rh.gs(app.aaps.core.ui.R.string.value_with_unit, decimalFormatter.to1Decimal(state.activityTtTarget), unitLabel) +
                            " (" + rh.gs(app.aaps.core.ui.R.string.format_mins, state.activityTtDuration) + ")"
                    )
                )
            }
            if (state.eatingSoonTtChecked) {
                line(
                    ConfirmationRole.NORMAL,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(R.string.temp_target_short),
                        rh.gs(app.aaps.core.ui.R.string.value_with_unit, decimalFormatter.to1Decimal(state.eatingSoonTtTarget), unitLabel) +
                            " (" + rh.gs(app.aaps.core.ui.R.string.format_mins, state.eatingSoonTtDuration) + ")"
                    )
                )
            }
            if (state.hypoTtChecked) {
                line(
                    ConfirmationRole.NORMAL,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(R.string.temp_target_short),
                        rh.gs(app.aaps.core.ui.R.string.value_with_unit, decimalFormatter.to1Decimal(state.hypoTtTarget), unitLabel) +
                            " (" + rh.gs(app.aaps.core.ui.R.string.format_mins, state.hypoTtDuration) + ")"
                    )
                )
            }

            // Alarm
            val timeOffset = state.timeOffsetMinutes
            if (state.alarmChecked && state.carbs > 0 && timeOffset > 0) {
                line(ConfirmationRole.INFO, rh.gs(app.aaps.core.ui.R.string.alarminxmin, timeOffset))
            }

            // Duration
            val duration = state.durationHours
            if (duration > 0) {
                line(
                    ConfirmationRole.NORMAL,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(app.aaps.core.ui.R.string.duration),
                        rh.gs(app.aaps.core.ui.R.string.value_with_unit, duration.toString(), rh.gs(app.aaps.core.interfaces.R.string.shorthour))
                    )
                )
            }

            // Carbs (with constraint check)
            val carbs = state.carbs
            var carbsAfterConstraints = constraintChecker.applyCarbsConstraints(
                ConstraintObject(carbs, aapsLogger)
            ).value()

            if (carbsAfterConstraints > 0) {
                line(
                    ConfirmationRole.CARBS,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(app.aaps.core.ui.R.string.carbs),
                        rh.gs(app.aaps.core.ui.R.string.format_carbs, carbsAfterConstraints)
                    )
                )
                if (carbsAfterConstraints != carbs) {
                    line(ConfirmationRole.WARNING, rh.gs(R.string.carbs_constraint_applied))
                }
            }
            if (carbsAfterConstraints < 0) {
                val cob = iobCobCalculator.ads.getLastAutosensData("carbsDialog", aapsLogger, dateUtil)?.cob ?: 0.0
                if (carbsAfterConstraints < -cob) carbsAfterConstraints = ceil(-cob).toInt()
                if (timeOffset != 0) carbsAfterConstraints = 0
                line(
                    ConfirmationRole.CARBS,
                    rh.gs(
                        app.aaps.core.ui.R.string.confirmation_line, rh.gs(app.aaps.core.ui.R.string.carbs),
                        rh.gs(app.aaps.core.ui.R.string.format_carbs, carbsAfterConstraints)
                    )
                )
                if (carbsAfterConstraints != carbs) {
                    line(ConfirmationRole.WARNING, rh.gs(R.string.carbs_constraint_applied))
                }
            }

            // Notes
            if (state.notes.isNotEmpty()) {
                line(ConfirmationRole.NORMAL, rh.gs(app.aaps.core.ui.R.string.confirmation_line, rh.gs(app.aaps.core.ui.R.string.notes_label), state.notes))
            }

            // Time
            if (state.eventTimeChanged) {
                line(ConfirmationRole.NORMAL, rh.gs(app.aaps.core.ui.R.string.confirmation_line, rh.gs(app.aaps.core.ui.R.string.time), dateUtil.dateAndTimeString(state.eventTime)))
            }
        }
    }

    fun hasAction(): Boolean {
        val state = uiState.value
        val carbs = state.carbs
        val carbsAfterConstraints = constraintChecker.applyCarbsConstraints(
            ConstraintObject(carbs, aapsLogger)
        ).value()
        return carbsAfterConstraints != 0 || state.activityTtChecked || state.eatingSoonTtChecked || state.hypoTtChecked
    }

    /**
     * AAPSCLIENT deliver-via-master for carbs: the master constraint-caps + records the carbs, the client
     * confirms the master's number then commits. No carbs are recorded on the client itself. Carbs are
     * carbs-only — [FixedBolusPrepare] carries insulin = 0.0 and there is no advisor, so we always commit
     * asAdvisor = false and show prepared.lines. (Like the insulin dialog this shows a second confirm AFTER
     * the dialog's own confirm; a later refinement can skip the local one.)
     */
    private suspend fun confirmRemoteBolus(carbs: Int, carbsTimeOffsetMinutes: Int, carbsDurationHours: Int, notes: String?) {
        val label = rh.gs(app.aaps.core.ui.R.string.carbs)
        val title = rh.gs(app.aaps.core.ui.R.string.carbs)
        val prepared =
            clientControlDispatcher.run(ClientControlActionDispatcher.Command.FixedBolusPrepare(insulin = 0.0, carbs = carbs, carbsTimeOffsetMinutes = carbsTimeOffsetMinutes, carbsDurationHours = carbsDurationHours, notes = notes ?: ""), label)
        if (prepared !is ActionProgress.Prepared) return // a failure already surfaced through the pending modal
        rxBus.send(
            EventShowDialog.OkCancel(
                title = title, message = "", confirmationLines = prepared.lines,
                onOk = { appScope.launch { clientControlDispatcher.run(ClientControlActionDispatcher.Command.BolusCommit(prepared.bolusId, false), label) } }
            )
        )
    }

    fun confirmAndSave() {
        val state = confirmedState ?: return
        val carbs = state.carbs
        val cob = iobCobCalculator.ads.getLastAutosensData("carbsDialog", aapsLogger, dateUtil)?.cob ?: 0.0
        var carbsAfterConstraints = constraintChecker.applyCarbsConstraints(
            ConstraintObject(carbs, aapsLogger)
        ).value()
        val timeOffset = state.timeOffsetMinutes
        val duration = state.durationHours
        val notes = state.notes
        val useAlarm = state.alarmChecked
        val remindBolus = state.bolusReminderChecked
        val eventTime = state.eventTime

        // Negative carbs constraint
        if (carbsAfterConstraints < 0) {
            if (carbsAfterConstraints < -cob) carbsAfterConstraints = ceil(-cob).toInt()
            if (timeOffset != 0) carbsAfterConstraints = 0
        }

        // Insert temp target if selected
        val selectedTTDuration = when {
            state.activityTtChecked   -> state.activityTtDuration
            state.eatingSoonTtChecked -> state.eatingSoonTtDuration
            state.hypoTtChecked       -> state.hypoTtDuration
            else                      -> 0
        }
        val selectedTT = when {
            state.activityTtChecked   -> state.activityTtTarget
            state.eatingSoonTtChecked -> state.eatingSoonTtTarget
            state.hypoTtChecked       -> state.hypoTtTarget
            else                      -> 0.0
        }
        val reason = when {
            state.activityTtChecked   -> TT.Reason.ACTIVITY
            state.eatingSoonTtChecked -> TT.Reason.EATING_SOON
            state.hypoTtChecked       -> TT.Reason.HYPOGLYCEMIA
            else                      -> TT.Reason.CUSTOM
        }

        if (reason != TT.Reason.CUSTOM) {
            // appScope, not viewModelScope: the screen navigates back immediately after confirm,
            // which cancels viewModelScope and could drop this direct DB write.
            appScope.launch {
                try {
                    persistenceLayer.insertAndCancelCurrentTemporaryTarget(
                        temporaryTarget = TT(
                            timestamp = System.currentTimeMillis(),
                            duration = TimeUnit.MINUTES.toMillis(selectedTTDuration.toLong()),
                            reason = reason,
                            lowTarget = profileUtil.convertToMgdl(selectedTT, state.units),
                            highTarget = profileUtil.convertToMgdl(selectedTT, state.units)
                        ),
                        action = Action.TT,
                        source = Sources.CarbDialog,
                        note = null,
                        listValues = listOf(
                            ValueWithUnit.TETTReason(reason),
                            ValueWithUnit.fromGlucoseUnit(selectedTT, state.units),
                            ValueWithUnit.Minute(selectedTTDuration)
                        )
                    )
                } catch (e: Exception) {
                    aapsLogger.error(LTag.UI, "Failed to save temp target", e)
                }
            }
        }

        // Carbs delivery now rides the shared executor (one audited path): it logs the user entry + delivers.
        if (carbsAfterConstraints != 0) {
            if (config.AAPSCLIENT) {
                // Client routes carbs through the master too (maintainer decision): the master records them and the
                // client confirms its number. Carbs are carbs-only (no pump gate / record-only forcing like insulin).
                appScope.launch {
                    confirmRemoteBolus(carbsAfterConstraints, timeOffset, duration, notes)
                    // Mirror the local path's opt-in bolus reminder — a device-LOCAL notification, so it must be
                    // scheduled on the client even though the master records the carbs. Optimistic: confirmRemoteBolus
                    // is a fire-and-forget signed round-trip with no delivery-success hook.
                    if (preferences.get(BooleanKey.OverviewUseBolusReminder) && remindBolus)
                        automation.scheduleAutomationEventBolusReminder()
                    automation.removeAutomationEventEatReminder()
                }
            } else {
                appScope.launch {
                    wizardBolusExecutor.deliverECarbs(
                        carbs = carbsAfterConstraints,
                        carbsTime = eventTime,
                        duration = duration,
                        delayMinutes = timeOffset,
                        notes = notes,
                        source = Sources.CarbDialog,
                        onError = { _sideEffect.tryEmit(SideEffect.ShowDeliveryError(it)) },
                        onSuccess = {
                            if (preferences.get(BooleanKey.OverviewUseBolusReminder) && remindBolus)
                                automation.scheduleAutomationEventBolusReminder()
                        }
                    )
                    automation.removeAutomationEventEatReminder()
                }
            }
        }

        // Schedule eat reminder alarm
        if (useAlarm && carbs > 0 && timeOffset > 0) {
            automation.scheduleTimeToEatReminder(T.mins(timeOffset.toLong()).secs().toInt())
        }
    }
}
