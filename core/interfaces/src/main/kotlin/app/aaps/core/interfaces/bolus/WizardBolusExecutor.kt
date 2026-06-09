package app.aaps.core.interfaces.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.ue.Sources

/**
 * Transport-neutral spine for wizard / quick-wizard bolus **prepare → confirm → deliver**. Owns the
 * single consume-once "pending bolus" slot shared by the full-wizard and quick-wizard prechecks, computes
 * the dose on the master's own current state, and delivers via the command queue (which re-applies bolus
 * constraints — the safety cap holds regardless of caller).
 *
 * One audited path a bolus reaches the pump from any trigger (wear today, client-control + the local UI
 * next): callers adapt only the transport — render the [PrepareResult.Preview], surface errors via the
 * [onError] sink, and tag the user entry with a [Sources].
 *
 * Declared here (interfaces only — no `core:objects` dependency, so it exposes primitives, not the
 * `BolusWizard` compute object); impl lives in `:implementation`. Idempotency: [confirm] drains the slot
 * and requires the bolusId to match, so a re-sent confirm finds it empty → never a double-bolus.
 */
interface WizardBolusExecutor {

    /** Park a pending bolus (the full-wizard precheck computes wear-side and stores its result here). */
    fun setPending(insulin: Double, carbs: Int, bolusCalculatorResult: BCR?, bolusId: Long)

    /** Drop any pending bolus (a non-wizard precheck supersedes it). */
    fun clearPending()

    /**
     * Quick-wizard prepare: gate → compute on current state → constraint-cap → park in the slot.
     * Returns the computed [PrepareResult.Preview] (or an [PrepareResult.Error]); the caller renders it
     * and, on the user's OK, calls [confirm] with the preview's `bolusId`.
     */
    suspend fun prepareQuickWizard(guid: String): PrepareResult

    /**
     * Drain the slot, verify [bolusId] matches the parked bolus, and deliver. Idempotent: a second
     * confirm finds the slot empty → [ConfirmResult.NoPending], never a second bolus.
     */
    suspend fun confirm(bolusId: Long, source: Sources, onError: (String) -> Unit): ConfirmResult

    /** Build + queue a bolus/carbs delivery. [onError] receives the mode-rejection and the async failure. */
    suspend fun deliver(
        amount: Double,
        carbs: Int,
        carbsTime: Long?,
        carbsDuration: Int,
        bolusCalculatorResult: BCR?,
        notes: String?,
        source: Sources,
        onError: (String) -> Unit
    )

    /** Prime/fill bolus (no carbs). */
    suspend fun deliverFillBolus(amount: Double, source: Sources, onError: (String) -> Unit)

    /** Extended/delayed carbs (delegates to [deliver] with zero insulin). */
    suspend fun deliverECarbs(carbs: Int, carbsTime: Long, duration: Int, notes: String?, source: Sources, onError: (String) -> Unit)

    sealed interface PrepareResult {

        /** Computed, constraint-capped, parked. [bolusId] is the confirm id; [explanation] is the short reasoning. */
        data class Preview(val insulin: Double, val carbs: Int, val explanation: String, val bolusId: Long) : PrepareResult
        data class Error(val message: String) : PrepareResult
    }

    sealed interface ConfirmResult {

        /** Bolus started (async). */
        data object Delivered : ConfirmResult

        /** Slot empty or id mismatch — nothing delivered (idempotent retry / stale confirm). */
        data object NoPending : ConfirmResult
    }
}
