package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Master-side poll for inbound client-control envelopes.
 *
 * For each authorized client (Pending or Active), fetches the matching deterministic
 * settings doc (`aaps_clientcontrol_hello_<clientId>` for now). On a verified envelope:
 * 1. promotes Pending → Active (hello) or just bumps lastSeen+counter (later commands)
 * 2. DELETEs the doc — the master has acknowledged it and the client must mint a fresh
 *    counter for any subsequent message.
 *
 * Replay protection rests on three checks before any state mutation:
 * - HMAC signature verifies against the stored secret for that clientId
 * - timestamp within ±5 min of master clock
 * - counter strictly greater than `counterReceived` for that client
 *
 * Failures (bad sig, stale timestamp, regressed counter, malformed JSON) are logged
 * and the doc is NOT deleted, so the master will retry on the next tick — except for
 * malformed-JSON which is hopeless and cleaned up to keep NS tidy.
 */
@Singleton
class ClientControlReceiver @Inject constructor(
    private val authorizedRepository: AuthorizedClientsRepository,
    private val nsClientV3Plugin: Provider<NSClientV3Plugin>,
    private val nsClientRepository: NSClientRepository,
    private val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Probe NS for any pending hello docs and process them. Safe to call repeatedly;
     * processed docs are deleted server-side.
     */
    suspend fun processPendingHellos() {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: return
        val now = dateUtil.now()
        // Pending entries are the ones expecting a hello. Active entries' hello docs
        // would already have been deleted, but we still check to clean up any stragglers.
        val candidates = authorizedRepository.current(now)
        for (entry in candidates) {
            val identifier = "${ClientControlPublisher.IDENTIFIER_HELLO_PREFIX}${entry.clientId}"
            val readResp = runCatching { client.getSettings(identifier) }.getOrNull() ?: continue
            val doc = readResp.values ?: continue
            val envelopeObj = doc.optJSONObject("envelope") ?: run {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier has no envelope field, deleting")
                runCatching { client.deleteSettings(identifier) }
                continue
            }
            val envelope = runCatching { json.decodeFromString<SignedEnvelope>(envelopeObj.toString()) }.getOrNull()
            if (envelope == null) {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier malformed envelope, deleting")
                runCatching { client.deleteSettings(identifier) }
                continue
            }
            if (envelope.clientId != entry.clientId) {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier clientId mismatch (got ${envelope.clientId}), deleting")
                runCatching { client.deleteSettings(identifier) }
                continue
            }
            if (envelope.type != "hello") {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier unexpected type=${envelope.type}, deleting")
                runCatching { client.deleteSettings(identifier) }
                continue
            }

            val lookup = authorizedRepository.secretLookup(entry.clientId) ?: continue
            // Order: signature first (proves authenticity), then freshness checks (counter + skew).
            // Sig-first means a failure log unambiguously means "forgery / wrong secret" rather than
            // being shadowed by a benign replay log on a forged-but-stale message.
            if (!ClientControlCrypto.verifyEnvelope(lookup.secretBytes, envelope)) {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier signature invalid")
                continue
            }
            if (envelope.counter <= lookup.counterReceived) {
                aapsLogger.error(
                    LTag.NSCLIENT,
                    "ClientControl: $identifier replay (counter=${envelope.counter} <= seen=${lookup.counterReceived}); leaving doc for diagnostics"
                )
                continue
            }
            if (!ClientControlCrypto.timestampWithinSkew(envelope.timestamp, now)) {
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier timestamp outside skew window")
                continue
            }

            // Verified hello — promote and ack
            when (entry.state) {
                ClientState.Pending -> authorizedRepository.markActive(entry.clientId, envelope.counter, now)
                ClientState.Active  -> authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
            }
            nsClientRepository.addLog("◄ CLIENTCTL", "hello accepted for ${entry.name} (${entry.clientId})")
            runCatching { client.deleteSettings(identifier) }
                .onFailure { aapsLogger.error(LTag.NSCLIENT, "ClientControl: failed to delete $identifier after ack: ${it.message}") }
        }
    }
}
