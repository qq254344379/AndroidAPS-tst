package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.interfaces.NSAndroidClient
import app.aaps.core.nssdk.localmodel.clientcontrol.AckEnvelope
import app.aaps.core.nssdk.localmodel.clientcontrol.AckPhase
import app.aaps.core.nssdk.localmodel.clientcontrol.AckStatus
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.localmodel.clientcontrol.MasterPairing
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

/**
 * Tests the client-side round-trip coordinator: the channelFlow that turns a published command into a
 * Sending → MasterExecuting → terminal stream by matching the master's signed ACK on `commandCounter`,
 * with timeout, masterReachable early-abort, poll fallback, and single-in-flight.
 */
internal class ClientControlRoundTripTest {

    @Mock private lateinit var publisher: ClientControlPublisher
    @Mock private lateinit var pairingRepository: ClientPairingRepository
    @Mock private lateinit var nsClientV3Plugin: NSClientV3Plugin
    @Mock private lateinit var nsAndroidClient: NSAndroidClient
    @Mock private lateinit var nsClientRepository: NSClientRepository
    @Mock private lateinit var config: Config
    @Mock private lateinit var dateUtil: DateUtil
    @Mock private lateinit var aapsLogger: AAPSLogger

    private val now = 1_700_000_000_000L
    private val counter = 5L
    private val clientId = "client-1"
    private val secret = ByteArray(32) { 0x17 }
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var reachable: MutableStateFlow<Boolean>
    private lateinit var sut: ClientControlRoundTrip

