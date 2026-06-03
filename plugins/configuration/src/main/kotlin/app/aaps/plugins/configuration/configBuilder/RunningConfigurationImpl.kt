package app.aaps.plugins.configuration.configBuilder

import app.aaps.core.data.model.SceneLifecycle
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.aps.APS
import app.aaps.core.interfaces.aps.APSResult
import app.aaps.core.interfaces.aps.Sensitivity
import app.aaps.core.interfaces.calibration.Calibration
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.configuration.ConfigBuilder
import app.aaps.core.interfaces.configuration.ConfigExportImport
import app.aaps.core.interfaces.configuration.RunningConfigurationKeys
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.constraints.Safety
import app.aaps.core.interfaces.insulin.Insulin
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.defs.fillFor
import app.aaps.core.interfaces.scenes.ActiveSceneSnapshot
import app.aaps.core.interfaces.scenes.ActiveSceneSync
import app.aaps.core.interfaces.scenes.Scenes
import app.aaps.core.interfaces.smoothing.Smoothing
import app.aaps.core.keys.BooleanNonKey
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.BooleanNonPreferenceKey
import app.aaps.core.keys.interfaces.IntNonPreferenceKey
import app.aaps.core.keys.interfaces.NonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.StringNonPreferenceKey
import app.aaps.core.keys.interfaces.SyncChannel
import app.aaps.core.keys.interfaces.UnitDoublePreferenceKey
import app.aaps.core.nssdk.interfaces.RunningConfiguration
import app.aaps.core.nssdk.localmodel.configuration.NSActiveScene
import app.aaps.core.nssdk.localmodel.configuration.NSRunningConfiguration
import app.aaps.core.objects.extensions.put
import app.aaps.core.objects.extensions.store
import app.aaps.plugins.configuration.R
import dagger.Reusable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@Reusable
class RunningConfigurationImpl @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val insulin: Insulin,
    private val scenes: Scenes,
    private val activeSceneSync: ActiveSceneSync,
    private val configBuilder: ConfigBuilder,
    private val preferences: Preferences,
    private val aapsLogger: AAPSLogger,
    private val config: Config,
    private val pumpSync: PumpSync,
    private val notificationManager: NotificationManager,
    private val nsClientRepository: NSClientRepository,
    private val constraintsChecker: ConstraintsChecker
) : RunningConfiguration, RunningConfigurationKeys {

    // Omit null fields so the frequently-published hot doc stays small.
    private val wireJson = Json { explicitNulls = false }

    // called in AAPS mode only
    override fun configuration(): JSONObject {
        val json = JSONObject()
        val pumpInterface = activePlugin.activePump

        if (!pumpInterface.isInitialized()) return json
        try {
            val sensitivityInterface = activePlugin.activeSensitivity
            val safetyInterface = activePlugin.activeSafety
            val smoothingInterface = activePlugin.activeSmoothing
            val calibrationInterface = activePlugin.activeCalibration
            // APS interface is needed for dynamic sensitivity calculation
            val apsInterface = activePlugin.activeAPS

            json.put("insulin", insulin.id.value)
            json.put("insulinConfiguration", JSONObject(buildFromPlugin(insulin).toString()))
            apsInterface?.let {
                json.put("aps", it.algorithm.name)
                json.put("apsConfiguration", JSONObject(buildFromPlugin(it).toString()))
            }
            json.put("sensitivity", sensitivityInterface.id.value)
            json.put("sensitivityConfiguration", JSONObject(buildFromPlugin(sensitivityInterface).toString()))
            json.put("smoothing", smoothingInterface.javaClass.simpleName)
            json.put("calibration", calibrationInterface.javaClass.simpleName)
            json.put("syncedPrefs", buildSyncedPrefs())
            json.put("safetyConfiguration", JSONObject(buildFromPlugin(safetyInterface).toString()))
            json.put("scenesConfiguration", JSONObject(buildFromPlugin(scenes).toString()))
            json.put("pump", pumpInterface.model().description)
            json.put("version", config.VERSION_NAME)
        } catch (e: JSONException) {
            aapsLogger.error("Unhandled exception", e)
        }
        return json
    }

    // called in AAPS mode only — the small "hot" doc: active scene (if any) + computed runtime flags.
    override fun activeSceneConfiguration(): JSONObject {
        val json = JSONObject()
        try {
            activeSceneSync.activeSceneSnapshot()?.let { snapshot ->
                // Serialize from the wire DTO so the field set stays in lockstep with
                // [NSActiveScene] (no hand-maintained put() list to drift). explicitNulls=false
                // keeps the doc small by omitting not-yet-resolved NS ids.
                val wire = NSActiveScene(
                    sceneId = snapshot.sceneId,
                    activatedAt = snapshot.activatedAt,
                    durationMs = snapshot.durationMs,
                    lifecycle = snapshot.lifecycle.name,
                    ttNsId = snapshot.ttNsId,
                    psNsId = snapshot.psNsId,
                    rmNsId = snapshot.rmNsId,
                    teNsId = snapshot.teNsId
                )
                json.put("activeScene", JSONObject(wireJson.encodeToString(NSActiveScene.serializer(), wire)))
            }
            json.put("usedAutosensOnMainPhone", constraintsChecker.isAutosensModeEnabled().value())
        } catch (e: JSONException) {
            aapsLogger.error("Unhandled exception", e)
        }
        return json
    }

    override fun observableKeys(): List<NonPreferenceKey> {
        val keys = mutableListOf<NonPreferenceKey>()
        keys += insulin.syncedKeys
        activePlugin.getSpecificPluginsListByInterface(APS::class.java).forEach {
            keys += (it as APS).syncedKeys
        }
        activePlugin.getSpecificPluginsListByInterface(Sensitivity::class.java).forEach {
            keys += (it as Sensitivity).syncedKeys
        }
        activePlugin.getSpecificPluginsListByInterface(Safety::class.java).forEach {
            keys += (it as Safety).syncedKeys
        }
        keys += scenes.syncedKeys
        // Cold-channel keys declared via SyncSpec (single source of truth) — a change republishes.
        // QuickWizard lives here now (StringNonKey.QuickWizard is Cold/Bidirectional), not on the
        // document channel — its domain object self-reloads from the key, so no reloadInternalState.
        keys += coldSyncKeys()
        return keys.distinctBy { it.key }
    }

    /** Plain preference keys declared to ride the cold running-config doc via [SyncChannel.Cold]. */
    private fun coldSyncKeys(): List<NonPreferenceKey> =
        preferences.getSyncKeys().filter { it.sync?.channel == SyncChannel.Cold }

    // Flat {keyString: valueAsString} block of cold synced prefs. Values serialized as strings so the
    // wire stays a Map<String, String> regardless of the key's underlying type. Boolean/String/Int/
    // UnitDouble are supported (see the `when` below); other types are skipped with a warning.
    //
    // We sync the RAW persisted value (via the BooleanNonPreferenceKey getter), NOT the mode-adjusted
    // effective value the BooleanPreferenceKey getter returns. Simple mode / defaultedBySM is a
    // per-device presentation choice; the persisted user setting is what should travel, and each
    // device re-applies its own mode logic on read. Do not "fix" this to preferences.get(asPreferenceKey).
    private fun buildSyncedPrefs(): JSONObject {
        val out = JSONObject()
        coldSyncKeys().forEach { key ->
            when (key) {
                is BooleanNonPreferenceKey -> out.put(key.key, preferences.get(key).toString())
                is StringNonPreferenceKey  -> out.put(key.key, preferences.get(key))
                is IntNonPreferenceKey     -> out.put(key.key, preferences.get(key).toString())
                is UnitDoublePreferenceKey -> out.put(key.key, preferences.getRaw(key).toString())   // raw mg/dl, 1:1
                else                       -> aapsLogger.warn(LTag.CORE, "syncedPrefs: unsupported key type for ${key.key}")
            }
        }
        return out
    }

    // Apply master-published synced prefs on the client. "Master wins": adopt verbatim via putRemote
    // (which suppresses the client→master echo and floors the modified stamp; the wire carries no
    // per-key lastModified yet). A later client edit out-stamps it via max(stored+1, now()).
    private fun applySyncedPrefs(prefs: Map<String, String>) {
        prefs.forEach { (keyString, valueString) ->
            val key = preferences.get(keyString) ?: return@forEach
            when (key) {
                is BooleanNonPreferenceKey -> valueString.toBooleanStrictOrNull()?.let { preferences.putRemote(key, it, 0L) }
                is StringNonPreferenceKey  -> preferences.putRemote(key, valueString, 0L)
                is IntNonPreferenceKey     -> valueString.toIntOrNull()?.let { preferences.putRemote(key, it, 0L) }
                is UnitDoublePreferenceKey -> valueString.toDoubleOrNull()?.let { preferences.putRemote(key, it, 0L) }   // raw mg/dl
                else                       -> aapsLogger.warn(LTag.CORE, "syncedPrefs: unsupported key type for $keyString")
            }
        }
    }

    // Runtime state — published to the separate "hot" doc, not the cold config doc.
    override fun hotKeys(): List<NonPreferenceKey> = listOf(StringNonKey.ActiveScene)

    // called in NSClient mode only — apply the cold config doc (everything except the active scene).
    override fun applyCold(configuration: NSRunningConfiguration) {
        assert(config.AAPSCLIENT)

        configuration.version?.let {
            nsClientRepository.addLog("◄ VERSION", "Received AAPS version  $it")
            if (config.VERSION_NAME.startsWith(it).not())
                notificationManager.post(NotificationId.NSCLIENT_VERSION_DOES_NOT_MATCH, R.string.nsclient_version_does_not_match)
        }
        configuration.insulinConfiguration?.let { ic ->
            applyToPlugin(insulin, ic)
        }

        configuration.aps?.let {
            val algorithm = APSResult.Algorithm.valueOf(it)
            for (p in activePlugin.getSpecificPluginsListByInterface(APS::class.java)) {
                val apsPlugin = p as APS
                if (apsPlugin.algorithm == algorithm) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing aps plugin to ${apsPlugin.algorithm}")
                        configBuilder.performPluginSwitch(p, true, PluginType.APS)
                    }
                    configuration.apsConfiguration?.let { ac -> applyToPlugin(apsPlugin, ac) }
                }
            }
        }

        configuration.sensitivity?.let {
            val sensitivity = Sensitivity.SensitivityType.fromInt(it)
            for (p in activePlugin.getSpecificPluginsListByInterface(Sensitivity::class.java)) {
                val sensitivityPlugin = p as Sensitivity
                if (sensitivityPlugin.id == sensitivity) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing sensitivity plugin to ${sensitivity.name}")
                        configBuilder.performPluginSwitch(p, true, PluginType.SENSITIVITY)
                    }
                    configuration.sensitivityConfiguration?.let { sc -> applyToPlugin(sensitivityPlugin, sc) }
                }
            }
        }

        configuration.smoothing?.let {
            for (p in activePlugin.getSpecificPluginsListByInterface(Smoothing::class.java)) {
                val smoothingPlugin = p as Smoothing
                if (smoothingPlugin.javaClass.simpleName == it) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing smoothing plugin to ${smoothingPlugin.javaClass.simpleName}")
                        configBuilder.performPluginSwitch(p, true, PluginType.SMOOTHING)
                    }
                }
            }
        }

        configuration.calibration?.let {
            for (p in activePlugin.getSpecificPluginsListByInterface(Calibration::class.java)) {
                val calibrationPlugin = p as Calibration
                if (calibrationPlugin.javaClass.simpleName == it) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing calibration plugin to ${calibrationPlugin.javaClass.simpleName}")
                        configBuilder.performPluginSwitch(p, true, PluginType.CALIBRATION)
                    }
                }
            }
        }

        configuration.pump?.let {
            if (preferences.get(StringKey.VirtualPumpType) != it) {
                preferences.put(StringKey.VirtualPumpType, it)
                activePlugin.activePump.pumpDescription.fillFor(PumpType.getByDescription(it))
                pumpSync.connectNewPump(endRunning = false) // do not end running TBRs, we call this only to accept data properly
                aapsLogger.debug(LTag.CORE, "Changing pump type to $it")
            }
        }

        configuration.syncedPrefs?.let { applySyncedPrefs(it) }

        configuration.safetyConfiguration?.let { sc ->
            applyToPlugin(activePlugin.activeSafety, sc)
        }

        configuration.scenesConfiguration?.let { sc ->
            applyToPlugin(scenes, sc)
        }
    }

    // called in NSClient mode only — apply the hot doc (active scene + computed runtime flags).
    // Kept separate from [applyCold] so a cold-doc apply (which carries no activeScene) never
    // clears a running scene.
    override fun applyHot(configuration: NSRunningConfiguration) {
        assert(config.AAPSCLIENT)
        // activeScene: null on the wire means "no scene active" — clear locally.
        // Always pass through (even null) so master-side dismissal propagates.
        activeSceneSync.applyActiveScene(configuration.activeScene?.toSnapshot())
        configuration.usedAutosensOnMainPhone?.let { preferences.put(BooleanNonKey.AutosensUsedOnMainPhone, it) }
    }

    private fun NSActiveScene.toSnapshot() =
        ActiveSceneSnapshot(
            sceneId = sceneId,
            activatedAt = activatedAt,
            durationMs = durationMs,
            // null on the wire = pre-lifecycle master, treat as ACTIVE.
            lifecycle = lifecycle?.let { runCatching { SceneLifecycle.valueOf(it) }.getOrNull() }
                ?: SceneLifecycle.ACTIVE,
            ttNsId = ttNsId,
            psNsId = psNsId,
            rmNsId = rmNsId,
            teNsId = teNsId
        )

    private fun buildFromPlugin(plugin: ConfigExportImport): JsonObject =
        plugin.syncedKeys.fold(JsonObject(emptyMap())) { acc, k -> acc.put(k, preferences) }

    private fun applyToPlugin(plugin: ConfigExportImport, json: JsonObject) {
        plugin.syncedKeys.forEach { json.store(it, preferences) }
        plugin.reloadInternalState()
    }

}
