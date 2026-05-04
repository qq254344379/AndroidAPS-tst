package app.aaps.plugins.sync.nsclientV3.services

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.configuration.RunningConfigurationKeys
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventConfigBuilderChange
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.interfaces.RunningConfiguration
import app.aaps.core.objects.extensions.observeChange
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Master-side publisher for the NS `settings/aaps` document.
 *
 * Observes every preference key in [RunningConfigurationKeys.observableKeys] plus
 * [EventConfigBuilderChange] (plugin switches), debounces 5s, and PATCHes the
 * running configuration JSON to the NS settings collection under identifier `"aaps"`.
 *
 * Document shape: `{ schemaVersion: 1, runningConfig: <RunningConfiguration.configuration()> }`
 *
 * Started by [NSClientV3Plugin.onStart] and stopped by `onStop`.
 * Only active when `config.APS` is true (master role); no-op on aapsclient.
 */
@OptIn(FlowPreview::class)
@Singleton
class RunningConfigurationPublisher @Inject constructor(
    private val runningConfiguration: RunningConfiguration,
    private val runningConfigurationKeys: RunningConfigurationKeys,
    private val nsClientV3Plugin: Provider<NSClientV3Plugin>,
    private val preferences: Preferences,
    private val rxBus: RxBus,
    private val aapsLogger: AAPSLogger,
    private val nsClientRepository: NSClientRepository,
    private val config: Config,
    private val dateUtil: DateUtil,
) {

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (!config.APS) return
        if (job != null) return
        job = scope.launch {
            // initial publish on plugin start so the doc is fresh
            publish()

            val keyTriggers: List<Flow<Unit>> = runningConfigurationKeys.observableKeys()
                .map { preferences.observeChange(it) }
            val switchTrigger: Flow<Unit> = rxBus.toFlow(EventConfigBuilderChange::class.java).map { }

            (keyTriggers + switchTrigger).merge()
                .debounce(DEBOUNCE_MS)
                .collect { publish() }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun publish() {
        val client = nsClientV3Plugin.get().nsAndroidClient ?: return
        val payload = runningConfiguration.configuration()
        if (payload.length() == 0) return // pump not initialized yet
        val doc = JSONObject().apply {
            // NS APIv3 validateCommon requires date / utcOffset / app on UPDATE; all three are
            // immutable after first create, so pick stable constants — the doc represents a
            // configuration snapshot, not an event in time.
            put("date", DOC_DATE)
            put("utcOffset", 0)
            put("app", "AAPS")
            put("schemaVersion", SCHEMA_VERSION)
            put("runningConfig", payload)
        }
        runCatching { client.updateSettings(IDENTIFIER, doc) }
            .onSuccess { resp ->
                if (resp.response in 200..299) {
                    val ts = resp.lastModified?.let { dateUtil.dateAndTimeAndSecondsString(it) } ?: "?"
                    nsClientRepository.addLog("► SETTINGS", "$IDENTIFIER srvModified=$ts")
                } else {
                    aapsLogger.error(LTag.NSCLIENT, "updateSettings($IDENTIFIER) HTTP ${resp.response}: ${resp.errorResponse}")
                    nsClientRepository.addLog("✕ SETTINGS", "$IDENTIFIER HTTP ${resp.response} ${resp.errorResponse ?: ""}")
                }
            }
            .onFailure { e ->
                aapsLogger.error(LTag.NSCLIENT, "updateSettings($IDENTIFIER) failed", e)
                nsClientRepository.addLog("✕ SETTINGS", "$IDENTIFIER ${e.message}")
            }
    }

    companion object {

        private const val IDENTIFIER = "aaps"
        private const val SCHEMA_VERSION = 1
        private const val DEBOUNCE_MS = 5_000L

        // 1 ms past NS APIv3 MIN_TIMESTAMP (946684800000 = 2000-01-01 UTC). Required by
        // validateCommon and immutable after create — kept constant so every update matches.
        private const val DOC_DATE = 946684800001L
    }
}
