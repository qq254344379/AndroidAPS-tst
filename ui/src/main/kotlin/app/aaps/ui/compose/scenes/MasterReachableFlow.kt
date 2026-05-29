package app.aaps.ui.compose.scenes

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.sync.NsClient
import app.aaps.core.objects.extensions.freshness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/**
 * Shared "master is reachable for scene operations" signal.
 *
 * On master devices this is constantly `true` — master IS the master, no remote dependency.
 * On AAPSCLIENT the signal combines two facts:
 *
 *  1. **Live WebSocket** — [NsClient.wsConnectedFlow], with a [graceMs] falling-edge grace so
 *     brief WS flaps don't lock the UI (reconnects unlock immediately).
 *  2. **Master heartbeat** — derived from [NsClient.lastDevicestatusReceivedAt] via
 *     [freshness]: pure WS-state can't detect "client healthy, master dead" (master's phone
 *     offline / crashed while the client is still cheerfully talking to NS). Master publishes
 *     devicestatus per loop cycle (~5 min), so a gap of more than [heartbeatStaleMs] means
 *     the loop publisher has gone silent. The shared [freshness] helper handles the wall-clock
 *     re-evaluation tick.
 *
 * **Pre-first-heartbeat:** while `lastDevicestatusReceivedAt == 0L` (never received) we treat
 * master as reachable. Otherwise a freshly-paired AAPSCLIENT would be false-locked the moment
 * it came up — master needs a window to publish its first devicestatus.
 *
 * Used by every scene-control entry point that needs to gate edit / activate / stop actions
 * on AAPSCLIENT — [SceneListViewModel], [app.aaps.ui.compose.scenesSheet.ScenesViewModel],
 * and the scene action paths in [app.aaps.ui.compose.main.MainViewModel].
 */
@Suppress("OPT_IN_USAGE")
@OptIn(FlowPreview::class)
internal fun masterReachableFlow(
    nsClient: NsClient,
    config: Config,
    scope: CoroutineScope,
    graceMs: Long = WS_DISCONNECT_GRACE_MS,
    heartbeatStaleMs: Long = HEARTBEAT_STALE_MS,
    now: () -> Long = { System.currentTimeMillis() }
): StateFlow<Boolean> {
    if (!config.AAPSCLIENT) return MutableStateFlow(true).asStateFlow()

    val wsWithGrace = nsClient.wsConnectedFlow.transformLatest { connected ->
        if (connected) emit(true)
        else {
            delay(graceMs)
            emit(false)
        }
    }
    val heartbeatFresh = nsClient.lastDevicestatusReceivedAt.freshness(
        thresholdMs = heartbeatStaleMs,
        scope = scope,
        tickMs = HEARTBEAT_TICK_MS,
        pristine = true,
        now = now
    )
    return combine(wsWithGrace, heartbeatFresh) { ws, fresh -> ws && fresh }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
}

/** Grace period before showing the offline banner on WS disconnect — long enough to swallow
 *  brief WS flaps (reconnect storms during NS server restarts are common), short enough that
 *  a real outage surfaces within a few seconds. */
internal const val WS_DISCONNECT_GRACE_MS = 5_000L

/** Heartbeat staleness threshold — about 1.8 loop cycles. Matches the BG-stale convention
 *  and gives one missed devicestatus publication of grace before flagging master as silent. */
internal const val HEARTBEAT_STALE_MS = 9 * 60_000L

/** Ticker cadence for freshness re-evaluation. Short enough that staleness surfaces within
 *  ~1 minute of the threshold being crossed, long enough not to burn cycles. */
internal const val HEARTBEAT_TICK_MS = 60_000L
