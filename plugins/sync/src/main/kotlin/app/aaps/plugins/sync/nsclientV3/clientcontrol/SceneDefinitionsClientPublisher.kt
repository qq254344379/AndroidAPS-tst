package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.scenes.ClientControlSceneSender
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
 * Client-side publisher for scene-definition edits.
 *
 * On an AAPSCLIENT device, observes the [StringNonKey.SceneDefinitions] preference,
 * debounces, and pushes the current snapshot to the master via
 * [ClientControlSceneSender.sendScenesUpdate]. The master applies a per-scene
 * last-writer-wins merge (by `lastModified`) and republishes via the running-config
 * doc, closing the loop.
 *
 * Only active when [Config.AAPSCLIENT] is true. No-op on master (master owns the
 * scenes pref directly; `RunningConfigurationPublisher` handles its outbound fan-out).
 *
 * The initial pref value is skipped (drop(1)-style via debounce-after-start) so a
 * fresh app launch on a paired client doesn't echo the locally-cached config back to
 * the master — only actual user edits trigger a publish.
 */
@OptIn(FlowPreview::class)
@Singleton
class SceneDefinitionsClientPublisher @Inject constructor(
    private val preferences: Preferences,
    private val clientControlSceneSender: ClientControlSceneSender,
    private val config: Config,
    private val aapsLogger: AAPSLogger
) {

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (!config.AAPSCLIENT) return
        if (job != null) return
        job = scope.launch {
            preferences.observeChange(StringNonKey.SceneDefinitions)
                .debounce(DEBOUNCE_MS)
                .collect {
                    val snapshot = preferences.get(StringNonKey.SceneDefinitions)
                    val result = clientControlSceneSender.sendScenesUpdate(snapshot)
                    aapsLogger.debug(LTag.NSCLIENT, "ClientControl: scenes.update publish result=$result")
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
