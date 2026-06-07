package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * Master→client acknowledgement for a single client-control command, signed with the same shared
 * HMAC secret as the inbound [SignedEnvelope] so the client can prove the result genuinely came from
 * its paired master (a forged "Ok" must not be able to make a client believe a therapy action applied).
 *
 * Two-step lifecycle, written to the per-client identifier `aaps_clientcontrol_ack_<clientId>`
 * (overwritten in place):
 * 1. [AckPhase.Executing] / [AckStatus.Pending] — written before dispatch: "received it, applying now".
 * 2. [AckPhase.Done] / [AckStatus.Ok] | [AckStatus.Failed] | [AckStatus.Expired] — the terminal result.
 *
 * [commandCounter] echoes the command envelope's `counter`, the correlation id the client matches
 * against its outstanding request (a stale ack from a previous command carries a different counter
 * and is ignored).
 */
@Serializable
data class AckEnvelope(
    val clientId: String,
    val commandCounter: Long,
    val phase: AckPhase,
    val status: AckStatus,
    val reason: String? = null,
    val timestamp: Long,
    val signature: String
) {

    /** Byte-stable HMAC input over every field except the signature. */
    fun canonicalString(): String =
        "$clientId|$commandCounter|$phase|$status|${reason ?: ""}|$timestamp"
}

enum class AckPhase { Executing, Done }

enum class AckStatus { Pending, Ok, Failed, Expired }
