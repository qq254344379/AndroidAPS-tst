package app.aaps.core.keys

import app.aaps.core.keys.interfaces.LongNonPreferenceKey

enum class LongNonKey(
    override val key: String,
    override val defaultValue: Long,
    override val exportable: Boolean = true
) : LongNonPreferenceKey {

    LocalProfileLastChange("local_profile_last_change", 0L),

    // Wall-clock ms of this device's last local automation-definitions edit. Whole-list
    // last-writer-wins version for client→master sync. Not exported — a restored stale value could
    // let an old client wrongly win the merge.
    AutomationEventsModified("automation_events_modified", 0L, exportable = false),

    // Wall-clock ms of this device's last local insulin-definitions edit. Whole-list last-writer-wins
    // version for client→master sync. Not exported — a restored stale value could let an old client
    // wrongly win the merge.
    InsulinConfigurationModified("insulin_configuration_modified", 0L, exportable = false),
    BtWatchdogLastBark("bt_watchdog_last", 0L),
    ActivePumpChangeTimestamp("active_pump_change_timestamp", 0L),
    LastCleanupRun("last_cleanup_run", 0L),

    // NSCv3 client-control pairing (excluded from export — replay protection regresses if restored)
    NsClientControlCounterSent("nsclient_control_counter_sent", 0L, exportable = false),

    // When this client paired with master. Used by OrphanDetector to suppress false-positive
    // orphan signals on settings/aaps docs whose srvModified predates the pairing (master
    // hasn't republished the roster yet). Not exported — re-pair regenerates this.
    NsClientControlPairedAt("nsclient_control_paired_at", 0L, exportable = false),
    LastVacuumRun("last_vacuum_run", 0L),
}