    private val cmd = ClientControlActionDispatcher.Command.InsulinActivate("""{"insulinLabel":"x"}""")

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        reachable = MutableStateFlow(true)
        whenever(dateUtil.now()).thenReturn(now)
        whenever(pairingRepository.currentPairing()).thenReturn(MasterPairing(masterInstallId = "m", clientId = clientId, masterSecretEnc = "enc"))
        whenever(pairingRepository.secretBytesOrNull()).thenReturn(secret)
        whenever(nsClientV3Plugin.masterReachable).thenReturn(reachable)
        whenever(nsClientV3Plugin.nsAndroidClient).thenReturn(nsAndroidClient)
        whenever(config.AAPSCLIENT).thenReturn(true)
        sut = ClientControlRoundTrip(publisher, pairingRepository, Provider { nsClientV3Plugin }, nsClientRepository, config, dateUtil, aapsLogger)
    }

    private suspend fun stubPublish(result: ClientControlSendResult, ctr: Long? = counter) {
        whenever(publisher.publishTracked(any(), any(), any())).thenReturn(ClientControlPublisher.TrackedPublish(result, ctr))
    }

    /** A signed ACK doc as the WS layer would hand it to onAckDoc. [signSecret] lets a test forge one. */
    private fun ackDoc(phase: AckPhase, status: AckStatus, reason: String? = null, ctr: Long = counter, signSecret: ByteArray = secret): JSONObject {
        val ack = ClientControlCrypto.signAck(signSecret, AckEnvelope(clientId, ctr, phase, status, reason, timestamp = now, signature = ""))
        return JSONObject().apply { put("ack", JSONObject(json.encodeToString(AckEnvelope.serializer(), ack))) }
    }

    @Test
    fun successEmitsSendingMasterExecutingApplied() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent() // Sending + publish + collectors subscribed, no time advanced (no timeout)
        sut.onAckDoc(ackDoc(AckPhase.Executing, AckStatus.Pending))
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok))
        runCurrent()
        job.join()
        assertThat(out).containsExactly(ActionProgress.Sending, ActionProgress.MasterExecuting, ActionProgress.Applied).inOrder()
    }

    @Test
    fun doneFailedEmitsRejectedWithReason() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Failed, reason = FailureReason.NoActiveProfile.name))
        runCurrent()
        job.join()
        assertThat(out.last()).isEqualTo(ActionProgress.Rejected(FailureReason.NoActiveProfile))
    }

    @Test
    fun expiredAckEmitsRejected() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Expired))
        runCurrent()
        job.join()
        assertThat(out.last()).isInstanceOf(ActionProgress.Rejected::class.java)
    }

    @Test
    fun notPairedEmitsRejectedWithoutWaiting() = runTest {
        stubPublish(ClientControlSendResult.NotPaired, ctr = null)
        val out = sut.dispatch(cmd).let { flow -> mutableListOf<ActionProgress>().also { l -> launch { flow.collect { l += it } }.join() } }
        assertThat(out).containsExactly(ActionProgress.Sending, ActionProgress.Rejected(FailureReason.NotPaired)).inOrder()
    }

    @Test
    fun timeoutWithNoAckEmitsUnconfirmed() = runTest {
        stubPublish(ClientControlSendResult.Success)
        whenever(nsAndroidClient.getSettings(any())).thenReturn(NSAndroidClient.ReadResponse(code = 200, lastServerModified = null, values = null))
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        advanceUntilIdle() // past validUntil + margin → withTimeoutOrNull times out → pollAck (null) → Unconfirmed
        job.join()
        assertThat(out.last()).isInstanceOf(ActionProgress.Unconfirmed::class.java)
    }

    @Test
    fun masterUnreachableMidWaitEmitsUnconfirmed() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent() // waiting, reachable still true
        reachable.value = false
        runCurrent()
        job.join()
        assertThat(out.last()).isEqualTo(ActionProgress.Unconfirmed(FailureReason.NotReachable))
    }

    /** Safety: a validly-shaped but WRONG-secret ACK must NOT resolve to Applied. */
    @Test
    fun forgedAckIsIgnoredAndTimesOutUnconfirmed() = runTest {
        stubPublish(ClientControlSendResult.Success)
        whenever(nsAndroidClient.getSettings(any())).thenReturn(NSAndroidClient.ReadResponse(code = 200, lastServerModified = null, values = null))
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok, signSecret = ByteArray(32) { 0x42 })) // forged
        advanceUntilIdle()
        job.join()
        assertThat(out).doesNotContain(ActionProgress.Applied)
        assertThat(out.last()).isInstanceOf(ActionProgress.Unconfirmed::class.java)
    }

    /** A Done ACK for a different command (different counter) must not resolve this request. */
    @Test
    fun ackForDifferentCounterIsIgnored() = runTest {
        stubPublish(ClientControlSendResult.Success)
        whenever(nsAndroidClient.getSettings(any())).thenReturn(NSAndroidClient.ReadResponse(code = 200, lastServerModified = null, values = null))
        val out = mutableListOf<ActionProgress>()
        val job = launch { sut.dispatch(cmd).collect { out += it } }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok, ctr = 99L)) // wrong correlation id
        advanceUntilIdle()
        job.join()
        assertThat(out).doesNotContain(ActionProgress.Applied)
        assertThat(out.last()).isInstanceOf(ActionProgress.Unconfirmed::class.java)
    }

    // ---- liveness (PING-PONG) ----

    @Test
    fun sendPingPublishesPingWithWantsAck() = runTest {
        whenever(publisher.publishTracked(any(), any(), any())).thenReturn(ClientControlPublisher.TrackedPublish(ClientControlSendResult.Success, counter))
        sut.sendPing()
        verify(publisher).publishTracked(eq(ClientControlMessage.Ping), any(), eq(true))
    }

    @Test
    fun validAckBumpsMasterSignal() = runTest {
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok))
        verify(nsClientV3Plugin).bumpMasterSignal(now) // dateUtil.now() == now
    }

    /** Safety: a forged ack must not be treated as a liveness signal either. */
    @Test
    fun forgedAckDoesNotBumpMasterSignal() = runTest {
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok, signSecret = ByteArray(32) { 0x42 }))
        verify(nsClientV3Plugin, never()).bumpMasterSignal(any())
    }

    // ---- run() drives the single app-level modal (pendingAction) ----

    @Test
    fun runSendsCommandAndClearsModalOnApplied() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val cmdPref = ClientControlActionDispatcher.Command.PreferenceEdit(mapOf("boluswizard_percentage" to ("80" to 100L)))
        val job = launch { sut.run(cmdPref, "update settings") }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok))
        advanceUntilIdle() // lets the MIN_MODAL_VISIBLE_MS hold elapse before the auto-dismiss
        job.join()
        verify(publisher).publishTracked(argThat { this is ClientControlMessage.PreferencesUpdate }, any(), any())
        assertThat(sut.pendingAction.value).isNull() // Applied → silent dismiss
    }

    @Test
    fun runReturnsAppliedTerminal() = runTest {
        stubPublish(ClientControlSendResult.Success)
        var terminal: ActionProgress? = null
        val job = launch { terminal = sut.run(cmd, "do thing") }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Ok))
        advanceUntilIdle()
        job.join()
        assertThat(terminal).isEqualTo(ActionProgress.Applied)
    }

    @Test
    fun runRejectedKeepsModalUntilDismiss() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val job = launch { sut.run(cmd, "do thing") }
        runCurrent()
        sut.onAckDoc(ackDoc(AckPhase.Done, AckStatus.Failed, reason = FailureReason.ExecutionFailed.name))
        advanceUntilIdle()
        job.join()
        assertThat(sut.pendingAction.value?.progress).isInstanceOf(ActionProgress.Rejected::class.java)
        sut.dismissActionProgress()
        assertThat(sut.pendingAction.value).isNull()
    }

    @Test
    fun dismissWhileWaitingClearsModalAndStopsRun() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val job = launch { sut.run(cmd, "do thing") }
        runCurrent() // waiting (Sending/MasterExecuting), modal up
        assertThat(sut.pendingAction.value?.progress).isInstanceOf(ActionProgress.Sending::class.java)
        sut.dismissActionProgress() // "stop waiting"
        advanceUntilIdle()
        job.join()
        assertThat(sut.pendingAction.value).isNull()
    }

    @Test
    fun secondConcurrentDispatchIsRejected() = runTest {
        stubPublish(ClientControlSendResult.Success)
        val out1 = mutableListOf<ActionProgress>()
        val job1 = launch { sut.dispatch(cmd).collect { out1 += it } }
        runCurrent() // first dispatch holds inFlight, waiting

        val out2 = mutableListOf<ActionProgress>()
        launch { sut.dispatch(cmd).collect { out2 += it } }.join()
        assertThat(out2).containsExactly(ActionProgress.Rejected(FailureReason.Busy))

        job1.cancel() // releases inFlight via finally
    }
}
