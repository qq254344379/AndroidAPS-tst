package app.aaps.core.interfaces.bolus

import app.aaps.core.data.model.ICfg

/**
 * One action in a multi-action batch ([WizardBolusExecutor.prepareBatch]) — the dose/carbs a dialog submits
 * alongside a temp target, applied by the master in one transaction. Domain form; the wire form is
 * `BatchActionDto` in `:core:nssdk`. A batch carries **at most one** [Bolus] and **at most one** [TempTarget].
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
        val startOffsetMinutes: Int
    ) : BatchAction
}
