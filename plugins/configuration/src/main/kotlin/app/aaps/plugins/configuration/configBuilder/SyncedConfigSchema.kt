package app.aaps.plugins.configuration.configBuilder

import app.aaps.core.keys.IntKey
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.UnitDoubleKey
import app.aaps.core.keys.interfaces.NonPreferenceKey

/**
 * Free-floating preference keys carried in the `overviewConfiguration` block of the
 * NS settings doc — keys that have no domain owner with a cache to invalidate.
 *
 * Plugin-owned keys (Insulin / APS / Sensitivity / Safety / QuickWizard) are NOT here —
 * those travel in their own sibling blocks via each plugin's `ConfigExportImport.syncedKeys`,
 * so `applyToPlugin` can call `reloadInternalState()` after writing.
 *
 * `BooleanNonKey.AutosensUsedOnMainPhone` is intentionally NOT in this list — its
 * outbound value is computed (`constraintsChecker.isAutosensModeEnabled()`), not
 * read from preferences. It's handled explicitly in [RunningConfigurationImpl].
 */
object SyncedConfigSchema {

    val overviewKeys: List<NonPreferenceKey> = listOf(
        StringKey.GeneralUnits,
        StringNonKey.TempTargetPresets,
        UnitDoubleKey.OverviewLowMark,
        UnitDoubleKey.OverviewHighMark,
        IntKey.OverviewCageWarning,
        IntKey.OverviewCageCritical,
        IntKey.OverviewIageWarning,
        IntKey.OverviewIageCritical,
        IntKey.OverviewSageWarning,
        IntKey.OverviewSageCritical,
        IntKey.OverviewSbatWarning,
        IntKey.OverviewSbatCritical,
        IntKey.OverviewBageWarning,
        IntKey.OverviewBageCritical,
        IntKey.OverviewResWarning,
        IntKey.OverviewResCritical,
        IntKey.OverviewBattWarning,
        IntKey.OverviewBattCritical,
        IntKey.OverviewBolusPercentage,
    )
}
