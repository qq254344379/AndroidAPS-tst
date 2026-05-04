package app.aaps.core.nssdk.remotemodel

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Gson-side mirror of [app.aaps.core.nssdk.localmodel.configuration.NSRunningConfiguration].
 */
internal data class RemoteRunningConfiguration(
    @SerializedName("pump") val pump: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("insulin") val insulin: Int?,
    @SerializedName("aps") val aps: String?,
    @SerializedName("sensitivity") val sensitivity: Int?,
    @SerializedName("smoothing") val smoothing: String?,
    @SerializedName("insulinConfiguration") val insulinConfiguration: JsonObject?,
    @SerializedName("apsConfiguration") val apsConfiguration: JsonObject?,
    @SerializedName("sensitivityConfiguration") val sensitivityConfiguration: JsonObject?,
    @SerializedName("overviewConfiguration") val overviewConfiguration: JsonObject?,
    @SerializedName("safetyConfiguration") val safetyConfiguration: JsonObject?,
    @SerializedName("quickWizardConfiguration") val quickWizardConfiguration: JsonObject?,
    @SerializedName("scenesConfiguration") val scenesConfiguration: JsonObject?,
    @SerializedName("automationConfiguration") val automationConfiguration: JsonObject?
)
