package app.aaps.plugins.aps.openAPSSMB

import android.content.SharedPreferences
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.iob.GlucoseStatusProvider
import app.aaps.core.interfaces.profiling.Profiler
import app.aaps.core.interfaces.stats.TddCalculator
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.AdaptiveSwitchPreference
import app.aaps.core.validators.AdaptiveDoublePreference
import app.aaps.core.validators.AdaptiveIntPreference
import app.aaps.core.validators.AdaptiveUnitPreference
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class OpenAPSSMBPluginTest : TestBaseWithProfile() {

    @Mock lateinit var constraintChecker: ConstraintsChecker
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var glucoseStatusProvider: GlucoseStatusProvider
    @Mock lateinit var determineBasalSMB: DetermineBasalSMB
    @Mock lateinit var theme: Theme
    @Mock lateinit var typedArray: TypedArray
    @Mock lateinit var sharedPrefs: SharedPreferences
    @Mock lateinit var bgQualityCheck: BgQualityCheck
    @Mock lateinit var tddCalculator: TddCalculator
    @Mock lateinit var uiInteraction: UiInteraction
    @Mock lateinit var profiler: Profiler
    private lateinit var openAPSSMBPlugin: OpenAPSSMBPlugin
    private lateinit var preferenceManager: PreferenceManager

    init {
        addInjector {
            if (it is AdaptiveDoublePreference) {
                it.profileUtil = profileUtil
                it.preferences = preferences
                it.sharedPrefs = sharedPrefs
            }
            if (it is AdaptiveIntPreference) {
                it.profileUtil = profileUtil
                it.preferences = preferences
                it.sharedPrefs = sharedPrefs
                it.config = config
            }
            if (it is AdaptiveUnitPreference) {
                it.profileUtil = profileUtil
                it.preferences = preferences
                it.sharedPrefs = sharedPrefs
            }
            if (it is AdaptiveSwitchPreference) {
                it.preferences = preferences
                it.sharedPrefs = sharedPrefs
            }
        }
    }

    @BeforeEach fun prepare() {
        preferenceManager = PreferenceManager(context)
        openAPSSMBPlugin = OpenAPSSMBPlugin(
            injector, aapsLogger, rxBus, constraintChecker, rh, profileFunction, profileUtil, config, activePlugin,
            iobCobCalculator, hardLimits, preferences, dateUtil, processedTbrEbData, persistenceLayer, glucoseStatusProvider,
            tddCalculator, bgQualityCheck, uiInteraction, determineBasalSMB, profiler
        )
        `when`(context.theme).thenReturn(theme)
        `when`(context.obtainStyledAttributes(anyObject(), any(), any(), any())).thenReturn(typedArray)
    }

    @Test
    fun specialEnableConditionTest() {
        assertThat(openAPSSMBPlugin.specialEnableCondition()).isTrue()
    }

    @Test
    fun specialShowInListConditionTest() {
        assertThat(openAPSSMBPlugin.specialShowInListCondition()).isTrue()
    }

    @Test
    fun preferenceScreenTest() {
        val ps = openAPSSMBPlugin.preferenceScreen(preferenceManager, context)
        assertThat(ps).isInstanceOf(PreferenceScreen::class.java)
    }
}
