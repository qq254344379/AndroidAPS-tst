package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.ClientControlPreferencesSender
import app.aaps.core.interfaces.insulin.ClientControlInsulinSender
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.scenes.ClientControlSceneSender
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.PrefEntry
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
) : ClientControlSceneSender, ClientControlInsulinSender, ClientControlPreferencesSender {

    companion object {

        const val IDENTIFIER_PREFIX = "aaps_clientcontrol_"
        const val IDENTIFIER_HELLO_PREFIX = "${IDENTIFIER_PREFIX}hello_"
        const val IDENTIFIER_CMD_PREFIX = "${IDENTIFIER_PREFIX}cmd_"

        // Master-published pairing offers (PIN-wrapped PairingPayload). Not envelopes — the
        // receiver must skip these so it does not try to verify its own offer as a hello/cmd.
        const val IDENTIFIER_OFFER_PREFIX = "${IDENTIFIER_PREFIX}offer_"
        const val SCHEMA_VERSION = 1

        // 1 ms past NS APIv3 MIN_TIMESTAMP (946684800000 = 2000-01-01 UTC). validateCommon
        // requires `date` and the server rejects any modification on subsequent PUTs to the
        // same identifier ("Field date cannot be modified by the client"). The real
        // message timestamp lives in envelope.timestamp (signed); the doc-level date is
        // a constant placeholder only.
        const val DOC_DATE = 946684800001L
    }

    private val json = Json { encodeDefaults = true }

    /**
     * Build + sign an envelope carrying [message] and write it to the NS settings collection
     * under the identifier appropriate for that message family. Returns true on HTTP 2xx.
     *
     * **Identifier scheme:**
     * - Hello: `aaps_clientcontrol_hello_<clientId>`
     * - Commands: `aaps_clientcontrol_cmd_<type>_<clientId>` where `<type>` is the variant's
     *   polymorphic discriminator (e.g. `scene.start`). Per-type slots prevent cross-type
     *   collision (a fresh `scene.start` won't overwrite an unprocessed `scene.stop`).
     *
     * **Same-type latest-wins is intentional**: if the user fires two commands of the same
     * type before the master has acknowledged the first (e.g. tap "Sleep" then change mind
     * and tap "Exercise"), `updateSettings` with the same identifier overwrites the first.
     * Master only sees the latter — which matches user intent ("I changed my mind"). For
     * scene control with human-paced taps this is the right semantics; if a future variant
     * needs queueing (e.g. sequenced bolus commands), it must use a different identifier
     * scheme and is responsible for inventing one.
     *
     * The identifier selector is an exhaustive `when` so adding a new [ClientControlMessage]
     * variant forces a hello-vs-command classification at compile time.
     */
    suspend fun publish(message: ClientControlMessage): ClientControlSendResult {
        val pairing = pairingRepository.currentPairing() ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: publish called while unpaired")
            return ClientControlSendResult.NotPaired
        }
        val payload = json.encodeToString(ClientControlMessage.serializer(), message)
        // Derive envelope.type from the payload's polymorphic discriminator so the two cannot
        // drift — there is exactly one source of truth (the variant's @SerialName).
        val type = json.parseToJsonElement(payload).jsonObject["type"]?.jsonPrimitive?.content ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: serializer produced no discriminator for $message")
            return ClientControlSendResult.PublishFailed("no discriminator")
        }
        val envelope = pairingRepository.nextSignedEnvelope(type, payload, dateUtil.now()) ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: failed to sign envelope for type=$type")
            return ClientControlSendResult.PublishFailed("sign failed")
        }
        val identifier = when (message) {
            is ClientControlMessage.Hello -> "$IDENTIFIER_HELLO_PREFIX${pairing.clientId}"
            is ClientControlMessage.SceneStart,
            is ClientControlMessage.SceneStop,
            is ClientControlMessage.InsulinActivate,
            is ClientControlMessage.PreferencesUpdate -> "$IDENTIFIER_CMD_PREFIX${type}_${pairing.clientId}"
        }
        return uploadEnvelope(identifier, envelope)
    }

    override suspend fun sendSceneStart(sceneId: String, durationMinutes: Int?): ClientControlSendResult =
        publish(ClientControlMessage.SceneStart(sceneId, durationMinutes))

    override suspend fun sendSceneStop(triggerChain: Boolean): ClientControlSendResult =
        publish(ClientControlMessage.SceneStop(triggerChain))

    override suspend fun sendInsulinActivate(iCfgJson: String): ClientControlSendResult =
        publish(ClientControlMessage.InsulinActivate(iCfgJson))

    override suspend fun sendPreferencesUpdate(changes: Map<String, Pair<String, Long>>): ClientControlSendResult =
        publish(ClientControlMessage.PreferencesUpdate(changes.mapValues { (_, v) -> PrefEntry(value = v.first, lastModified = v.second) }))

    private suspend fun uploadEnvelope(identifier: String, envelope: SignedEnvelope): ClientControlSendResult {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: NS client not initialized")
            return ClientControlSendResult.PublishFailed("NS client not initialized")
        }
        val envelopeJson = json.encodeToString(SignedEnvelope.serializer(), envelope)
        val doc = JSONObject().apply {
            // validateCommon requires date/utcOffset/app, but `date` is immutable after first
            // create — sending the live envelope timestamp here fails the second PUT to the same
            // identifier with HTTP 400. The authoritative timestamp is envelope.timestamp (signed);
            // the doc-level date is a stable placeholder, matching RunningConfigurationPublisher.
            put("date", DOC_DATE)
            put("utcOffset", 0)
            put("app", "AAPS")
            put("schemaVersion", SCHEMA_VERSION)
            put("envelope", JSONObject(envelopeJson))
        }
        return runCatching { client.updateSettings(identifier, doc) }
            .fold(
                onSuccess = { resp ->
                    if (resp.response in 200..299) {
                        nsClientRepository.addLog("► CLIENTCTL", "$identifier counter=${envelope.counter}")
                        ClientControlSendResult.Success
                    } else {
                        aapsLogger.error(LTag.NSCLIENT, "ClientControl publish HTTP ${resp.response}: ${resp.errorResponse}")
                        nsClientRepository.addLog("✕ CLIENTCTL", "$identifier HTTP ${resp.response}")
                        ClientControlSendResult.PublishFailed("HTTP ${resp.response}")
                    }
                },
                onFailure = { t ->
                    aapsLogger.error(LTag.NSCLIENT, "ClientControl publish exception: ${t.message}", t)
                    ClientControlSendResult.PublishFailed(t.message)
                }
            )
    }
}
