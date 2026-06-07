package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.localmodel.clientcontrol.AckEnvelope
import app.aaps.core.nssdk.localmodel.clientcontrol.AckPhase
import app.aaps.core.nssdk.localmodel.clientcontrol.AckStatus
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientControlMessage
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Client-side round-trip coordinator: sends a command via [ClientControlPublisher.publishTracked] and
 * resolves the master's two-step [AckEnvelope] into an [ActionProgress] stream.
 *
 * Wait window is principled — derived from the command's [validUntil] ([ROUND_TRIP_TTL_MS]) plus
 * [PROPAGATION_MARGIN_MS] for the Done ack to travel back. If the master goes unreachable mid-wait we
 * short-circuit to [ActionProgress.Unconfirmed] rather than burning the whole timeout. ACKs arrive via
 * [onAckDoc] (WS push); a single [pollAck] is the last-chance fallback before declaring Unconfirmed.
 *
 * ACK events are buffered with replay so an ack landing in the narrow window between publish and the
 * collector starting is not lost; the per-counter filter discards anything not for the live request.
 */
@Singleton
class ClientControlRoundTrip @Inject constructor(
    private val publisher: ClientControlPublisher,
    private val pairingRepository: ClientPairingRepository,
    private val nsClientV3Plugin: Provider<NSClientV3Plugin>,
    private val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger
) : ClientControlActionDispatcher {

    companion object {

        // The command travels over a live WS to the master and back — a healthy round trip is
        // sub-second, so "fast or never": wait ~10 s total, then treat it as unconfirmed. Validity (8 s)
        // is shorter than the client's give-up (8 s + 2 s margin) so the master never applies a command
        // AFTER the client has stopped waiting; the 2 s margin (and pollAck) catch a last-moment ack.
        const val ROUND_TRIP_TTL_MS = 8_000L
        const val PROPAGATION_MARGIN_MS = 2_000L
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Single command in flight at a time: one shared ACK identifier per client, so two concurrent
    // round-trips would race on it. Set when a dispatch starts collecting, cleared in its finally
    // (including cancellation / "stop waiting").
    private val inFlight = AtomicBoolean(false)

    private val ackEvents = MutableSharedFlow<AckEnvelope>(replay = 16, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * WS-push entry for a master ACK doc (`aaps_clientcontrol_ack_<clientId>`). Parses, checks it is
     * addressed to this client, verifies the signature against the paired master secret (a forged
     * "Ok" must not surface as Applied), and republishes to the in-process [ackEvents].
     */
    fun onAckDoc(doc: JSONObject) {
        val ackObj = doc.optJSONObject("ack") ?: return
        val ack = runCatching { json.decodeFromString<AckEnvelope>(ackObj.toString()) }.getOrNull() ?: run {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: malformed ACK doc")
            return
        }
        val clientId = pairingRepository.currentPairing()?.clientId ?: return
        if (ack.clientId != clientId) return // ack for a different client sharing this NS
        val secret = pairingRepository.secretBytesOrNull() ?: return
        if (!ClientControlCrypto.verifyAck(secret, ack)) {
            aapsLogger.error(LTag.NSCLIENT, "ClientControl: ACK signature invalid (counter=${ack.commandCounter})")
            return
        }
        ackEvents.tryEmit(ack)
    }

    override fun dispatch(command: ClientControlActionDispatcher.Command): Flow<ActionProgress> = channelFlow {
        if (!inFlight.compareAndSet(false, true)) {
            send(ActionProgress.Rejected("another request is already in progress"))
            return@channelFlow
        }
        try {
            send(ActionProgress.Sending)
            val message = when (command) {
                is ClientControlActionDispatcher.Command.InsulinActivate -> ClientControlMessage.InsulinActivate(command.iCfgJson)
            }
            val validUntil = dateUtil.now() + ROUND_TRIP_TTL_MS
            val tracked = publisher.publishTracked(message, validUntil)
            val counter = when (val r = tracked.result) {
                is ClientControlSendResult.NotPaired     -> { send(ActionProgress.Rejected("not paired with a master")); return@channelFlow }
                is ClientControlSendResult.PublishFailed -> { send(ActionProgress.Rejected(r.reason ?: "upload failed")); return@channelFlow }
                is ClientControlSendResult.Success       -> tracked.counter
            }
            if (counter == null) { send(ActionProgress.Rejected("internal error: missing correlation id")); return@channelFlow }

            val plugin = nsClientV3Plugin.get()
            val terminal = CompletableDeferred<ActionProgress>()

            val ackJob = launch {
                ackEvents.filter { it.commandCounter == counter }.collect { ack ->
                    when (ack.phase) {
                        AckPhase.Executing -> if (ack.status == AckStatus.Pending) send(ActionProgress.MasterExecuting)
                        AckPhase.Done      -> doneOutcome(ack)?.let { terminal.complete(it) }
                    }
                }
            }
            // React only to a transition to unreachable (drop the current value). If it was already
            // unreachable the timeout/poll path covers it.
            val reachJob = launch {
                plugin.masterReachable.drop(1).filter { !it }.collect {
                    terminal.complete(ActionProgress.Unconfirmed("connection to master lost while waiting"))
                }
            }

            val waitMs = (validUntil + PROPAGATION_MARGIN_MS - dateUtil.now()).coerceAtLeast(0L)
            val result = withTimeoutOrNull(waitMs) { terminal.await() }
                ?: pollAck(counter)
                ?: ActionProgress.Unconfirmed("no confirmation from master in time")
            ackJob.cancel()
            reachJob.cancel()
            send(result)
        } finally {
            inFlight.set(false)
        }
    }

    private fun doneOutcome(ack: AckEnvelope): ActionProgress? = when (ack.status) {
        AckStatus.Ok      -> ActionProgress.Applied
        AckStatus.Failed  -> ActionProgress.Rejected(ack.reason ?: "master rejected the command")
        AckStatus.Expired -> ActionProgress.Rejected("expired before the master applied it")
        AckStatus.Pending -> null // not terminal
    }

    /** Last-chance single read of the ACK doc before declaring Unconfirmed (covers a WS-missed ack). */
    private suspend fun pollAck(counter: Long): ActionProgress? {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: return null
        val clientId = pairingRepository.currentPairing()?.clientId ?: return null
        val identifier = ClientControlPublisher.IDENTIFIER_ACK_PREFIX + clientId
        val doc = runCatching { client.getSettings(identifier) }.getOrNull()?.values ?: return null
        val ackObj = doc.optJSONObject("ack") ?: return null
        val ack = runCatching { json.decodeFromString<AckEnvelope>(ackObj.toString()) }.getOrNull() ?: return null
        if (ack.clientId != clientId || ack.commandCounter != counter || ack.phase != AckPhase.Done) return null
        val secret = pairingRepository.secretBytesOrNull() ?: return null
        if (!ClientControlCrypto.verifyAck(secret, ack)) return null
        return doneOutcome(ack)
    }
}
