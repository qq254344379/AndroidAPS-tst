package app.aaps.core.interfaces.clientcontrol

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
     * Low-level: send [command] and emit progress until a terminal [ActionProgress]
     * ([ActionProgress.Applied] / [ActionProgress.Rejected] / [ActionProgress.Unconfirmed]). Most
     * callers should use [run] instead — it also drives the single app-level modal ([actionProgress]).
     */
    fun dispatch(command: Command): Flow<ActionProgress>

    /**
     * Run [command] and drive the **single, app-level** pending modal ([actionProgress]) for its whole
     * lifecycle, returning the terminal [ActionProgress] for the caller's feature-specific follow-up
     * (e.g. a snackbar / data refresh on [ActionProgress.Applied]). The modal is hosted once (in the
     * activity) and is feature-independent — callers don't render it themselves. Round-trips are
     * single-in-flight, so there's at most one modal at a time.
     */
    suspend fun run(command: Command): ActionProgress

    /**
     * The single client-control pending-modal signal, fed by every [run] regardless of which feature
     * triggered it. Non-null while the modal should show; cleared on success / dismiss. Hosted once at
     * the app root. Default empty for impls without the channel.
     */
    val actionProgress: StateFlow<ActionProgress?> get() = NO_ACTION_PROGRESS

    /** Dismiss the modal (terminal Rejected/Unconfirmed) or stop waiting on an in-flight [run]. */
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
    }

    companion object {

        /** Minimum time a round-trip pending modal stays up, so a sub-second round trip doesn't flash.
         *  Shared by every modal driven off this dispatcher (pref edits and the insulin activation). */
        const val MIN_MODAL_VISIBLE_MS = 1_500L

        // Shared empty flow for the default [actionProgress] (impls without the channel) —
        // a single instance, so the default getter doesn't allocate per call.
        private val NO_ACTION_PROGRESS: StateFlow<ActionProgress?> = MutableStateFlow(null)
    }
}
