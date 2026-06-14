package app.aaps.core.interfaces.bolus

import app.aaps.core.data.model.ICfg
import app.aaps.core.data.model.RM

/**
 * One action in a multi-action batch ([WizardBolusExecutor.prepareBatch]) — the dose/carbs a dialog submits
 * alongside a temp target, applied by the master in one transaction. Domain form; the wire form is
 * `BatchActionDto` in `:core:nssdk`. A batch carries **at most one** [Bolus], **at most one** [TempTarget],
 * and **at most one** [ProfileSwitch].
 */
sealed interface BatchAction {

    /**
     * A FIXED bolus/carbs (capped, never recomputed). [recordOnly] persists it without a pump command (a pen
     * bolus the user gave outside AAPS) — and is **not** constraint-capped (a record of what was given). [iCfg]
     * is the logged insulin's config for the record-only case; null for a delivery (the master uses its active).
     */
    data class Bolus(
        val insulin: Double,
        val carbs: Int,
        val carbsTimeOffsetMinutes: Int,
        val carbsDurationHours: Int,
        val recordOnly: Boolean,
        val notes: String,
        val timestamp: Long,
        val iCfg: ICfg?
    ) : BatchAction

    /**
     * A temp target. [startOffsetMinutes] is from now (an eating-soon/activity TT starts now even when the
     * bolus is back-dated). [reason] is the [app.aaps.core.data.model.TT.Reason] text.
     */
    data class TempTarget(
        val reason: String,
        val lowMgdl: Double,
        val highMgdl: Double,
        val durationMinutes: Int,
        val startOffsetMinutes: Int,
        val notes: String? = null
    ) : BatchAction

    /**
     * A profile switch by [percentage] / [timeShiftHours] for [durationMinutes] (0 = until the next switch).
     * [profileName] null → switch the **currently active** profile (wear / CPP); non-null → switch to that
     * **named** profile from the master's profile store. The master resolves the store by name, so a client can
     * relay a named switch the master executes. [notes] is an optional user note.
     */
    data class ProfileSwitch(
        val percentage: Int,
        val timeShiftHours: Int,
        val durationMinutes: Int,
        val profileName: String? = null,
        val notes: String? = null
    ) : BatchAction

    /**
     * A loop running-mode change ([RM.Mode]). [durationMinutes] is required (>0) for the temporary modes
     * (SUSPENDED_BY_USER / DISCONNECTED_PUMP) and 0 for the loop modes. The master re-validates the [mode] is a
     * legal transition (`Loop.allowedNextModes`) at prepare time, so a client/watch can relay a mode the master owns.
     */
    data class RunningMode(
        val mode: RM.Mode,
        val durationMinutes: Int = 0
    ) : BatchAction
}
