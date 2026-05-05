package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.localmodel.clientcontrol.HelloMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Client-side publisher for client-control envelopes.
 *
 * Writes signed envelopes into the NS `settings` collection under deterministic
 * identifiers the master polls for:
 * - `aaps_clientcontrol_hello_<clientId>` — first message after pairing
 * - (later) `aaps_clientcontrol_cmd_<clientId>_<counter>` for commands
 *
 * Settings collection is reused as the transport because the existing sync layer
 * already authenticates writes against NS and provides an obvious lifecycle. A
 * dedicated `clientControl` collection would be cleaner long-term but requires
 * NS server-side schema work; settings is good enough for v1.
 *
 * The doc carries `date`/`utcOffset`/`app` to satisfy `validateCommon` plus a
 * `schemaVersion` field for forward compatibility, with the signed envelope
 * embedded under `envelope`.
 */
@Singleton
class ClientControlPublisher @Inject constructor(
    private val pairingRepository: ClientPairingRepository,
    private val nsClientV3Plugin: Provider<NSClientV3Plugin>,
    private val nsClientRepository: NSClientRepository,
    private val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger
) {

    companion object {

        const val IDENTIFIER_PREFIX = "aaps_clientcontrol_"
        const val IDENTIFIER_HELLO_PREFIX = "${IDENTIFIER_PREFIX}hello_"
        const val SCHEMA_VERSION = 1
    }

    private val json = Json { encodeDefaults = true }

    /**
     * Build + sign a hello envelope for the current pairing and write it to NS.
     * Returns true on HTTP 2xx, false otherwise (caller logs / surfaces).
     */
    suspend fun publishHello(): Boolean {
        val pairing = pairingRepository.currentPairing() ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: publishHello called while unpaired")
            return false
        }
        val helloJson = json.encodeToString(HelloMessage.serializer(), HelloMessage())
        val envelope = pairingRepository.nextSignedEnvelope("hello", helloJson, dateUtil.now()) ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: failed to build signed hello envelope")
            return false
        }
        val identifier = "$IDENTIFIER_HELLO_PREFIX${pairing.clientId}"
        return uploadEnvelope(identifier, envelope)
    }

    private suspend fun uploadEnvelope(identifier: String, envelope: SignedEnvelope): Boolean {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: NS client not initialized")
            return false
        }
        val envelopeJson = json.encodeToString(SignedEnvelope.serializer(), envelope)
        val doc = JSONObject().apply {
            // validateCommon requires these on UPDATE; sender's clock at message-mint time is the
            // most useful "date" for the receiver to inspect. utcOffset stays zero — the timestamp
            // in the envelope is millis-epoch and self-describing.
            put("date", envelope.timestamp)
            put("utcOffset", 0)
            put("app", "AAPS")
            put("schemaVersion", SCHEMA_VERSION)
            put("envelope", JSONObject(envelopeJson))
        }
        return runCatching { client.updateSettings(identifier, doc) }
            .onSuccess { resp ->
                if (resp.response in 200..299) {
                    nsClientRepository.addLog("► CLIENTCTL", "$identifier counter=${envelope.counter}")
                } else {
                    aapsLogger.error(LTag.NSCLIENT, "ClientControl publish HTTP ${resp.response}: ${resp.errorResponse}")
                    nsClientRepository.addLog("✕ CLIENTCTL", "$identifier HTTP ${resp.response}")
                }
            }
            .onFailure { aapsLogger.error(LTag.NSCLIENT, "ClientControl publish exception: ${it.message}", it) }
            .map { it.response in 200..299 }
            .getOrDefault(false)
    }
}
