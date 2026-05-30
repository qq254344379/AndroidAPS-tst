package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.data.model.SceneEndAction
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.scenes.ActiveSceneSync
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.localmodel.clientcontrol.AuthorizedClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Master-side receiver for inbound client-control envelopes.
 *
 * Two entry points feed the same verify-and-ack pipeline:
 * - [onSettingsDocChanged] — primary, low-latency. Called from the WS create/update
 *   listener with the doc already in hand.
 * - [processPending] — polling fallback. Probes the deterministic hello + cmd
 *   identifiers per authorized client to catch anything WS missed during a
 *   disconnect window.
 *
 * Replay protection sits between parse and dispatch, in this order:
 * 1. HMAC signature against the stored secret for the envelope's clientId
 *    (only check that proves authenticity)
 * 2. counter strictly greater than `counterReceived` for that client (monotonic)
 * 3. timestamp within ±5 min of master clock (skew window)
 *
 * Sig-first means a failure log unambiguously means "forgery / wrong secret"
 * rather than being shadowed by a benign replay log on a forged-but-stale message.
 *
 * Behaviour on failure:
 * - Hopeless garbage (no envelope field, malformed JSON, unknown clientId) → DELETE
 *   so NS settings doesn't accumulate junk.
 * - Sig / counter / skew failures → leave the doc in place. The next tick (poll or
 *   WS update) will re-verify. Counter regression in particular often just means
 *   "we already processed this and the delete request didn't reach NS" — leaving
 *   the doc is harmless because the counter check still gates state mutation.
 *
 * On verified envelope: dispatch by [SignedEnvelope.type], then DELETE the doc.
 * Unknown types still advance the counter + delete (the secret-holder sent this
 * intentionally, an older master version just doesn't know what to do with it).
 */
@Singleton
class ClientControlReceiver @Inject constructor(
    private val authorizedRepository: AuthorizedClientsRepository,
    private val nsClientV3Plugin: Provider<NSClientV3Plugin>,
    private val nsClientRepository: NSClientRepository,
    private val sceneAutomationApi: SceneAutomationApi,
    private val activeSceneSync: ActiveSceneSync,
    private val offerPublisher: PairingOfferPublisher,
    private val preferences: Preferences,
    private val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Polling fallback. Lists NS settings, filters to client-control identifiers, and
     * dispatches each through [verifyAndAck]. Safe to call repeatedly — processed docs are
     * deleted server-side, and counter checks reject anything we already accepted.
     *
     * Using `searchSettings` instead of probing per-identifier means we don't need to know
     * in advance which clients exist or which command types they're using. New variants
     * land here with no polling-loop changes.
     */
    suspend fun processPending() {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: return
        val now = dateUtil.now()
        val resp = runCatching { client.searchSettings(limit = 100) }.getOrNull() ?: return
        for (doc in resp.values) {
            val identifier = doc.optString("identifier")
            if (!identifier.startsWith(ClientControlPublisher.IDENTIFIER_PREFIX)) continue
            // Skip our own pairing offers — they share the prefix but are not signed envelopes,
            // so verifyAndAck would log them as "malformed envelope, deleting" and wipe a still-
            // valid offer out from under the client mid-pairing.
            if (identifier.startsWith(ClientControlPublisher.IDENTIFIER_OFFER_PREFIX)) continue
            verifyAndAck(identifier, doc, now)
        }
    }

    /**
     * WS-push entry. NSClientV3Service routes settings-collection create/update events
     * to here for any identifier with the [ClientControlPublisher.IDENTIFIER_PREFIX].
     */
    suspend fun onSettingsDocChanged(identifier: String, doc: JSONObject) {
        if (!identifier.startsWith(ClientControlPublisher.IDENTIFIER_PREFIX)) return
        if (identifier.startsWith(ClientControlPublisher.IDENTIFIER_OFFER_PREFIX)) return
        verifyAndAck(identifier, doc, dateUtil.now())
    }

    private suspend fun verifyAndAck(identifier: String, doc: JSONObject, now: Long) {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: return
        val envelopeObj = doc.optJSONObject("envelope") ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier has no envelope field, deleting")
            runCatching { client.deleteSettings(identifier) }
            return
        }
        val envelope = runCatching { json.decodeFromString<SignedEnvelope>(envelopeObj.toString()) }.getOrNull()
        if (envelope == null) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier malformed envelope, deleting")
            runCatching { client.deleteSettings(identifier) }
            return
        }
        // Resolve the authorized client by the envelope's clientId — never trust the
        // identifier suffix, since any paired client could write to any identifier.
        // findRaw is captured *before* current(now) so the pre-prune state is available for
        // the diagnostic branch below — current(now) silently drops expired pendings, which
        // would otherwise make a scraped-expired-QR replay indistinguishable from a typo.
        val raw = authorizedRepository.findRaw(envelope.clientId)
        val entry = authorizedRepository.current(now).firstOrNull { it.clientId == envelope.clientId }
        if (entry == null) {
            if (raw != null && raw.state == ClientState.Pending && raw.pairExpiresAt <= now)
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier pairing window expired for clientId=${envelope.clientId}, deleting")
            else
                aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier unknown clientId=${envelope.clientId}, deleting")
            runCatching { client.deleteSettings(identifier) }
            return
        }
        val lookup = authorizedRepository.secretLookup(entry.clientId) ?: return
        if (!ClientControlCrypto.verifyEnvelope(lookup.secretBytes, envelope)) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier signature invalid")
            return
        }
        if (envelope.counter <= lookup.counterReceived) {
            aapsLogger.error(
                LTag.NSCLIENT,
                "ClientControl: $identifier replay (counter=${envelope.counter} <= seen=${lookup.counterReceived}); leaving doc for diagnostics"
            )
            return
        }
        if (!ClientControlCrypto.timestampWithinSkew(envelope.timestamp, now)) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: $identifier timestamp outside skew window")
            return
        }
        // Decode the payload into the sealed message family. A failure here means the verified
        // sender wrote a payload this master cannot interpret — older master vs newer client,
        // typically. Treat it the same as an unknown type: advance the counter so the next
        // envelope can be processed, and delete the doc to avoid replay storms.
        val message = runCatching { json.decodeFromString(ClientControlMessage.serializer(), envelope.payload) }.getOrNull()
        if (message == null) {
            onVerifiedUndecodablePayload(entry, envelope, now)
        } else {
            when (message) {
                is ClientControlMessage.Hello                  -> onVerifiedHello(entry, envelope, now)
                // Hello handling already includes the Pending → Active transition; offer doc
                // cleanup is inside onVerifiedHello so it can suspend on deleteOffer.
                is ClientControlMessage.SceneStart             -> onVerifiedSceneStart(entry, envelope, message, now)
                is ClientControlMessage.SceneStop              -> onVerifiedSceneStop(entry, envelope, message, now)
                is ClientControlMessage.SceneDefinitionsUpdate -> onVerifiedScenesUpdate(entry, envelope, message, now)
            }
        }
        // Don't deleteSettings here. NS soft-deletes (tombstones) the identifier; the next
        // legitimate same-type command from the same client would PUT to that identifier and
        // hit HTTP 410. Counter dedup (line above) prevents replay; the doc just lingers in
        // the slot until the next overwrite or NS auto-prune. Error-path deletes above stay —
        // those purge unverifiable garbage rather than acknowledged commands.
    }

    private suspend fun onVerifiedHello(entry: AuthorizedClient, envelope: SignedEnvelope, now: Long) {
        when (entry.state) {
            ClientState.Pending -> {
                authorizedRepository.markActive(entry.clientId, envelope.counter, now)
                // Pair-complete: drop the wrapped offer doc so the brute-force window closes the moment
                // the secret is no longer needed for fetching. Without this, the offer would linger up
                // to pairExpiresAt giving an attacker the full window even though pairing already
                // succeeded — directly contradicting the security model in ClientControlPairingCrypto.
                offerPublisher.deleteOffer(entry.clientId)
            }

            ClientState.Active  -> authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        }
        nsClientRepository.addLog("◄ CLIENTCTL", "hello accepted for ${entry.name} (${entry.clientId})")
    }

    private suspend fun onVerifiedSceneStart(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.SceneStart, now: Long) {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        val result = sceneAutomationApi.runScene(message.sceneId, message.durationMinutes)
        nsClientRepository.addLog("◄ CLIENTCTL", "scene.start sceneId=${message.sceneId} from ${entry.name}: ${result.tag()}")
        if (result !is SceneAutomationResult.Success)
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: scene.start failed for ${entry.name} sceneId=${message.sceneId}: ${result.tag()}")
    }

    private suspend fun onVerifiedSceneStop(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.SceneStop, now: Long) {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        // triggerChain=true means "Skip to <ChainTarget>". Read the active scene's currently
        // configured chain target FRESH at receipt time (not from the wire) so a stale client
        // view can't trigger an unintended scene.
        val chainTargetId = if (message.triggerChain) {
            val activeId = activeSceneSync.activeSceneSnapshot()?.sceneId
            val activeScene = activeId?.let { sceneAutomationApi.getScene(it) }
            (activeScene?.endAction as? SceneEndAction.ChainScene)?.sceneId
        } else null
        val result = if (chainTargetId != null) sceneAutomationApi.stopActiveSceneAndStartScene(chainTargetId)
        else sceneAutomationApi.stopActiveScene()
        // Outcome distinguishes the three branches so an operator reading the NS log can tell
        // chain-fired apart from chain-requested-but-no-target apart from plain-stop.
        val outcome = when {
            chainTargetId != null -> "chain→$chainTargetId"
            message.triggerChain  -> "no-chain-target"
            else                  -> "plain-stop"
        }
        nsClientRepository.addLog("◄ CLIENTCTL", "scene.stop $outcome from ${entry.name}: ${result.tag()}")
        // ChainCompleted with failedCount==0 is a fully successful chain — not a failure to warn about.
        val isFailure = when (result) {
            SceneAutomationResult.Success           -> false
            is SceneAutomationResult.ChainCompleted -> result.failedCount > 0
            else                                    -> true
        }
        if (isFailure)
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: scene.stop failed for ${entry.name}: ${result.tag()}")
    }

    /**
     * Apply a client-pushed scenes JSON to the master's `SceneDefinitions` pref via per-scene
     * last-writer-wins on `lastModified`. JSON-level merge — keeps this module free of a
     * `:ui` dependency on the Scene model. Writing the merged JSON back to the pref triggers
     * `RunningConfigurationPublisher`'s debounce, which fans the result out to all paired
     * clients via the running-config doc.
     *
     * Tombstones (`isValid = false`) are merged as-is; the editor's load-time purge handles
     * physical removal lazily — consistent with how local deletes on master behave.
     */
    private fun onVerifiedScenesUpdate(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.SceneDefinitionsUpdate, now: Long) {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        val incoming = runCatching { JSONArray(message.scenesJson) }.getOrNull()
        if (incoming == null) {
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: scenes_update from ${entry.name} has invalid JSON, ignoring")
            nsClientRepository.addLog("◄ CLIENTCTL", "scenes.update from ${entry.name}: invalid JSON")
            return
        }
        val existing = runCatching { JSONArray(preferences.get(StringNonKey.SceneDefinitions)) }.getOrNull() ?: JSONArray()
        // Build id → (index, lastModified) over the existing array so we can apply LWW
        // per scene without rebuilding the array structure (preserves any unknown fields).
        val existingIndex = HashMap<String, Int>()
        for (i in 0 until existing.length()) {
            existing.optJSONObject(i)?.optString("id")?.takeIf { it.isNotEmpty() }?.let { existingIndex[it] = i }
        }
        var changed = 0
        var dropped = 0
        for (i in 0 until incoming.length()) {
            val inc = incoming.optJSONObject(i) ?: continue
            val id = inc.optString("id").takeIf { it.isNotEmpty() } ?: continue
            val incLm = inc.optLong("lastModified", 0L)
            val existIdx = existingIndex[id]
            if (existIdx == null) {
                existing.put(inc)
                existingIndex[id] = existing.length() - 1
                changed++
            } else {
                val existLm = existing.optJSONObject(existIdx)?.optLong("lastModified", 0L) ?: 0L
                if (incLm > existLm) {
                    existing.put(existIdx, inc)
                    changed++
                } else {
                    dropped++
                }
            }
        }
        if (changed > 0) preferences.put(StringNonKey.SceneDefinitions, existing.toString())
        nsClientRepository.addLog("◄ CLIENTCTL", "scenes.update from ${entry.name}: applied=$changed stale=$dropped")
    }

    private fun SceneAutomationResult.tag(): String = when (this) {
        SceneAutomationResult.Success           -> "ok"
        SceneAutomationResult.SceneNotFound     -> "scene-not-found"
        SceneAutomationResult.SceneDisabled     -> "scene-disabled"
        is SceneAutomationResult.Failed         -> "failed:${message ?: "unknown"}"
        is SceneAutomationResult.ChainCompleted ->
            if (failedCount == 0) "chained:${endedSceneName ?: "?"}→$targetSceneName"
            else "chain-partial:${endedSceneName ?: "?"}→$targetSceneName($failedCount/$totalCount)"
    }

    private fun onVerifiedUndecodablePayload(entry: AuthorizedClient, envelope: SignedEnvelope, now: Long) {
        // Advance counter + delete: the secret-holder sent this intentionally, this master just
        // doesn't have a ClientControlMessage variant for type=${envelope.type}. Not advancing
        // would let the same doc replay every WS update.
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        nsClientRepository.addLog("◄ CLIENTCTL", "type=${envelope.type} undecodable for ${entry.name}")
        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: ${entry.clientId} verified envelope type=${envelope.type} has no decoder on this master, advancing counter and acking")
    }
}
