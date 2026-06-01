package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.insulin.ClientControlInsulinSender
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.scenes.ClientControlSendResult
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
 * Client-side publisher for insulin-definition edits — the insulin analogue of
 * [SceneDefinitionsClientPublisher] / [AutomationDefinitionsClientPublisher].
 *
 * On an AAPSCLIENT device, observes the [StringNonKey.InsulinConfiguration] preference, debounces, and
 * pushes the current snapshot + its local edit version ([LongNonKey.InsulinConfigurationModified]) to
 * the master via [ClientControlInsulinSender.sendInsulinUpdate]. The master applies whole-list
 * last-writer-wins by version and republishes via the running-config doc, closing the loop.
 *
 * Only active when [Config.AAPSCLIENT] is true. The initial replay is skipped (observeChange does a
 * drop(1)) so a fresh launch doesn't echo the locally-cached config back to the master — only real
 * user edits trigger a publish.
 *
 * Crucially, only **version increases** are published. A genuine local edit bumps
 * [LongNonKey.InsulinConfigurationModified]; a config *applied from the master* (running-config doc)
 * rewrites [StringNonKey.InsulinConfiguration] without bumping the version. Gating on the version
 * therefore ignores apply-driven writes and prevents an apply→observe→publish echo loop (the master
 * republishes on every received command, which would otherwise re-trigger this publisher endlessly).
 */
@OptIn(FlowPreview::class)
@Singleton
class InsulinDefinitionsClientPublisher @Inject constructor(
    private val preferences: Preferences,
    private val clientControlInsulinSender: ClientControlInsulinSender,
    private val config: Config,
    private val aapsLogger: AAPSLogger
) {

    private var job: Job? = null
    private var lastPublishedVersion = 0L

    fun start(scope: CoroutineScope) {
        if (!config.AAPSCLIENT) return
        if (job != null) return
        job = scope.launch {
            lastPublishedVersion = preferences.get(LongNonKey.InsulinConfigurationModified)
            preferences.observeChange(StringNonKey.InsulinConfiguration)
                .debounce(DEBOUNCE_MS)
                .collect {
                    val version = preferences.get(LongNonKey.InsulinConfigurationModified)
                    // Skip writes that didn't advance the version (e.g. a master-pushed config being
                    // applied locally) — only genuine local edits should be pushed back to the master.
                    if (version <= lastPublishedVersion) return@collect
                    val snapshot = preferences.get(StringNonKey.InsulinConfiguration)
                    val result = clientControlInsulinSender.sendInsulinUpdate(snapshot, version)
                    // Advance only on success so a failed publish is retried on the next trigger.
                    if (result == ClientControlSendResult.Success) lastPublishedVersion = version
                    aapsLogger.debug(LTag.NSCLIENT, "ClientControl: insulin.update publish result=$result")
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
