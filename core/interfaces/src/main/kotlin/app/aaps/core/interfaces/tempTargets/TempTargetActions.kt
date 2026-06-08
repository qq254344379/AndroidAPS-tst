package app.aaps.core.interfaces.tempTargets

import app.aaps.core.data.model.TT
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.clientcontrol.ActionProgress

/**
 * Per-domain execution facade for setting/cancelling a temp target, hiding the master/client split
 * (mirrors [app.aaps.core.interfaces.scenes.SceneActions] / `InsulinActions`):
 *  - **master** writes via the persistence layer and returns the terminal outcome immediately;
 *  - **client** goes through the confirmed client→master round-trip (drives the single app-level modal)
 *    and returns the master's terminal [ActionProgress].
 *
 * Both paths resolve the result — every legacy caller ignored the `TransactionResult`, and a client
 * was blind fire-and-forget. Now callers get [ActionProgress.Applied]/[ActionProgress.Rejected]/
 * [ActionProgress.Unconfirmed].
 */
interface TempTargetActions {

    /**
     * Set a temp target. [timestamp] is the intended start time (supports a back-dated event time);
     * [lowMgdl]/[highMgdl] are mg/dL (equal for a single target). [source]/[note] feed the master-local
     * UserEntry log (on a client the master logs the applied command itself).
     */
    suspend fun set(
        reason: TT.Reason,
        lowMgdl: Double,
        highMgdl: Double,
        durationMinutes: Int,
        timestamp: Long,
        source: Sources,
        note: String?
    ): ActionProgress

    /** Cancel the currently active temp target (master cancels at its own "now"). */
    suspend fun cancel(source: Sources, note: String?): ActionProgress
}
