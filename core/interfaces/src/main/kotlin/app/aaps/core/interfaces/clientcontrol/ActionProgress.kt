package app.aaps.core.interfaces.clientcontrol

/**
 * Progress/outcome of an action dispatched through [ClientControlActionDispatcher]. A `dispatch`
 * emits a sequence ending in exactly one terminal value.
 *
 * The split between [Rejected] and [Unconfirmed] is the whole point of the round-trip: [Rejected]
 * means "definitely not applied" (safe to tell the user nothing happened), while [Unconfirmed] means
 * "unknown" — the command was sent but no result came back in time, so the real state must be read
 * from normal sync-back, not assumed. A therapy action must never be reported as done on uncertainty.
 */
sealed interface ActionProgress {

    /** Intermediate: the command is being uploaded. */
    data object Sending : ActionProgress

    /** Intermediate (remote only): the master acknowledged receipt and is applying the command. */
    data object MasterExecuting : ActionProgress

    /** Terminal: applied successfully (locally, or confirmed by the master's Done/Ok ack). */
    data object Applied : ActionProgress

    /** Terminal: definitely not applied — master refused / failed, or it never reached NS. */
    data class Rejected(val reason: String?) : ActionProgress

    /** Terminal (remote only): sent, but no confirmation before the deadline / connection lost. State unknown. */
    data class Unconfirmed(val reason: String?) : ActionProgress
}
