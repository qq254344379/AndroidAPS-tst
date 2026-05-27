package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.data.model.Scene
import app.aaps.core.data.model.SceneEndAction
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.protection.SecureEncrypt
import app.aaps.core.interfaces.scenes.ActiveSceneSnapshot
import app.aaps.core.interfaces.scenes.ActiveSceneSync
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.interfaces.NSAndroidClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
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
    @Mock private lateinit var dateUtil: DateUtil

    // Same deterministic fake as the repository tests — encrypt/decrypt round-trip via reverse,
    // so the persisted form does not contain the original plaintext.
    private val secureEncrypt = object : SecureEncrypt {
        override fun encrypt(plaintextSecret: String, keystoreAlias: String): String = "ENC:$keystoreAlias:${plaintextSecret.reversed()}"
        override fun decrypt(encryptedSecret: String): String = encryptedSecret.removePrefix("ENC:NsClientControlSecret:").reversed()
        override fun isValidDataString(data: String?): Boolean = data != null && data.startsWith("ENC:")
    }

    private lateinit var authorizedRepository: AuthorizedClientsRepository
    private lateinit var sut: ClientControlReceiver

    private var stored = "[]"
    private val now = 1_700_000_000_000L

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        stored = "[]"
        whenever(preferences.get(StringNonKey.NsClientControlAuthorizedClients)).thenAnswer { stored }
        whenever(preferences.put(any<StringNonKey>(), any<String>())).thenAnswer { invocation ->
            stored = invocation.arguments[1] as String
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
            dateUtil,
            aapsLogger
        )
    }

    /** Adds a pending client and returns (clientId, secretBytes). */
    private fun pair(name: String = "phone"): Pair<String, ByteArray> {
        val (entry, secretHex) = authorizedRepository.addPending(name, qrTtlMs = 60_000L, now = now - 10_000L)
        val secretBytes = secretHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return entry.clientId to secretBytes
    }

    /**
     * Adds a pending client whose QR window already expired relative to `now`.
     * Created 2h ago with a 5-min TTL, so qrExpiresAt is well in the past.
     */
    private fun pairExpired(name: String = "phone"): Pair<String, ByteArray> {
        val (entry, secretHex) = authorizedRepository.addPending(name, qrTtlMs = 5 * 60_000L, now = now - 2 * 60 * 60 * 1000L)
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
    fun helloFromPendingClientPromotesToActiveAndDeletes() = runTest {
        val (clientId, secret) = pair()
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val doc = wrap(envelope(clientId, secret, counter = 1L))
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, doc)

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Active)
        assertThat(entry.counterReceived).isEqualTo(1L)
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun helloFromAlreadyActiveClientBumpsLastSeenAndDeletes() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_HELLO_PREFIX + clientId
        val doc = wrap(envelope(clientId, secret, counter = 2L))
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, doc)

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.state).isEqualTo(ClientState.Active)
        assertThat(entry.counterReceived).isEqualTo(2L)
        assertThat(entry.lastSeenAt).isEqualTo(now)
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun verifiedEnvelopeWithUndecodablePayloadAdvancesCounterAndDeletes() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = ClientControlPublisher.IDENTIFIER_CMD_PREFIX + clientId
        // Forge a verified envelope whose payload doesn't match any ClientControlMessage variant.
        // Simulates "newer client speaks a type this older master doesn't know yet".
        val signed = signedEnvelope(clientId, secret, type = "scene.future", payload = """{"type":"scene.future","sceneId":"x"}""", counter = 5L)
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)

        sut.onSettingsDocChanged(identifier, wrap(signed))

        val entry = authorizedRepository.current(now).single { it.clientId == clientId }
        assertThat(entry.counterReceived).isEqualTo(5L)
        verify(nsAndroidClient).deleteSettings(identifier)
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
    fun sceneStartDispatchesToAutomationApiAndDeletes() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.start_$clientId"
        val msg = ClientControlMessage.SceneStart(sceneId = "sleep", durationMinutes = 30)
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.runScene("sleep", 30)).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = msg, counter = 5L)))

        verify(sceneAutomationApi).runScene("sleep", 30)
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithoutChainDispatchesToStopActiveScene() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(false), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithChainResolvesActiveSceneAndDispatchesAndChain() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
        // Simulate master state: active scene "sleep", chained to "wakeup"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "sleep", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("sleep")).thenReturn(
            Scene(id = "sleep", name = "Sleep", endAction = SceneEndAction.ChainScene("wakeup"))
        )
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveSceneAndStartScene("wakeup")).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveSceneAndStartScene("wakeup")
        verify(sceneAutomationApi, never()).stopActiveScene()
        verify(nsAndroidClient).deleteSettings(identifier)
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
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
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
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
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
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(
            ActiveSceneSnapshot(sceneId = "exercise", activatedAt = now - 60_000L, durationMs = 3_600_000L)
        )
        whenever(sceneAutomationApi.getScene("exercise")).thenReturn(
            Scene(id = "exercise", name = "Exercise", endAction = SceneEndAction.Notification)
        )
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun sceneStopWithChainFallsBackToPlainStopWhenNoActiveSceneAtAll() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val identifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
        // No active scene on master at receipt time — short-circuits before getScene lookup.
        whenever(activeSceneSync.activeSceneSnapshot()).thenReturn(null)
        whenever(nsAndroidClient.deleteSettings(identifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.onSettingsDocChanged(identifier, wrap(envelope(clientId, secret, message = ClientControlMessage.SceneStop(true), counter = 5L)))

        verify(sceneAutomationApi).stopActiveScene()
        verify(sceneAutomationApi, never()).stopActiveSceneAndStartScene(any())
        verify(sceneAutomationApi, never()).getScene(any())
        verify(nsAndroidClient).deleteSettings(identifier)
    }

    @Test
    fun pollingListsSettingsAndDispatchesEachClientControlDoc() = runTest {
        val (clientId, secret) = pair()
        authorizedRepository.markActive(clientId, counterReceived = 1L, now = now - 5_000L)
        val cmdIdentifier = "${ClientControlPublisher.IDENTIFIER_CMD_PREFIX}scene.stop_$clientId"
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
        whenever(nsAndroidClient.deleteSettings(cmdIdentifier)).thenReturn(deleteOk)
        whenever(sceneAutomationApi.stopActiveScene()).thenReturn(SceneAutomationResult.Success)

        sut.processPending()

        verify(nsAndroidClient).searchSettings(limit = 100)
        verify(sceneAutomationApi).stopActiveScene()
        verify(nsAndroidClient).deleteSettings(cmdIdentifier)
        // Unrelated doc is ignored — never deleted, never dispatched
        verify(nsAndroidClient, never()).deleteSettings(eq("aaps"))
    }
}
