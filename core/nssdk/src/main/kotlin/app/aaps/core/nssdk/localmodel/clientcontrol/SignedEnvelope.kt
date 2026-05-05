package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * Generic HMAC-signed envelope used for every client→master message
 * (hello, scene.start, scene.stop, …).
 *
 * `payload` is the already-serialized JSON of the inner message — kept as a
 * raw String so signature verification compares the *bytes that travelled the
 * wire*, immune to receiver-side JSON canonicalization differences.
 *
 * Replay protection rests on three fields together:
 * - `clientId` scopes the counter / secret to one pairing
 * - `counter` must be strictly greater than the master's last accepted value
 * - `timestamp` (millis epoch) must be within ±5 min of master clock
 *
 * Signature input is the canonical string produced by [canonicalString].
 */
@Serializable
data class SignedEnvelope(
    val clientId: String,
    val counter: Long,
    val timestamp: Long,
    val type: String,
    val payload: String,
    val signature: String
) {

    /**
     * Deterministic byte-stable representation of all fields except the
     * signature itself, used as HMAC input on both sender and receiver.
     */
    fun canonicalString(): String =
        "$clientId|$counter|$timestamp|$type|$payload"
}
