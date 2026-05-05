package app.aaps.core.keys

import app.aaps.core.keys.interfaces.StringNonPreferenceKey

enum class StringNonKey(
    override val key: String,
    override val defaultValue: String,
    override val exportable: Boolean = true
) : StringNonPreferenceKey {

    QuickWizard(key = "QuickWizard", defaultValue = "[]"),
    WearCwfWatchfaceName(key = "wear_cwf_watchface_name", defaultValue = ""),
    WearCwfAuthorVersion(key = "wear_cwf_author_version", defaultValue = ""),
    WearCwfFileName(key = "wear_cwf_filename", defaultValue = ""),
    BolusInfoStorage(key = "key_bolus_storage", defaultValue = ""),
    ActivePumpType(key = "active_pump_type", defaultValue = ""),
    ActivePumpSerialNumber(key = "active_pump_serial_number", defaultValue = ""),
    SmsOtpSecret("smscommunicator_otp_secret", defaultValue = ""),
    TotalBaseBasal("TBB", defaultValue = "10.00"),
    PumpCommonBolusStorage(key = "pump_sync_storage_bolus", defaultValue = ""),
    PumpCommonTbrStorage(key = "pump_sync_storage_tbr", defaultValue = ""),
    TempTargetPresets(key = "temp_target_presets", defaultValue = "[]"),
    SceneDefinitions(key = "scene_definitions", defaultValue = "[]"),
    ActiveScene(key = "active_scene", defaultValue = ""),
    QuickLaunchActions(key = "quick_launch_actions", defaultValue = "[{\"type\":\"wizard\"},{\"type\":\"quick_launch_config\"}]"),
    InsulinConfiguration("insulin_configuration", "{}"),
    ComposeGraphConfig("compose_graphconfig", ""),

    NotificationReaderPackages(key = "notification_reader_packages", defaultValue = ""),
    NotificationReaderDedupState(key = "notification_reader_dedup_state", defaultValue = ""),

    // Google Drive settings (internal, no preferences UI)
    @Deprecated("fix")
    GoogleDriveStorageType(key = "google_drive_storage_type", defaultValue = "local"),
    GoogleDriveFolderId(key = "google_drive_folder_id", defaultValue = ""),
    GoogleDriveRefreshToken(key = "google_drive_refresh_token", defaultValue = ""),

    // NSCv3 client-control pairing (excluded from export — secrets / monotonic state)
    NsClientControlAuthorizedClients(key = "nsclient_control_authorized_clients", defaultValue = "[]", exportable = false),
    NsClientControlMasterInstallId(key = "nsclient_control_master_install_id", defaultValue = "", exportable = false),
    NsClientControlClientId(key = "nsclient_control_client_id", defaultValue = "", exportable = false),
    NsClientControlMasterSecretEnc(key = "nsclient_control_master_secret_enc", defaultValue = "", exportable = false),
    NsClientControlCapabilities(key = "nsclient_control_capabilities", defaultValue = "", exportable = false),

}
