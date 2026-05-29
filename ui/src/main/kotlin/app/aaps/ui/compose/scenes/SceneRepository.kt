package app.aaps.ui.compose.scenes

import app.aaps.core.data.model.Scene
import app.aaps.core.interfaces.scenes.Scenes
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.NonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for scene definitions. Reads/writes scene list from SharedPreferences
 * via StringNonKey.SceneDefinitions.
 *
 * Implements [Scenes] (a [app.aaps.core.interfaces.configuration.ConfigExportImport])
 * so the scene list participates in NS settings sync. No in-memory cache —
 * [scenesFlow] is a [StateFlow] backed by preferences, so external writes propagate
 * automatically and `reloadInternalState` is a no-op.
 */
@Singleton
class SceneRepository @Inject constructor(
    private val preferences: Preferences,
    private val dateUtil: DateUtil
) : Scenes {

    override val syncedKeys: List<NonPreferenceKey> = listOf(StringNonKey.SceneDefinitions)

    override fun reloadInternalState() {}

    /** Observable raw flow of the scene-definitions pref (includes tombstones — UI callers should filter via [List.validOnly]). */
    val scenesFlow: StateFlow<String> = preferences.observe(StringNonKey.SceneDefinitions)

    /** Get the current valid scenes (tombstones excluded). */
    fun getScenes(): List<Scene> = allScenes().validOnly()

    /** Get a scene by ID, ignoring tombstones. */
    fun getScene(id: String): Scene? = getScenes().find { it.id == id }

    /** Includes tombstones (`isValid = false`). Used by [purgeInvalid]; sync layer reads the pref directly. */
    private fun allScenes(): List<Scene> = preferences.get(StringNonKey.SceneDefinitions).toScenes()

    /** Save the full list of scenes */
    fun saveScenes(scenes: List<Scene>) {
        preferences.put(StringNonKey.SceneDefinitions, scenes.toJson())
    }

    /**
     * Add or update a scene. Stamps `lastModified = now` and forces `isValid = true` so the
     * caller can't accidentally publish a stale timestamp — this is the user-edit entry point.
     * Master receivers that merge inbound JSON bypass this and go through [saveScenes] to
     * preserve incoming timestamps.
     */
    fun saveScene(scene: Scene) {
        val stamped = scene.copy(lastModified = dateUtil.now(), isValid = true)
        // Work over the raw list (including tombstones) so re-saving an id that was previously
        // soft-deleted revives the existing slot in place instead of appending a duplicate.
        val scenes = allScenes().toMutableList()
        val index = scenes.indexOfFirst { it.id == stamped.id }
        if (index >= 0) {
            scenes[index] = stamped
        } else {
            scenes.add(stamped)
        }
        saveScenes(scenes)
    }

    /**
     * Soft-delete by id. Sets `isValid = false` and bumps `lastModified` so the tombstone
     * dominates stale upserts in the per-scene LWW merge on the master, and so a client edit
     * made offline still carries its delete intent on the next publish.
     */
    fun deleteScene(id: String) {
        val scenes = allScenes().toMutableList()
        val index = scenes.indexOfFirst { it.id == id }
        if (index < 0) return
        scenes[index] = scenes[index].copy(lastModified = dateUtil.now(), isValid = false)
        saveScenes(scenes)
    }

    /**
     * Physically remove any soft-deleted entries from the local prefs. Called by the editor
     * on load — by then the master has had a chance to apply our delete and republish a
     * tombstone-free snapshot, so any `isValid = false` entries we still hold are safe to GC.
     */
    fun purgeInvalid() {
        val scenes = allScenes()
        if (scenes.any { !it.isValid }) saveScenes(scenes.validOnly())
    }
}

/** Tombstone filter — applied at every public boundary so UI never sees `isValid = false`. */
internal fun List<Scene>.validOnly(): List<Scene> = filter { it.isValid }
