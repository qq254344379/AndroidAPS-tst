package app.aaps.core.interfaces.configuration

import app.aaps.core.keys.interfaces.NonPreferenceKey

/**
 * Exposes the union of preference keys whose changes should trigger a republish of
 * the running configuration document. Implemented by `RunningConfigurationImpl`.
 *
 * Subscribers should additionally listen to `EventConfigBuilderChange` to catch
 * plugin switches that are not preference-driven.
 */
interface RunningConfigurationKeys {

    /**
     * All preference keys whose values are part of the published running configuration.
     * Combines plugin-owned keys (via `ConfigExportImport.syncedKeys` of all installed
     * Insulin / APS / Sensitivity / Safety / QuickWizard sources) with free-floating
     * overview keys.
     */
    fun observableKeys(): List<NonPreferenceKey>
}
