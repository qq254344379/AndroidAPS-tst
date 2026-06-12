package app.aaps.core.interfaces.clientcontrol

import app.aaps.core.interfaces.bolus.BatchAction
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Round-trip dispatcher for a paired AAPSCLIENT: sends a client-control command to the master and
 * surfaces the master's two-step acknowledgement as an [ActionProgress] stream, so a client UI can
 * wait for the real result and close like a local execution.
 *
 * Implemented in `:plugins:sync` (where the wire format + signing live); declared here so `:ui`
 * consumers depend only on `core:interfaces`. Phase 1 wires the insulin-activate action; other
 * client-control actions migrate onto this in Phase 2.
 */
interface ClientControlActionDispatcher {

    /**
     * Low-level: send [command] and emit progress until a terminal [ActionProgress]. Most callers
     * should use [execute] (or [run]) instead — those also drive the single app-level modal.
     */
    fun dispatch(command: Command): Flow<ActionProgress>

    /**
     * Run [command] over the round-trip (client side) and drive the **single, app-level** pending modal
     * ([pendingAction]) for its whole lifecycle, returning the terminal [ActionProgress]. [label] is a
     * short, already-localized description of the action shown in the modal. Round-trips are
     * single-in-flight, so there's at most one modal at a time.
     */
    suspend fun run(command: Command, label: String): ActionProgress

    /**
     * **The generalization point for user-initiated actions.** Resolves [command] for the right role
     * and surfaces the result through the one app-level modal for BOTH roles:
     *  - **client** → the round-trip ([run]);
     *  - **master** → runs [localExecute] and, if it fails (non-[ActionProgress.Applied]), shows the
     *    same modal (success is silent — instant).
     *
     * [label] is a short, already-localized action description ("set temp target", "activate scene
     * Sleep"). Returns the terminal for the caller's success follow-up. **Do not** use this for inbound
     * remote commands applied on the master — those must not pop a master dialog (use the domain API).
     */
    suspend fun execute(command: Command, label: String, localExecute: suspend () -> ActionProgress): ActionProgress

    /**
     * The single client-control modal signal (progress + label), fed by every [run]/[execute]
     * regardless of feature. Non-null while the modal should show; cleared on success / dismiss. Hosted
     * once at the app root. Default empty for impls without the channel.
     */
    val pendingAction: StateFlow<PendingAction?> get() = NO_PENDING_ACTION

    /** Dismiss the modal (terminal Rejected/Unconfirmed) or stop waiting on an in-flight action. */
    fun dismissActionProgress() {}

    /** The closed set of actions dispatchable through the round-trip channel. */
    sealed interface Command {

        /** Activate the insulin described by [iCfgJson] (the JSON `ICfg` serializes to). */
        data class InsulinActivate(val iCfgJson: String) : Command

        /**
         * Push bidirectionally-synced preference edits (keyString → serialized value + lastModified).
         * The master applies LWW and republishes; the ACK resolves the round-trip.
         */
        data class PreferenceEdit(val prefs: Map<String, Pair<String, Long>>) : Command

        /** Activate the scene [sceneId] on the master (null duration → the scene's stored default). */
        data class SceneStart(val sceneId: String, val durationMinutes: Int?) : Command

        /** Deactivate the master's active scene; [triggerChain] = also fire its configured chain target. */
        data class SceneStop(val triggerChain: Boolean) : Command

        /** Set a temp target on the master ([timestamp] = intended start time; targets mg/dL; reason text). */
        data class TempTargetSet(
            val timestamp: Long,
            val lowTargetMgdl: Double,
            val highTargetMgdl: Double,
            val durationMinutes: Int,
            val reason: String
        ) : Command

        /** Cancel the master's currently active temp target. */
        data object TempTargetCancel : Command

        /** Prepare a QuickWizard (WIZARD-mode) bolus on the master ([guid] = the synced entry); master returns the preview. */
        data class BolusPrepare(val guid: String) : Command

        /** Confirm a prepared bolus by [bolusId] (master delivers once); [asAdvisor] = the high-BG correction-only branch. */
        data class BolusCommit(val bolusId: Long, val asAdvisor: Boolean = false) : Command

        /** Prepare a MANUAL bolus-wizard bolus on the master from raw [inputs] (master recomputes + returns the preview). */
        data class WizardPrepare(val inputs: WizardBolusExecutor.WizardInputs) : Command

        /** PREPARE a multi-action batch on the master — TWO-STEP: master caps + returns the MERGED confirmation; commit via [BolusCommit]. */
        data class BatchPrepare(val actions: List<BatchAction>) : Command
    }

    companion object {

        /** Minimum time a round-trip pending modal stays up, so a sub-second round trip doesn't flash.
         *  Shared by every modal driven off this dispatcher (pref edits and the insulin activation). */
        const val MIN_MODAL_VISIBLE_MS = 1_500L

        // Shared empty flow for the default [pendingAction] (impls without the channel) —
        // a single instance, so the default getter doesn't allocate per call.
        private val NO_PENDING_ACTION: StateFlow<PendingAction?> = MutableStateFlow(null)
    }
}
