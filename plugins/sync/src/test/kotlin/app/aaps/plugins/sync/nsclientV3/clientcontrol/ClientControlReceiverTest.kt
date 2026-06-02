package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.data.model.ICfg
import app.aaps.core.data.model.Scene
import app.aaps.core.data.model.SceneEndAction
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.insulin.Insulin
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.protection.SecureEncrypt
import app.aaps.core.interfaces.scenes.ActiveSceneSnapshot
import app.aaps.core.interfaces.scenes.ActiveSceneSync
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.IntKey
import app.aaps.core.keys.LongComposedKey
import app.aaps.core.keys.LongNonKey
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.BooleanNonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.interfaces.NSAndroidClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.localmodel.clientcontrol.PrefEntry
import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import app.aaps.core.nssdk.localmodel.treatment.CreateUpdateResponse
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class ClientControlReceiverTest {

    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var aapsLogger: AAPSLogger
    @Mock private lateinit var nsClientRepository: NSClientRepository
    @Mock private lateinit var nsClientV3Plugin: NSClientV3Plugin
    @Mock private lateinit var nsAndroidClient: NSAndroidClient
    @Mock private lateinit var sceneAutomationApi: SceneAutomationApi
    @Mock private lateinit var activeSceneSync: ActiveSceneSync
    @Mock private lateinit var automation: Automation
    @Mock private lateinit var insulin: Insulin
    @Mock private lateinit var profileFunction: ProfileFunction
    @Mock private lateinit var offerPublisher: PairingOfferPublisher
    @Mock private lateinit var dateUtil: DateUtil
    @Mock private lateinit var uel: UserEntryLogger

    // Same deterministic fake as the repository tests — encrypt/decrypt round-trip via reverse,
    // so the persisted form does not contain the original plaintext.
    private val secureEncrypt = object : SecureEncrypt {
        override fun encrypt(plaintextSecret: String, keystoreAlias: String): String = "ENC:$keystoreAlias:${plaintextSecret.reversed()}"
        override fun decrypt(encryptedSecret: String): String = encryptedSecret.removePrefix("ENC:NsClientControlSecret:").reversed()
        override fun isValidDataString(data: String?): Boolean = data != null && data.startsWith("ENC:")
        override fun deleteKey(keystoreAlias: String) {}
    }

    private lateinit var authorizedRepository: AuthorizedClientsRepository
    private lateinit var sut: ClientControlReceiver

    private var stored = "[]"
    private var automationVersion = 0L
    private var insulinVersion = 0L

    // Per-key backing store — the scenes-update merge writes a different StringNonKey
    // (`SceneDefinitions`) than the authorized-clients repository, so a shared `stored`
    // would clobber whichever was written last. Keyed by [StringNonKey.key].
    private val storage = mutableMapOf<String, String>()
    private val now = 1_700_000_000_000L

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        stored = "[]"
        storage.clear()
        storage[StringNonKey.SceneDefinitions.key] = "[]"
        whenever(preferences.get(StringNonKey.NsClientControlAuthorizedClients)).thenAnswer { stored }
        whenever(preferences.get(StringNonKey.SceneDefinitions)).thenAnswer {
            storage[StringNonKey.SceneDefinitions.key] ?: "[]"
        }
        whenever(preferences.put(any<StringNonKey>(), any<String>())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as StringNonKey
            val value = invocation.arguments[1] as String
            when (key) {
                StringNonKey.SceneDefinitions,
                StringNonKey.AutomationEvents,
                StringNonKey.InsulinConfiguration -> storage[key.key] = value

                else                              -> stored = value
            }
        }
        // Automation whole-list version (LongNonKey). Receiver reads it for the LWW compare and
        // writes it on apply.
        whenever(preferences.get(LongNonKey.AutomationEventsModified)).thenAnswer { automationVersion }
        whenever(preferences.get(LongNonKey.InsulinConfigurationModified)).thenAnswer { insulinVersion }
        whenever(preferences.put(any<LongNonKey>(), any<Long>())).thenAnswer { invocation ->
            when (invocation.arguments[0]) {
                LongNonKey.AutomationEventsModified     -> automationVersion = invocation.arguments[1] as Long
                LongNonKey.InsulinConfigurationModified -> insulinVersion = invocation.arguments[1] as Long

                else                                    -> {}
            }
        }
        authorizedRepository = AuthorizedClientsRepository(preferences, secureEncrypt, aapsLogger)
        whenever(nsClientV3Plugin.nsAndroidClient).thenReturn(nsAndroidClient)
        whenever(dateUtil.now()).thenReturn(now)
        sut = ClientControlReceiver(
            authorizedRepository,
            Provider { nsClientV3Plugin },
            nsClientRepository,
            sceneAutomationApi,
            activeSceneSync,
            automation,
            insulin,
            profileFunction,
            offerPublisher,
            preferences,
            dateUtil,
            uel,
            aapsLogger
        )
    }

    /** Adds a pending client and returns (clientId, secretBytes). */
    private fun pair(name: String = "phone"): Pair<String, ByteArray> {
        val (entry, secretHex) = authorizedRepository.addPending(name, pairTtlMs = 60_000L, now = now - 10_000L)
        val secretBytes = secretHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return entry.clientId to secretBytes
    }

    /**
     * Adds a pending client whose pairing window already expired relative to `now`.
     * Created 2h ago with a 5-min TTL, so pairExpiresAt is well in the past.
     */
    private fun pairExpired(name: String = "phone"): Pair<String, ByteArray> {
        val (entry, secretHex) = authorizedRepository.addPending(name, pairTtlMs = 5 * 60_000L, now = now - 2 * 60 * 60 * 1000L)
        val secretBytes = secretHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return entry.clientId to secretBytes
    }

    // Mirror the publisher's Json instance so test envelopes contain the same payload bytes
    // production would emit — particularly: `encodeDefaults = true` writes `protocolVersion: 1`
    // explicitly. Without this, the test could pass on shorter bytes than production sends.
    private val publisherJson = Json { encodeDefaults = true }

    private fun envelope(
        clientId: String,
        secret: ByteArray,
        message: ClientControlMessage = ClientControlMessage.Hello(),
        counter: Long = 1L,
        timestamp: Long = now
    ): SignedEnvelope {
        val payload = publisherJson.encodeToString(ClientControlMessage.serializer(), message)
        val type = publisherJson.parseToJsonElement(payload).jsonObject["type"]!!.jsonPrimitive.content
        return signedEnvelope(
            clientId = clientId,
            secret = secret,
            type = type,
            payload = payload,
            counter = counter,
            timestamp = timestamp
        )
    }

    /** Lower-level helper for tests that need to forge a non-decodable payload while keeping a valid signature. */
    private fun signedEnvelope(clientId: String, secret: ByteArray, type: String, payload: String, counter: Long = 1L, timestamp: Long = now): SignedEnvelope =
        ClientControlCrypto.signEnvelope(
            secret,
            SignedEnvelope(clientId = clientId, counter = counter, timestamp = timestamp, type = type, payload = payload, signature = "")
        )

    private fun wrap(envelope: SignedEnvelope): JSONObject =
        JSONObject().apply {
            put("date", envelope.timestamp)
            put("utcOffset", 0)
            put("app", "AAPS")
            put("schemaVersion", 1)
            put("envelope", JSONObject(Json.encodeToString(SignedEnvelope.serializer(), envelope)))
        }

    private val deleteOk = CreateUpdateResponse(response = 200, identifier = null, isDeduplication = false, deduplicatedIdentifier = null, lastModified = null, errorResponse = null)

    @Test
    fun helloFromPendingClientPromotesToActive() = runTest {
        val (clientId, secret) = pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val doc = wrap(envelope(clientId, secret, counter = 1L))

        sut.onSettingsDocChanged(identifier, doc)

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Active)
        assertThat(entry.counterReceived).isEqualTo(1L)
        // Success path no longer deletes the hello doc — NS would tombstone the slot and reject
        // the next same-type command with HTTP 410. Counter dedup handles replay protection.
        verify(nsAndroidClient, never()).deleteSettings(identifier)
        // …but the offer doc IS dropped on the Pending → Active transition so the wrapped secret
        // does not linger on NS past pairing-complete.
        verify(offerPublisher).deleteOffer(clientId)
    }

    @Test
    fun helloFromAlreadyActiveClientDoesNotDeleteOfferAgain() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val doc = wrap(envelope(clientId, secret, counter = 2L))

        sut.onSettingsDocChanged(identifier, doc)

        // Active hellos only bump lastSeen — no redundant offer-delete.
        verify(offerPublisher, never()).deleteOffer(clientId)
    }

    @Test
    fun helloFromAlreadyActiveClientBumpsLastSeen() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val doc = wrap(envelope(clientId, secret, counter = 2L))

        sut.onSettingsDocChanged(identifier, doc)

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Active)
        assertThat(entry.counterReceived).isEqualTo(2L)
        assertThat(entry.lastSeenAt).isEqualTo(now)
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun verifiedEnvelopeWithUndecodablePayloadAdvancesCounter() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_CMD_PREFIX + clientId
        // Forge a verified envelope whose payload doesn't match any ClientControlMessage variant.
        // Simulates "newer client speaks a type this older master doesn't know yet".
        val signed = signedEnvelope(clientId, secret, type = "scene.future", payload = """{"type":"scene.future","sceneId":"x"}""", counter = 5L)

        sut.onSettingsDocChanged(identifier, wrap(signed))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.counterReceived).isEqualTo(5L)
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun badSignatureLeavesDocAndDoesNotMutateState() = runTest {
        val (clientId, secret) = pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val signed = envelope(clientId, secret)
        val tampered = signed.copy(signature = "0".repeat(64))

        sut.onSettingsDocChanged(identifier, wrap(tampered))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Pending)
        assertThat(entry.counterReceived).isEqualTo(0L)
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun signedByWrongSecretLeavesDoc() = runTest {
        val (clientId, _) = pair()
        val wrongSecret = ByteArray(32) { 0x42 }
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, wrongSecret)))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Pending)
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun counterRegressionLeavesDocForDiagnostics() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 5L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_CMD_PREFIX + clientId

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, counter = 5L)))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.counterReceived).isEqualTo(5L)
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun timestampOutsideSkewLeavesDoc() = runTest {
        val (clientId, secret) = pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val ancient = envelope(clientId, secret, timestamp = now - 60 * 60 * 1000L) // 1h ago

        sut.onSettingsDocChanged(identifier, wrap(ancient))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Pending)
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun unknownClientIdDeletesAsHopeless() = runTest {
        pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + "stranger-uuid"
        val signed = envelope("stranger-uuid", ByteArray(32) { 0x11 })
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, wrap(signed))

        verify(nsAndroidClient).deleteSettings(identifier)
    }

    /**
     * A hello arriving for a pending entry whose QR window already elapsed should
     * delete the doc and log distinctly from the "truly unknown clientId" path —
     * the latter is operational noise (typo / stale identifier), the former is the
     * scraped-expired-QR replay signature an operator wants to notice.
     * State must not flip to Active.
     */
    @Test
    fun helloForExpiredPendingLogsPairingWindowExpiredAndDeletes() = runTest {
        val (clientId, secret) = pairExpired()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret)))

        // Entry pruned by current(now); attacker's hello did not promote it.
        assertThat(authorizedRepository.current(now).none { it.clientId == clientId }).isTrue()
        verify(nsAndroidClient).deleteSettings(identifier)
        verify(aapsLogger).error(eq(LTag.NSCLIENT), argThat<String> { contains("pairing window expired") })
    }

    @Test
    fun missingEnvelopeFieldDeletesAsHopeless() = runTest {
        pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + "anything"
        val noEnvelope = JSONObject().apply {
            put("date", now)
            put("app", "AAPS")
        }
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, noEnvelope)

        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun malformedEnvelopeJsonDeletesAsHopeless() = runTest {
        pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + "anything"
        val malformed = JSONObject().apply { put("envelope", JSONObject().apply { put("garbage", true) }) }
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, malformed)

        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun nonClientControlIdentifiersAreIgnored() = runTest {
        val (clientId, secret) = pair()
        sut.onSettingsDocChanged("aaps_runningconfiguration", wrap(envelope(clientId, secret)))

        verify(nsAndroidClient, never()).deleteSettings(any())
        verify(nsAndroidClient, never()).getSettings(any())
    }

    @Test
    fun sceneStartDispatchesToAutomationApi() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_start_$clientId"
        val msg = ClientControlMessage.SceneStart(sceneId = "sleep", durationMinutes = 30)
        whenever(sceneAutomationApi.runScene("sleep", 30)).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = 5L)))

        verify(sceneAutomationApi).runScene("sleep", 30)
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithoutChainDispatchesToStopActiveScene() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(false), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithChainResolvesActiveSceneAndDispatchesAndChain() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        // Simulate master state: active scene "sleep", chained to "wakeup"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "sleep", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("sleep")).thenReturn(
            Scene(id = "sleep", name = "Sleep", endAction = SceneEndAction.ChainScene("wakeup"))
        )
        whenever(sceneAutomationApi.stopActiveSceneAndStartScene("wakeup")).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveSceneAndStartScene("wakeup")
        verify(sceneAutomationApi, never()).stopActiveScene()
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    /**
     * ChainCompleted with failedCount==0 is a fully successful chain — receiver must NOT log a warn,
     * even though the result type isn't [SceneAutomationResult.Success]. Locks in the isFailure
     * classification added when ChainCompleted was introduced.
     */
    @Test
    fun sceneStopWithChainCompletedSuccessfullyDoesNotWarn() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "sleep", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("sleep")).thenReturn(
            Scene(id = "sleep", name = "Sleep", endAction = SceneEndAction.ChainScene("wakeup"))
        )
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveSceneAndStartScene("wakeup")).thenReturn(
            SceneAutomationResult.ChainCompleted(endedSceneName = "Sleep", targetSceneName = "Wakeup", failedCount = 0, totalCount = 3)
        )

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(aapsLogger, never()).warn(eq(LTag.NSCLIENT), argThat<String> { contains("scene.stop failed") })
    }

    /**
     * ChainCompleted with failedCount>0 means the target activated but some actions failed.
     * Receiver must log a warn so operators reading the NS log see partial failures.
     */
    @Test
    fun sceneStopWithChainPartialFailureWarns() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "sleep", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("sleep")).thenReturn(
            Scene(id = "sleep", name = "Sleep", endAction = SceneEndAction.ChainScene("wakeup"))
        )
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveSceneAndStartScene("wakeup")).thenReturn(
            SceneAutomationResult.ChainCompleted(endedSceneName = "Sleep", targetSceneName = "Wakeup", failedCount = 2, totalCount = 3)
        )

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(aapsLogger).warn(eq(LTag.NSCLIENT), argThat<String> { contains("scene.stop failed") && contains("chain-partial") })
    }

    @Test
    fun sceneStopWithChainFallsBackToPlainStopWhenActiveSceneHasNoChainTarget() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "exercise", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("exercise")).thenReturn(
            Scene(id = "exercise", name = "Exercise", endAction = SceneEndAction.Notification)
        )
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithChainFallsBackToPlainStopWhenNoActiveSceneAtAll() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        // No active scene on master at receipt time — short-circuits before getScene lookup.
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(null)
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(sceneAutomationApi, never()).getScene(any())
        verify(nsAndroidClient, never()).deleteSettings(identifier)
    }

    @Test
    fun pollingListsSettingsAndDispatchesEachClientControlDoc() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val cmdIdentifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_stop_$clientId"
        val cmdDoc = wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(false), counter = 5L)).also {
            it.put("identifier", cmdIdentifier)
        }
        // Mix in a non-clientcontrol doc that polling should ignore.
        val unrelatedDoc = JSONObject().apply {
            put("identifier", "aaps")
            put("date", now)
        }
        whenever(nsAndroidClient.searchSettings(limit = 100)).thenReturn(
            NSAndroidClient.ReadResponse(code = 200, lastServerModified = null, values = listOf(cmdDoc, unrelatedDoc))
        )
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.processPending()

        verify(nsAndroidClient).searchSettings(limit = 100)
        verify(sceneAutomationApi).stopActiveScene()
        // Success path no longer deletes — counter dedup handles replay; leaving the doc avoids
        // tombstoning the slot. Unrelated doc is also untouched.
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun pollingSkipsOfferDocsWithoutDeleting() = runTest {
        // Master writes its own pairing-offer docs under IDENTIFIER_OFFER_PREFIX. Without an
        // explicit skip, verifyAndAck would treat them as envelopes, find no `envelope` field,
        // and delete a still-live offer mid-pairing — leaving the client unable to fetch it.
        val offerDoc = JSONObject().apply {
            put("identifier", "${ClientControlPublisher.IDENTIFIER_OFFER_PREFIX}any-client-id")
            put("date", now)
            put("offer", JSONObject().apply { put("clientId", "any-client-id") })
        }
        whenever(nsAndroidClient.searchSettings(limit = 100)).thenReturn(
            NSAndroidClient.ReadResponse(code = 200, lastServerModified = null, values = listOf(offerDoc))
        )

        sut.processPending()

        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    @Test
    fun wsPushSkipsOfferDocs() = runTest {
        val identifier = "${ClientControlPublisher.IDENTIFIER_OFFER_PREFIX}any-client-id"
        val doc = JSONObject().apply { put("identifier", identifier); put("date", now) }
        sut.onSettingsDocChanged(identifier, doc)
        verify(nsAndroidClient, never()).deleteSettings(any())
    }

    // -- scene_definitions_update --------------------------------------------------------

    /** Compact JSON of a single scene with explicit timestamps, matching `SceneSerializer`'s shape. */
    private fun sceneJson(id: String, name: String, lastModified: Long, isValid: Boolean = true): String =
        """{"id":"$id","name":"$name","icon":"star","defaultDurationMinutes":60,"isDeletable":true,"isEnabled":true,"sortOrder":0,"lastModified":$lastModified,"isValid":$isValid,"actions":[],"endAction":{"type":"notification"}}"""

    private suspend fun sendScenesUpdate(clientId: String, secret: ByteArray, scenesJson: String, counter: Long = 5L) {
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene_definitions_update_$clientId"
        val msg = ClientControlMessage.SceneDefinitionsUpdate(scenesJson)
        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = counter)))
    }

    @Test
    fun scenesUpdateAddsAbsentScene() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        // Existing pref is empty — incoming scene must land.
        sendScenesUpdate(clientId, secret, "[${sceneJson("a", "x", lastModified = 1_000L)}]")

        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"id\":\"a\"")
        assertThat(merged).contains("\"name\":\"x\"")
    }

    @Test
    fun scenesUpdateAppliesWhenIncomingLastModifiedIsNewer() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        // Master starts with an older copy.
        storage[StringNonKey.SceneDefinitions.key] = "[${sceneJson("a", "old-name", lastModified = 1_000L)}]"

        sendScenesUpdate(clientId, secret, "[${sceneJson("a", "new-name", lastModified = 2_000L)}]")

        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"name\":\"new-name\"")
        assertThat(merged).doesNotContain("\"name\":\"old-name\"")
    }

    @Test
    fun scenesUpdateDropsStaleIncoming() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        // Master has the fresher copy.
        storage[StringNonKey.SceneDefinitions.key] = "[${sceneJson("a", "master-name", lastModified = 5_000L)}]"

        sendScenesUpdate(clientId, secret, "[${sceneJson("a", "stale-name", lastModified = 1_000L)}]")

        // Stale upsert dropped → master's name survives. Critical guarantee: offline client
        // edits don't overwrite newer master state.
        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"name\":\"master-name\"")
        assertThat(merged).doesNotContain("\"name\":\"stale-name\"")
    }

    @Test
    fun scenesUpdateEqualLastModifiedIsTreatedAsStale() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        // Equal lastModified → not strictly greater → drop. Matches the receiver's `>` rule;
        // the no-write fast-path also avoids spurious republish round-trips.
        storage[StringNonKey.SceneDefinitions.key] = "[${sceneJson("a", "master-name", lastModified = 5_000L)}]"

        sendScenesUpdate(clientId, secret, "[${sceneJson("a", "client-name", lastModified = 5_000L)}]")

        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"name\":\"master-name\"")
        assertThat(merged).doesNotContain("\"name\":\"client-name\"")
    }

    @Test
    fun scenesUpdateMergesTombstoneAsRegularEntry() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        storage[StringNonKey.SceneDefinitions.key] = "[${sceneJson("a", "active", lastModified = 1_000L)}]"

        // Client publishes the soft-delete with a newer lastModified — master accepts the
        // tombstone in the merged pref (editor-load purge handles physical removal lazily).
        sendScenesUpdate(clientId, secret, "[${sceneJson("a", "active", lastModified = 2_000L, isValid = false)}]")

        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"isValid\":false")
        assertThat(merged).contains("\"lastModified\":2000")
    }

    @Test
    fun scenesUpdateNonOverlappingEditsBothApply() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        // Master has scene A; client publishes the same A unchanged plus a new B. Both must
        // survive — this is the common "I edited a different scene on my phone" case and
        // must NOT clobber unrelated entries.
        storage[StringNonKey.SceneDefinitions.key] = "[${sceneJson("a", "x", lastModified = 1_000L)}]"

        sendScenesUpdate(
            clientId, secret,
            "[${sceneJson("a", "x", lastModified = 1_000L)},${sceneJson("b", "y", lastModified = 2_000L)}]"
        )

        val merged = storage[StringNonKey.SceneDefinitions.key]!!
        assertThat(merged).contains("\"id\":\"a\"")
        assertThat(merged).contains("\"id\":\"b\"")
    }

    @Test
    fun scenesUpdateInvalidJsonLeavesPrefUntouched() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val before = "[${sceneJson("a", "x", lastModified = 1_000L)}]"
        storage[StringNonKey.SceneDefinitions.key] = before

        sendScenesUpdate(clientId, secret, "{not an array}")

        // Garbled JSON from a verified-but-broken client must not corrupt the master's pref.
        assertThat(storage[StringNonKey.SceneDefinitions.key]).isEqualTo(before)
    }

    // -- automation_definitions_update ---------------------------------------------------

    private fun automationJson(id: String, title: String): String =
        """[{"id":"$id","title":"$title","enabled":true,"systemAction":false,"readOnly":false,"autoRemove":false,"userAction":false,"trigger":"{}","actions":[]}]"""

    private suspend fun sendAutomationUpdate(clientId: String, secret: ByteArray, json: String, version: Long, counter: Long = 5L) {
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}automation_definitions_update_$clientId"
        val msg = ClientControlMessage.AutomationDefinitionsUpdate(json, version)
        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = counter)))
    }

    @Test
    fun automationUpdateAppliesWhenIncomingVersionIsNewer() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        automationVersion = 1_000L
        storage[StringNonKey.AutomationEvents.key] = automationJson("a", "old")

        sendAutomationUpdate(clientId, secret, automationJson("a", "new"), version = 2_000L)

        assertThat(storage[StringNonKey.AutomationEvents.key]).contains("\"title\":\"new\"")
        assertThat(automationVersion).isEqualTo(2_000L)
        verify(automation).reloadInternalState()
    }

    @Test
    fun automationUpdateDropsStaleIncoming() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        automationVersion = 5_000L
        storage[StringNonKey.AutomationEvents.key] = automationJson("a", "master")

        sendAutomationUpdate(clientId, secret, automationJson("a", "stale"), version = 1_000L)

        // Offline client edit must not overwrite newer master state.
        assertThat(storage[StringNonKey.AutomationEvents.key]).contains("\"title\":\"master\"")
        assertThat(automationVersion).isEqualTo(5_000L)
        verify(automation, never()).reloadInternalState()
    }

    @Test
    fun automationUpdateEqualVersionIsTreatedAsStale() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        automationVersion = 3_000L
        storage[StringNonKey.AutomationEvents.key] = automationJson("a", "master")

        sendAutomationUpdate(clientId, secret, automationJson("a", "incoming"), version = 3_000L)

        assertThat(storage[StringNonKey.AutomationEvents.key]).contains("\"title\":\"master\"")
        verify(automation, never()).reloadInternalState()
    }

    @Test
    fun automationUpdateInvalidJsonLeavesPrefUntouched() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        automationVersion = 1_000L
        val before = automationJson("a", "x")
        storage[StringNonKey.AutomationEvents.key] = before

        sendAutomationUpdate(clientId, secret, "{not an array}", version = 9_000L)

        assertThat(storage[StringNonKey.AutomationEvents.key]).isEqualTo(before)
        verify(automation, never()).reloadInternalState()
    }

    // -- insulin_configuration_update ----------------------------------------------------

    private fun insulinJson(label: String): String =
        """{"insulin":[{"insulinLabel":"$label","insulinPeakTime":75,"dia":6.0,"concentration":100.0}]}"""

    private suspend fun sendInsulinUpdate(clientId: String, secret: ByteArray, json: String, version: Long, counter: Long = 5L) {
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}insulin_configuration_update_$clientId"
        val msg = ClientControlMessage.InsulinConfigurationUpdate(json, version)
        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = counter)))
    }

    @Test
    fun insulinUpdateAppliesWhenIncomingVersionIsNewer() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        insulinVersion = 1_000L
        storage[StringNonKey.InsulinConfiguration.key] = insulinJson("old")

        sendInsulinUpdate(clientId, secret, insulinJson("new"), version = 2_000L)

        assertThat(storage[StringNonKey.InsulinConfiguration.key]).contains("\"insulinLabel\":\"new\"")
        assertThat(insulinVersion).isEqualTo(2_000L)
        verify(insulin).reloadInternalState()
    }

    @Test
    fun insulinUpdateDropsStaleIncoming() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        insulinVersion = 5_000L
        storage[StringNonKey.InsulinConfiguration.key] = insulinJson("master")

        sendInsulinUpdate(clientId, secret, insulinJson("stale"), version = 1_000L)

        assertThat(storage[StringNonKey.InsulinConfiguration.key]).contains("\"insulinLabel\":\"master\"")
        assertThat(insulinVersion).isEqualTo(5_000L)
        verify(insulin, never()).reloadInternalState()
    }

    @Test
    fun insulinUpdateEqualVersionIsTreatedAsStale() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        insulinVersion = 3_000L
        storage[StringNonKey.InsulinConfiguration.key] = insulinJson("master")

        sendInsulinUpdate(clientId, secret, insulinJson("incoming"), version = 3_000L)

        assertThat(storage[StringNonKey.InsulinConfiguration.key]).contains("\"insulinLabel\":\"master\"")
        verify(insulin, never()).reloadInternalState()
    }

    @Test
    fun insulinUpdateInvalidJsonLeavesPrefUntouched() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        insulinVersion = 1_000L
        val before = insulinJson("x")
        storage[StringNonKey.InsulinConfiguration.key] = before

        sendInsulinUpdate(clientId, secret, "{not json", version = 9_000L)

        assertThat(storage[StringNonKey.InsulinConfiguration.key]).isEqualTo(before)
        verify(insulin, never()).reloadInternalState()
    }

    // -- insulin_activate -----------------------------------------------------------------

    private fun iCfgJson(label: String): String =
        """{"insulinLabel":"$label","insulinEndTime":360,"insulinPeakTime":75,"concentration":100.0,"insulinNickname":"$label"}"""

    private suspend fun sendInsulinActivate(clientId: String, secret: ByteArray, json: String, counter: Long = 5L) {
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}insulin_activate_$clientId"
        val msg = ClientControlMessage.InsulinActivate(json)
        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = counter)))
    }

    @Test
    fun insulinActivateCreatesProfileSwitchWithPushedICfg() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(profileFunction.createProfileSwitchWithNewInsulin(any(), any())).thenReturn(true)

        sendInsulinActivate(clientId, secret, iCfgJson("remote"))

        verify(profileFunction).createProfileSwitchWithNewInsulin(
            argThat<ICfg> { insulinLabel == "remote" },
            eq(Sources.Insulin)
        )
    }

    @Test
    fun insulinActivateWithInvalidJsonDoesNotSwitch() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)

        sendInsulinActivate(clientId, secret, "{not json")

        verify(profileFunction, never()).createProfileSwitchWithNewInsulin(any(), any())
    }

    // -- preferences_update (generic synced-pref channel) ---------------------------------

    private val concentrationKey = BooleanKey.GeneralInsulinConcentration.key

    private suspend fun sendPreferencesUpdate(clientId: String, secret: ByteArray, prefs: Map<String, PrefEntry>, counter: Long = 5L) {
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}preferences_update_$clientId"
        val msg = ClientControlMessage.PreferencesUpdate(prefs)
        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = counter)))
    }

    @Test
    fun preferencesUpdateAppliesStrictlyNewerBidirectionalKey() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(preferences.get(concentrationKey)).thenReturn(BooleanKey.GeneralInsulinConcentration)
        whenever(preferences.get(LongComposedKey.SyncedPrefModified, concentrationKey)).thenReturn(1_000L)

        sendPreferencesUpdate(clientId, secret, mapOf(concentrationKey to PrefEntry("true", 2_000L)))

        verify(preferences).putRemote(BooleanKey.GeneralInsulinConcentration, true, 2_000L)
    }

    @Test
    fun preferencesUpdateDropsStaleByLww() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(preferences.get(concentrationKey)).thenReturn(BooleanKey.GeneralInsulinConcentration)
        whenever(preferences.get(LongComposedKey.SyncedPrefModified, concentrationKey)).thenReturn(5_000L)

        sendPreferencesUpdate(clientId, secret, mapOf(concentrationKey to PrefEntry("true", 1_000L)))

        verify(preferences, never()).putRemote(any<BooleanNonPreferenceKey>(), any(), any())
    }

    @Test
    fun preferencesUpdateAppliesStringKey() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(preferences.get(StringKey.AutomationLocation.key)).thenReturn(StringKey.AutomationLocation)
        whenever(preferences.get(LongComposedKey.SyncedPrefModified, StringKey.AutomationLocation.key)).thenReturn(1_000L)

        sendPreferencesUpdate(clientId, secret, mapOf(StringKey.AutomationLocation.key to PrefEntry("GPS", 2_000L)))

        verify(preferences).putRemote(StringKey.AutomationLocation, "GPS", 2_000L)
    }

    @Test
    fun preferencesUpdateAppliesIntKey() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(preferences.get(IntKey.OverviewBolusPercentage.key)).thenReturn(IntKey.OverviewBolusPercentage)
        whenever(preferences.get(LongComposedKey.SyncedPrefModified, IntKey.OverviewBolusPercentage.key)).thenReturn(1_000L)

        sendPreferencesUpdate(clientId, secret, mapOf(IntKey.OverviewBolusPercentage.key to PrefEntry("80", 2_000L)))

        verify(preferences).putRemote(IntKey.OverviewBolusPercentage, 80, 2_000L)
    }

    @Test
    fun preferencesUpdateRejectsUnknownKey() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        whenever(preferences.get("not_a_synced_key")).thenReturn(null)

        sendPreferencesUpdate(clientId, secret, mapOf("not_a_synced_key" to PrefEntry("true", 9_000L)))

        verify(preferences, never()).putRemote(any<BooleanNonPreferenceKey>(), any(), any())
    }
}
