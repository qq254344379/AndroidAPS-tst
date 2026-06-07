package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.data.model.ICfg
import app.aaps.core.data.model.SceneEndAction
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.scenes.ActiveSceneSync
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.LongComposedKey
import app.aaps.core.keys.interfaces.BooleanNonPreferenceKey
import app.aaps.core.keys.interfaces.DoubleNonPreferenceKey
import app.aaps.core.keys.interfaces.IntNonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.StringNonPreferenceKey
import app.aaps.core.keys.interfaces.SyncDirection
import app.aaps.core.keys.interfaces.UnitDoublePreferenceKey
import app.aaps.core.nssdk.interfaces.NSAndroidClient
import app.aaps.core.nssdk.localmodel.clientcontrol.AckEnvelope
import app.aaps.core.nssdk.localmodel.clientcontrol.AckPhase
import app.aaps.core.nssdk.localmodel.clientcontrol.AckStatus
import app.aaps.core.nssdk.localmodel.clientcontrol.AuthorizedClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.core.objects.extensions.fromJsonObject
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import app.aaps.plugins.sync.nsclientV3.services.RunningConfigurationPublisher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    private val profileFunction: ProfileFunction,
    private val offerPublisher: PairingOfferPublisher,
    private val preferences: Preferences,
    private val dateUtil: DateUtil,
    private val uel: UserEntryLogger,
    private val runningConfigurationPublisher: RunningConfigurationPublisher,
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
            // Skip our own command ACKs (master-written, for clients). Same reason as offers.
            if (identifier.startsWith(ClientControlPublisher.IDENTIFIER_ACK_PREFIX)) continue
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
        // Command ACKs are master-written, consumed by clients — never an inbound command here.
        if (identifier.startsWith(ClientControlPublisher.IDENTIFIER_ACK_PREFIX)) return
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
        // Hard expiry: a command that sat past its validity must NOT fire late (e.g. a profile switch
        // requested 15 min ago and only now picked up). Consume the counter so it can't replay, and
        // tell the client definitively rather than leaving it to time out.
        if (now > envelope.validUntil) {
            authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
            nsClientRepository.addLog("◄ CLIENTCTL", "expired ${envelope.type} from ${entry.name}")
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: $identifier expired (validUntil=${envelope.validUntil} < now=$now), not executing")
            if (envelope.wantsAck) writeAck(client, lookup.secretBytes, envelope.clientId, envelope.counter, AckPhase.Done, AckStatus.Expired, "expired before master applied it", now)
            return
        }
        // Decode the payload into the sealed message family. A failure here means the verified
        // sender wrote a payload this master cannot interpret — older master vs newer client,
        // typically. Treat it the same as an unknown type: advance the counter so the next
        // envelope can be processed.
        val message = runCatching { json.decodeFromString(ClientControlMessage.serializer(), envelope.payload) }.getOrNull()
        // Hello is the pairing handshake, not a user action — no ACK round-trip. Its handling
        // includes the Pending → Active transition + offer-doc cleanup (suspends on deleteOffer).
        if (message is ClientControlMessage.Hello) {
            onVerifiedHello(entry, envelope, now)
            return
        }
        // Round-trip commands (wantsAck) get the two-step ACK: Executing before dispatch, then the
        // result. Fire-and-forget commands (scenes, pref sync) skip it — no client is waiting, so the
        // ACK doc would be pure write amplification.
        if (envelope.wantsAck) writeAck(client, lookup.secretBytes, envelope.clientId, envelope.counter, AckPhase.Executing, AckStatus.Pending, null, now)
        val outcome = when (message) {
            null                                      -> {
                onVerifiedUndecodablePayload(entry, envelope, now)
                AckOutcome(AckStatus.Failed, "unsupported command type ${envelope.type}")
            }

            is ClientControlMessage.Hello             -> AckOutcome(AckStatus.Ok, null) // unreachable (handled above)
            ClientControlMessage.Ping                 -> onVerifiedPing(entry, envelope, now)
            is ClientControlMessage.SceneStart        -> onVerifiedSceneStart(entry, envelope, message, now)
            is ClientControlMessage.SceneStop         -> onVerifiedSceneStop(entry, envelope, message, now)
            is ClientControlMessage.InsulinActivate   -> onVerifiedInsulinActivate(entry, envelope, message, now)
            is ClientControlMessage.PreferencesUpdate -> onVerifiedPreferencesUpdate(entry, envelope, message, now)
        }
        if (envelope.wantsAck) writeAck(client, lookup.secretBytes, envelope.clientId, envelope.counter, AckPhase.Done, outcome.status, outcome.reason, now)
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

    /** Liveness probe: nothing to do but advance the counter and ack Ok — the ack is the pong. */
    private fun onVerifiedPing(entry: AuthorizedClient, envelope: SignedEnvelope, now: Long): AckOutcome {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        nsClientRepository.addLog("◄ CLIENTCTL", "ping from ${entry.name}")
        return AckOutcome(AckStatus.Ok, null)
    }

    private suspend fun onVerifiedSceneStart(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.SceneStart, now: Long): AckOutcome {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        val result = sceneAutomationApi.runScene(message.sceneId, message.durationMinutes)
        nsClientRepository.addLog("◄ CLIENTCTL", "scene.start sceneId=${message.sceneId} from ${entry.name}: ${result.tag()}")
        if (result !is SceneAutomationResult.Success)
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: scene.start failed for ${entry.name} sceneId=${message.sceneId}: ${result.tag()}")
        return if (result is SceneAutomationResult.Success) AckOutcome(AckStatus.Ok, null) else AckOutcome(AckStatus.Failed, result.tag())
    }

    private suspend fun onVerifiedSceneStop(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.SceneStop, now: Long): AckOutcome {
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
        return if (isFailure) AckOutcome(AckStatus.Failed, result.tag()) else AckOutcome(AckStatus.Ok, null)
    }

    /**
     * Activate an insulin requested by a client: create a profile switch over the master's CURRENT
     * profile with the pushed insulin config. The master is authoritative for the profile (the
     * client's view may be stale), so only the iCfg travels; the resulting EPS syncs back normally.
     * No-op (logged) if the master has no active profile switch to re-apply.
     */
    private suspend fun onVerifiedInsulinActivate(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.InsulinActivate, now: Long): AckOutcome {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        val iCfg = runCatching {
            (Json.parseToJsonElement(message.iCfgJson) as? JsonObject)?.let { ICfg.fromJsonObject(it) }
        }.getOrNull()
        if (iCfg == null) {
            aapsLogger.warn(LTag.NSCLIENT, "ClientControl: insulin.activate from ${entry.name} has invalid iCfg JSON, ignoring")
            nsClientRepository.addLog("◄ CLIENTCTL", "insulin.activate from ${entry.name}: invalid JSON")
            return AckOutcome(AckStatus.Failed, "invalid insulin config")
        }
        val applied = profileFunction.createProfileSwitchWithNewInsulin(iCfg, Sources.Insulin)
        nsClientRepository.addLog(
            "◄ CLIENTCTL",
            "insulin.activate from ${entry.name}: " + if (applied) "applied ${iCfg.insulinLabel}" else "no active profile"
        )
        return if (applied) AckOutcome(AckStatus.Ok, null) else AckOutcome(AckStatus.Failed, "no active profile to re-apply")
    }

    /**
     * Generic apply of client-pushed bidirectionally-synced preference values. Per key: resolve it
     * from the registry, reject anything not declared `Bidirectional` (authority), drop stale pushes
     * by per-key last-writer-wins, and write the winners via [Preferences.putRemote] — which stamps
     * the modified time to the pushed version and suppresses re-publish (no echo). The running-config
     * publisher then republishes the cold doc, fanning the merged value out to all clients.
     *
     * Value parsing handles Boolean/String/Int/UnitDouble (see the `when` below); extend it per type
     * as new types are marked `Bidirectional`.
     */
    private fun onVerifiedPreferencesUpdate(entry: AuthorizedClient, envelope: SignedEnvelope, message: ClientControlMessage.PreferencesUpdate, now: Long): AckOutcome {
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        val applied = mutableListOf<String>()
        message.prefs.forEach { (keyString, pushed) ->
            val key = preferences.get(keyString)
            if (key == null || key.sync?.direction != SyncDirection.Bidirectional) {
                aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} rejected $keyString (unknown or not client-writable)")
                return@forEach
            }
            if (pushed.lastModified <= preferences.get(LongComposedKey.SyncedPrefModified, keyString)) return@forEach // stale, LWW
            when (key) {
                is BooleanNonPreferenceKey -> {
                    val value = pushed.value.toBooleanStrictOrNull()
                    if (value == null) {
                        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} bad boolean for $keyString: '${pushed.value}'")
                        return@forEach
                    }
                    preferences.putRemote(key, value, pushed.lastModified)
                    applied += "$keyString=$value"
                }

                is StringNonPreferenceKey  -> {
                    preferences.putRemote(key, pushed.value, pushed.lastModified)
                    applied += "$keyString=${pushed.value}"
                }

                is IntNonPreferenceKey     -> {
                    val value = pushed.value.toIntOrNull()
                    if (value == null) {
                        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} bad int for $keyString: '${pushed.value}'")
                        return@forEach
                    }
                    preferences.putRemote(key, value, pushed.lastModified)
                    applied += "$keyString=$value"
                }

                is DoubleNonPreferenceKey  -> {
                    val value = pushed.value.toDoubleOrNull()
                    if (value == null) {
                        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} bad double for $keyString: '${pushed.value}'")
                        return@forEach
                    }
                    preferences.putRemote(key, value, pushed.lastModified)
                    applied += "$keyString=$value"
                }

                is UnitDoublePreferenceKey -> {
                    val value = pushed.value.toDoubleOrNull()
                    if (value == null) {
                        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} bad double for $keyString: '${pushed.value}'")
                        return@forEach
                    }
                    preferences.putRemote(key, value, pushed.lastModified)   // raw mg/dl
                    applied += "$keyString=$value"
                }

                else                       ->
                    aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update from ${entry.name} unsupported key type for $keyString")
            }
        }
        nsClientRepository.addLog("◄ CLIENTCTL", "preferences.update from ${entry.name}: " + if (applied.isEmpty()) "nothing applied" else "applied ${applied.joinToString()}")
        if (applied.isNotEmpty()) uel.log(Action.REMOTE_CONFIG_CHANGED, Sources.NSClient, note = "${entry.name}: ${applied.joinToString()}")
        // Always republish the cold doc so the client converges to the master's authoritative values —
        // including keys LWW-dropped here as a no-op (those don't change a value, so they wouldn't
        // trigger the observeChange-driven republish on their own). Without this a client that loses LWW
        // on a concurrent edit would stay stuck on its optimistic value.
        runningConfigurationPublisher.requestColdRepublish()
        // Best-effort merge: per-key rejects (unknown/stale/bad-type) are logged but the batch as a
        // whole is acknowledged Ok.
        return AckOutcome(AckStatus.Ok, null)
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
        // Advance counter: the secret-holder sent this intentionally, this master just doesn't have a
        // ClientControlMessage variant for type=${envelope.type}. Not advancing would let the same doc
        // replay every WS update.
        authorizedRepository.bumpLastSeen(entry.clientId, envelope.counter, now)
        nsClientRepository.addLog("◄ CLIENTCTL", "type=${envelope.type} undecodable for ${entry.name}")
        aapsLogger.warn(LTag.NSCLIENT, "ClientControl: ${entry.clientId} verified envelope type=${envelope.type} has no decoder on this master, advancing counter and acking")
    }

    /**
     * Write a signed [AckEnvelope] to the per-client ACK identifier (overwritten in place). Signed
     * with the same shared secret as the inbound command so the client can prove it came from us.
     * Exception-safe: a failed ACK write must never abort command processing (the command already
     * applied) — it just degrades the client to its timeout/sync-back fallback.
     */
    private suspend fun writeAck(
        client: NSAndroidClient,
        secret: ByteArray,
        clientId: String,
        commandCounter: Long,
        phase: AckPhase,
        status: AckStatus,
        reason: String?,
        now: Long
    ) {
        val ack = ClientControlCrypto.signAck(
            secret,
            AckEnvelope(clientId = clientId, commandCounter = commandCounter, phase = phase, status = status, reason = reason, timestamp = now, signature = "")
        )
        val identifier = ClientControlPublisher.IDENTIFIER_ACK_PREFIX + clientId
        val doc = JSONObject().apply {
            put("date", ClientControlPublisher.DOC_DATE)
            put("utcOffset", 0)
            put("app", "AAPS")
            put("schemaVersion", ClientControlPublisher.SCHEMA_VERSION)
            put("ack", JSONObject(json.encodeToString(AckEnvelope.serializer(), ack)))
        }
        runCatching { client.updateSettings(identifier, doc) }
            .onSuccess { nsClientRepository.addLog("► CLIENTCTL", "ack $phase/$status counter=$commandCounter" + (reason?.let { " ($it)" } ?: "")) }
            .onFailure { aapsLogger.error(LTag.NSCLIENT, "ClientControl: ack write failed for $identifier: ${it.message}") }
    }

    /** Result a command handler reports back so [verifyAndAck] can write the terminal Done ack. */
    private data class AckOutcome(val status: AckStatus, val reason: String?)
}
