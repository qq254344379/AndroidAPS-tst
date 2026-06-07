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
     * Send [command] to the master and emit progress until a terminal [ActionProgress]
     * ([ActionProgress.Applied] / [ActionProgress.Rejected] / [ActionProgress.Unconfirmed]).
     *
     * [command] is the same JSON shape carried by the corresponding `ClientControlMessage` variant.
     * Phase 1 supports only [Command.InsulinActivate].
     */
    fun dispatch(command: Command): Flow<ActionProgress>

    /**
     * Ambient progress for a "settings edit" round-trip that isn't tied to a single screen's collect
     * (a synced-preference edit can originate from any settings screen). Non-null while an app-level
     * pending modal should show; cleared on success or via [dismissPreferenceEditProgress]. Phase-3.3
     * spike. Default empty for impls without the channel.
     */
    val preferenceEditProgress: StateFlow<ActionProgress?> get() = NO_PREFERENCE_EDIT

    /** Dismiss the ambient preference-edit modal (terminal Rejected/Unconfirmed states). */
    fun dismissPreferenceEditProgress() {}

    /** The closed set of actions dispatchable through the round-trip channel. */
    sealed interface Command {

        /** Activate the insulin described by [iCfgJson] (the JSON `ICfg` serializes to). */
        data class InsulinActivate(val iCfgJson: String) : Command

        /**
         * Push bidirectionally-synced preference edits (keyString → serialized value + lastModified).
         * The master applies LWW and republishes; the ACK resolves the round-trip.
         */
        data class PreferenceEdit(val prefs: Map<String, Pair<String, Long>>) : Command
    }

    private companion object {

        // Shared empty flow for the default [preferenceEditProgress] (impls without the channel) —
        // a single instance, so the default getter doesn't allocate per call.
        private val NO_PREFERENCE_EDIT: StateFlow<ActionProgress?> = MutableStateFlow(null)
    }
}
