package app.aaps.wear.tile.source

import android.content.Context
import android.content.res.Resources
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.weardata.EventData
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.wear.R
import app.aaps.wear.interaction.actions.BackgroundActionActivity
import app.aaps.wear.tile.Action
import app.aaps.wear.tile.TileSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneSource @Inject constructor(private val context: Context, private val sp: SP, private val aapsLogger: AAPSLogger) : TileSource {

    override fun getSelectedActions(): List<Action> {
        if (isSceneActive()) {
            aapsLogger.info(LTag.WEAR, "getSelectedActions: scene is active, showing End")
            return listOf(
                Action(
                    buttonText = context.resources.getString(R.string.scene_end),
                    iconRes = R.drawable.ic_cancel_red,
                    activityClass = BackgroundActionActivity::class.java.name,
                    action = EventData.ActionSceneStopPreCheck(),
                )
            )
        }
        val sceneData = getSceneData(sp)
        seedDefaultSlotsIfNeeded(sceneData)

        val selectedEntries = (1..4).mapNotNull { i ->
            val id = sp.getString(preferenceKey(i), "none")
            if (id == "none") null else sceneData.entries.find { it.id == id }
        }
        return selectedEntries.map { entry ->
            Action(
                buttonText = entry.title,
                iconRes = R.drawable.ic_scene_purple,
                activityClass = BackgroundActionActivity::class.java.name,
                action = EventData.ActionScenePreCheck(entry.id, entry.title),
                message = context.resources.getString(R.string.action_scene_confirmation),
            ).also { aapsLogger.info(LTag.WEAR, """getSelectedActions: scene ${entry.title} id=${entry.id}""") }
        }
    }

    override fun getValidFor(): Long? = null

    /** Scene entries currently known to the watch, for the tile settings picker. */
    fun getSceneEntries(): List<EventData.SceneList.SceneEntry> = getSceneData(sp).entries

    private fun preferenceKey(index: Int) = "tile_scene_$index"

    // Seeds the 4 slots with the first scenes in phone order, once — mirrors StaticTileSource's
    // setDefaultSettings(). Deferred until real scene data exists so a watch that hasn't synced
    // yet doesn't permanently lock all slots to "none" before any scenes ever arrived.
    private fun seedDefaultSlotsIfNeeded(sceneData: EventData.SceneList) {
        if (sp.contains(preferenceKey(1)) || sceneData.entries.isEmpty()) return
        val ids = sceneData.entries.map { it.id }
        for (i in 1..4) sp.putString(preferenceKey(i), ids.getOrElse(i - 1) { "none" })
    }

    private fun getSceneData(sp: SP): EventData.SceneList =
        EventData.deserialize(sp.getString(R.string.key_scene_data, EventData.SceneList(arrayListOf()).serialize())) as EventData.SceneList

    private fun isSceneActive(): Boolean {
        val raw = sp.getString(R.string.key_active_scene_state, "")
        if (raw.isEmpty()) return false
        return runCatching { (EventData.deserialize(raw) as? EventData.ActiveSceneState)?.active == true }.getOrDefault(false)
    }

    override fun getResourceReferences(resources: Resources): List<Int> = listOf(R.drawable.ic_scene_purple, R.drawable.ic_cancel_red)
}
