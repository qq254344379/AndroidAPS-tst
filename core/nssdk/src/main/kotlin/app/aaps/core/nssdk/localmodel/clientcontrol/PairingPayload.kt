package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * Contents of the pairing QR shown by the master and scanned by the client.
 *
 * Carries everything a client needs to enroll: identity of the master, the
 * clientId the master assigned to this pairing, the shared HMAC secret, the
 * set of capabilities the master is willing to grant, and a hard expiry after
 * which the master refuses the resulting `hello`.
 *
 * The QR is sensitive — the screen showing it should be FLAG_SECURE and not
 * leak via screenshots. Treat the JSON as a one-time bearer token.
 */
@Serializable
data class PairingPayload(
    val v: Int = 1,
    val masterInstallId: String,
    val clientId: String,
    val secretHex: String,
    val capabilities: List<String>,
    val expiresAt: Long
)
