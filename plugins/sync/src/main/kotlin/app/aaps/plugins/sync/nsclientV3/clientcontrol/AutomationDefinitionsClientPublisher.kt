package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.automation.ClientControlAutomationSender
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.keys.LongNonKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.extensions.observeChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side publisher for automation-definition edits — the automation analogue of
 * [SceneDefinitionsClientPublisher].
 *
 * On an AAPSCLIENT device, observes the [StringNonKey.AutomationEvents] preference, debounces, and
 * pushes the current snapshot + its local edit version ([LongNonKey.AutomationEventsModified]) to
 * the master via [ClientControlAutomationSender.sendAutomationUpdate]. The master applies whole-list
 * last-writer-wins by version and republishes via the running-config doc, closing the loop.
 *
 * Only active when [Config.AAPSCLIENT] is true. The initial replay is skipped (observeChange does a
 * drop(1)) so a fresh launch doesn't echo the locally-cached config back to the master — only real
 * user edits trigger a publish.
 */
@OptIn(FlowPreview::class)
@Singleton
class AutomationDefinitionsClientPublisher @Inject constructor(
    private val preferences: Preferences,
    private val clientControlAutomationSender: ClientControlAutomationSender,
    private val config: Config,
    private val aapsLogger: AAPSLogger
) {

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (!config.AAPSCLIENT) return
        if (job != null) return
        job = scope.launch {
            preferences.observeChange(StringNonKey.AutomationEvents)
                .debounce(DEBOUNCE_MS)
                .collect {
                    val snapshot = preferences.get(StringNonKey.AutomationEvents)
                    val version = preferences.get(LongNonKey.AutomationEventsModified)
                    val result = clientControlAutomationSender.sendAutomationUpdate(snapshot, version)
                    aapsLogger.debug(LTag.NSCLIENT, "ClientControl: automation.update publish result=$result")
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {

        private const val DEBOUNCE_MS = 2_000L
    }
}
