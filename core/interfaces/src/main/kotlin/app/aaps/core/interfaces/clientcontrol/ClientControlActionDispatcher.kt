package app.aaps.core.interfaces.clientcontrol

import kotlinx.coroutines.flow.Flow

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

    /** The closed set of actions dispatchable through the round-trip channel. */
    sealed interface Command {

        /** Activate the insulin described by [iCfgJson] (the JSON `ICfg` serializes to). */
        data class InsulinActivate(val iCfgJson: String) : Command
    }
}
