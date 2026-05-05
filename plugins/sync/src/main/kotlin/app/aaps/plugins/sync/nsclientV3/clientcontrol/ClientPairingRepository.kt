package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.protection.SecureEncrypt
import app.aaps.core.keys.LongNonKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.localmodel.clientcontrol.MasterPairing
import app.aaps.core.nssdk.localmodel.clientcontrol.PairingPayload
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.core.nssdk.utils.ClientControlCrypto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side store of the single master pairing.
 *
 * Symmetric to [AuthorizedClientsRepository] on the master side — the secret bytes are
 * wrapped via [SecureEncrypt] (AndroidKeyStore-backed AES/GCM) before persistence and
 * unwrapped only on demand. Plaintext never sits in shared prefs.
 *
 * Replay protection on the client side rests on a single monotonically-increasing counter
 * stored in [LongNonKey.NsClientControlCounterSent]. The counter is incremented inside
 * [nextSignedEnvelope] under a process-local lock so concurrent send paths cannot reuse
 * a value. A backup-restore that regresses the counter will cause the next message to be
 * rejected by the master (counter ≤ counterReceived); failure mode is "client must
 * re-pair", not "replay window opens".
 */
@Singleton
class ClientPairingRepository @Inject constructor(
    private val preferences: Preferences,
    private val secureEncrypt: SecureEncrypt,
    private val aapsLogger: AAPSLogger
) {

    companion object {

        const val SECURE_ENCRYPT_ALIAS = "NsClientControlSecret"
    }

    private val lock = Any()

    /** True iff a pairing is currently stored (clientId is the canonical "paired" marker). */
    fun isPaired(): Boolean = preferences.get(StringNonKey.NsClientControlClientId).isNotEmpty()

    /** Returns the assembled pairing snapshot or null if not paired / corrupt. */
    fun currentPairing(): MasterPairing? = synchronized(lock) {
        val masterInstallId = preferences.get(StringNonKey.NsClientControlMasterInstallId)
        val clientId = preferences.get(StringNonKey.NsClientControlClientId)
        val secretEnc = preferences.get(StringNonKey.NsClientControlMasterSecretEnc)
        if (masterInstallId.isEmpty() || clientId.isEmpty() || secretEnc.isEmpty()) return@synchronized null
        MasterPairing(
            masterInstallId = masterInstallId,
            clientId = clientId,
            masterSecretEnc = secretEnc
        )
    }

    /**
     * Persist a freshly-scanned [PairingPayload]. Overwrites any existing pairing — the
     * caller is expected to confirm with the user before calling. Resets the counter so
     * the first signed message after pairing is counter=1.
     */
    fun pair(payload: PairingPayload): Unit = synchronized(lock) {
        preferences.put(StringNonKey.NsClientControlMasterInstallId, payload.masterInstallId)
        preferences.put(StringNonKey.NsClientControlClientId, payload.clientId)
        preferences.put(StringNonKey.NsClientControlMasterSecretEnc, secureEncrypt.encrypt(payload.secretHex, SECURE_ENCRYPT_ALIAS))
        preferences.put(LongNonKey.NsClientControlCounterSent, 0L)
    }

    /** Clears all pairing state. Counter reset stops a re-pair from inheriting the old counter. */
    fun unpair(): Unit = synchronized(lock) {
        preferences.put(StringNonKey.NsClientControlMasterInstallId, "")
        preferences.put(StringNonKey.NsClientControlClientId, "")
        preferences.put(StringNonKey.NsClientControlMasterSecretEnc, "")
        preferences.put(LongNonKey.NsClientControlCounterSent, 0L)
    }

    /**
     * Increments the counter atomically, signs an envelope of the given [type] over [payloadJson],
     * and returns it ready to ship. Returns null when:
     * - not paired
     * - encrypted secret blob fails [SecureEncrypt.isValidDataString] (corrupted)
     * - decrypt yields empty (KeyStore reset / backup-restore — typically requires re-pair)
     * - secret hex is malformed
     *
     * Each `null` path is logged separately so a backup-restore failure produces a diagnostic
     * instead of silent send drops.
     */
    fun nextSignedEnvelope(type: String, payloadJson: String, timestamp: Long): SignedEnvelope? = synchronized(lock) {
        val pairing = currentPairing() ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: nextSignedEnvelope called while unpaired")
            return@synchronized null
        }
        if (!secureEncrypt.isValidDataString(pairing.masterSecretEnc)) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: stored secret blob is corrupted (re-pair required)")
            return@synchronized null
        }
        val secretHex = secureEncrypt.decrypt(pairing.masterSecretEnc)
        if (secretHex.isEmpty()) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: secret decrypt failed (KeyStore reset?) — re-pair required")
            return@synchronized null
        }
        val secretBytes = runCatching { ClientControlCrypto.hexToBytes(secretHex) }.getOrNull()
        if (secretBytes == null) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: secret hex is malformed")
            return@synchronized null
        }
        val nextCounter = preferences.get(LongNonKey.NsClientControlCounterSent) + 1L
        preferences.put(LongNonKey.NsClientControlCounterSent, nextCounter)
        val draft = SignedEnvelope(
            clientId = pairing.clientId,
            counter = nextCounter,
            timestamp = timestamp,
            type = type,
            payload = payloadJson,
            signature = ""
        )
        ClientControlCrypto.signEnvelope(secretBytes, draft)
    }
}
