package app.aaps.core.interfaces.scenes

/**
 * Outcome of a [ClientControlSceneSender] call. Three variants distinguish the cases the call
 * site should treat differently in UI:
 *
 * - [Success]: the envelope reached NS with HTTP 2xx. Master will dispatch (or has already);
 *   no user action needed.
 * - [NotPaired]: this device has no master pairing. Recoverable by the user — they need to
 *   complete the pairing flow before scene control can work.
 * - [PublishFailed]: NS upload failed (no client, HTTP error, exception). Transient — retry
 *   when connectivity recovers. [reason] is for logs / dialog detail.
 *
 * Boolean was the original return type; collapsing the three cases into a single `false` hid
 * the not-paired case from the user, who would otherwise tap "Start scene" forever wondering
 * why nothing happened on the master.
 */
sealed class ClientControlSendResult {

    data object Success : ClientControlSendResult()

    data object NotPaired : ClientControlSendResult()

    data class PublishFailed(val reason: String? = null) : ClientControlSendResult()
}
