package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * Master-local record of a client allowed (or pending allowance) to send
 * commands. Persisted as JSON inside `StringNonKey.NsClientControlAuthorizedClients`.
 *
 * `encryptedSecret` is the SecureEncrypt-wrapped HMAC secret (AndroidKeyStore-
 * backed AES/GCM). The plaintext bytes never sit in shared preferences, so a
 * leaked prefs file is useless without TEE access.
 *
 * Lifecycle:
 * - `Pending` from QR generation; auto-pruned after `qrExpiresAt`.
 * - `Active` after first signed `hello` verifies; `lastSeenAt` updated on
 *   every accepted command.
 *
 * `counterReceived` is the highest counter value already accepted from this
 * client — strictly increasing.
 */
@Serializable
data class AuthorizedClient(
    val clientId: String,
    val name: String,
    val encryptedSecret: String,
    val capabilities: List<String>,
    val state: ClientState,
    val createdAt: Long,
    val lastSeenAt: Long = 0L,
    val counterReceived: Long = 0L,
    val qrExpiresAt: Long = 0L
)

@Serializable
enum class ClientState {

    Pending, Active
}
