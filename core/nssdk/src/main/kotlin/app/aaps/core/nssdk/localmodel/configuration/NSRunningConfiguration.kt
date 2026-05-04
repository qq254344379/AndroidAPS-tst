package app.aaps.core.nssdk.localmodel.configuration

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Typed payload of the NS `settings/aaps` doc's `runningConfig` block.
 *
 * Carries the master AAPS install's currently selected plugin set and their owned
 * preference values, applied by clients via [app.aaps.core.nssdk.interfaces.RunningConfiguration].
 *
 * Lives in the settings collection, not in devicestatus — though the field shape
 * was originally introduced for the (now-removed) devicestatus piggyback channel.
 */
@Serializable
data class NSRunningConfiguration(
    val pump: String? = null,
    val version: String? = null,
    val insulin: Int? = null,
    val aps: String? = null,
    val sensitivity: Int? = null,
    val smoothing: String? = null,
    val insulinConfiguration: JsonObject? = null,
    val apsConfiguration: JsonObject? = null,
    val sensitivityConfiguration: JsonObject? = null,
    val overviewConfiguration: JsonObject? = null,
    val safetyConfiguration: JsonObject? = null,
    val quickWizardConfiguration: JsonObject? = null,
    val scenesConfiguration: JsonObject? = null,
    val automationConfiguration: JsonObject? = null
)
