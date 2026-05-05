package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.protection.SecureEncrypt
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.localmodel.clientcontrol.AuthorizedClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.utils.ClientControlCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Master-side store of clients authorized to send commands.
 *
 * Persists a JSON array in [StringNonKey.NsClientControlAuthorizedClients].
 * Plaintext HMAC secrets never sit in prefs — the bytes are wrapped through
 * [SecureEncrypt] (AndroidKeyStore-backed AES/GCM) before persistence and
 * unwrapped only on demand.
 *
 * All mutations and the prune-on-read path go through a single intrinsic lock
 * so the UI thread (delete/add) cannot race the WS handler thread (markActive
 * / bumpLastSeen). Without this, a bump racing a delete could re-write the
 * deleted entry and silently re-admit a revoked client — a safety concern on
 * a pump-control surface.
 */
@Singleton
class AuthorizedClientsRepository @Inject constructor(
    private val preferences: Preferences,
    private val secureEncrypt: SecureEncrypt,
    private val aapsLogger: AAPSLogger
) {

    companion object {

        const val SECURE_ENCRYPT_ALIAS = "NsClientControlSecret"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    /** Current list with expired pending entries pruned. Side-effects prefs if any were pruned. */
    fun current(now: Long): List<AuthorizedClient> = synchronized(lock) {
        val list = decode()
        val kept = list.filter { it.state != ClientState.Pending || it.qrExpiresAt > now }
        if (kept.size != list.size) write(kept)
        kept
    }

    /** Flow of the list (raw — does not auto-prune; callers can prune via [pruneExpired]). */
    fun observe(): Flow<List<AuthorizedClient>> =
        preferences.observe(StringNonKey.NsClientControlAuthorizedClients).map { decode(it) }

    /**
     * Create a new pending pairing. Returns the freshly-generated secret in **plaintext hex**
     * for QR rendering — caller must not persist it. The encrypted form is stored on the entry.
     */
    fun addPending(name: String, capabilities: List<String>, qrTtlMs: Long, now: Long): PendingResult = synchronized(lock) {
        val secretBytes = ClientControlCrypto.newSecretBytes()
        val secretHex = ClientControlCrypto.bytesToHex(secretBytes)
        val entry = AuthorizedClient(
            clientId = ClientControlCrypto.newClientId(),
            name = name,
            encryptedSecret = secureEncrypt.encrypt(secretHex, SECURE_ENCRYPT_ALIAS),
            capabilities = capabilities,
            state = ClientState.Pending,
            createdAt = now,
            qrExpiresAt = now + qrTtlMs
        )
        write(decode() + entry)
        PendingResult(entry, secretHex)
    }

    fun delete(clientId: String): Unit = synchronized(lock) {
        write(decode().filterNot { it.clientId == clientId })
    }

    /** Promote a pending entry to active on first verified hello. No-op if not present or already active. */
    fun markActive(clientId: String, counterReceived: Long, now: Long): Unit = synchronized(lock) {
        val list = decode()
        val updated = list.map {
            if (it.clientId == clientId && it.state == ClientState.Pending)
                it.copy(state = ClientState.Active, counterReceived = counterReceived, lastSeenAt = now)
            else it
        }
        if (updated != list) write(updated)
    }

    /** Update lastSeen + counter after an accepted command. No-op if entry missing. */
    fun bumpLastSeen(clientId: String, counterReceived: Long, now: Long): Unit = synchronized(lock) {
        val list = decode()
        val updated = list.map {
            if (it.clientId == clientId) it.copy(counterReceived = counterReceived, lastSeenAt = now) else it
        }
        if (updated != list) write(updated)
    }

    /** Drop pending entries past their qrExpiresAt. Returns number removed. */
    fun pruneExpired(now: Long): Int = synchronized(lock) {
        val list = decode()
        val kept = list.filter { it.state != ClientState.Pending || it.qrExpiresAt > now }
        if (kept.size != list.size) {
            write(kept); list.size - kept.size
        } else 0
    }

    /**
     * Resolves the HMAC secret + the last-accepted counter for [clientId] in one shot. Returning
     * both forces the caller to compare the incoming counter against `counterReceived` before
     * accepting a message — a separate `secretBytesFor` would let callers skip the replay check.
     *
     * Returns null in three distinct cases (logged separately so a backup-restore / KeyStore
     * reset does not silently reject every command):
     * - entry missing for [clientId]
     * - stored encryptedSecret fails [SecureEncrypt.isValidDataString] (corrupted blob)
     * - decrypt produces empty result (KeyStore key gone — typical after backup-restore)
     */
    fun secretLookup(clientId: String): SecretLookup? = synchronized(lock) {
        val entry = decode().firstOrNull { it.clientId == clientId } ?: return@synchronized null
        if (!secureEncrypt.isValidDataString(entry.encryptedSecret)) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: stored secret blob for $clientId is corrupted")
            return@synchronized null
        }
        val hex = secureEncrypt.decrypt(entry.encryptedSecret)
        if (hex.isEmpty()) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: decrypt of secret for $clientId failed (KeyStore reset?)")
            return@synchronized null
        }
        val bytes = runCatching { ClientControlCrypto.hexToBytes(hex) }.getOrNull()
        if (bytes == null) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: secret hex for $clientId is malformed")
            return@synchronized null
        }
        SecretLookup(bytes, entry.counterReceived)
    }

    private fun decode(raw: String = preferences.get(StringNonKey.NsClientControlAuthorizedClients)): List<AuthorizedClient> =
        runCatching { json.decodeFromString<List<AuthorizedClient>>(raw) }.getOrNull() ?: emptyList()

    private fun write(list: List<AuthorizedClient>) {
        preferences.put(StringNonKey.NsClientControlAuthorizedClients, json.encodeToString(list))
    }

    data class PendingResult(val entry: AuthorizedClient, val secretHex: String)
    data class SecretLookup(val secretBytes: ByteArray, val counterReceived: Long) {

        override fun equals(other: Any?): Boolean =
            other is SecretLookup && secretBytes.contentEquals(other.secretBytes) && counterReceived == other.counterReceived

        override fun hashCode(): Int = secretBytes.contentHashCode() * 31 + counterReceived.hashCode()
    }
}
