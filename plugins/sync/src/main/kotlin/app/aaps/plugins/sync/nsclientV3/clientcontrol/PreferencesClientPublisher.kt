package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.ClientControlPreferencesSender
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.keys.LongComposedKey
import app.aaps.core.keys.interfaces.BooleanNonPreferenceKey
import app.aaps.core.keys.interfaces.NonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic client-side publisher for bidirectionally-synced **plain preference** edits — the
 * registry-driven analogue of the per-domain `*DefinitionsClientPublisher`s. On an AAPSCLIENT device
 * it collects [Preferences.syncedLocalChanges] (emitted only on LOCAL writes to `Bidirectional`
 * keys), debounces, batches the changed keys with their current value + modified stamp, and pushes
 * them to the master via [ClientControlPreferencesSender]. The master applies per-key
 * last-writer-wins and republishes via the running-config doc.
 *
 * No version-gate is needed here: applied-from-sync writes go through `Preferences.putRemote` and
 * are never emitted on [Preferences.syncedLocalChanges], so the apply→observe→publish echo can't
 * form — origin is known at the write, not inferred.
 *
 * Adding a new bidirectional setting requires no change here — it just shows up as another key on
 * the flow. (Value (de)serialization is currently Boolean-only; extend [serialize] per type as the
 * first non-Boolean bidirectional key lands.)
 */
@OptIn(FlowPreview::class)
@Singleton
class PreferencesClientPublisher @Inject constructor(
    private val preferences: Preferences,
    private val clientControlPreferencesSender: ClientControlPreferencesSender,
    private val config: Config,
    private val aapsLogger: AAPSLogger
) {

    private var job: Job? = null

    // Keys changed since the last successful send, drained on each debounced trigger.
    private val pending = mutableSetOf<NonPreferenceKey>()

    fun start(scope: CoroutineScope) {
        if (!config.AAPSCLIENT) return
        if (job != null) return
        job = scope.launch {
            preferences.syncedLocalChanges
                .onEach { key -> synchronized(pending) { pending.add(key) } }
                .debounce(DEBOUNCE_MS)
                .collect {
                    val batch = synchronized(pending) { pending.toList().also { pending.clear() } }
                    val changes = batch.mapNotNull { key -> serialize(key)?.let { key.key to it } }.toMap()
                    if (changes.isEmpty()) return@collect
                    val result = clientControlPreferencesSender.sendPreferencesUpdate(changes)
                    if (result != ClientControlSendResult.Success)
                    // Re-queue on failure so the next local edit (or retry) picks them up again.
                        synchronized(pending) { pending.addAll(batch) }
                    aapsLogger.debug(LTag.NSCLIENT, "ClientControl: preferences.update keys=${changes.keys} result=$result")
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** (serialized value, lastModified) for a synced key, or null for an unsupported type. */
    private fun serialize(key: NonPreferenceKey): Pair<String, Long>? {
        val value = when (key) {
            is BooleanNonPreferenceKey -> preferences.get(key).toString()
            else                       -> {
                aapsLogger.warn(LTag.NSCLIENT, "ClientControl: preferences.update unsupported key type for ${key.key}, skipping")
                return null
            }
        }
        return value to preferences.get(LongComposedKey.SyncedPrefModified, key.key)
    }

    private companion object {

        private const val DEBOUNCE_MS = 2_000L
    }
}
