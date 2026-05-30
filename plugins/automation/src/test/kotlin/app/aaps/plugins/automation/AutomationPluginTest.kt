package app.aaps.plugins.automation

import android.Manifest
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.AutomationEvent
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.receivers.ReceiverStatusStore
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.plugins.automation.services.LocationServiceHelper
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

class AutomationPluginTest : TestBaseWithProfile() {

    @Mock lateinit var constraintChecker: ConstraintsChecker
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var loop: Loop
    @Mock lateinit var locationServiceHelper: LocationServiceHelper
    @Mock lateinit var timerUtil: TimerUtil
    @Mock lateinit var receiverStatusStore: ReceiverStatusStore
    @Mock lateinit var uel: UserEntryLogger
    @Mock lateinit var sceneApi: SceneAutomationApi
    private lateinit var automationPlugin: AutomationPlugin

    @BeforeEach fun prepare() {
        automationPlugin = AutomationPlugin(
            injector, aapsLogger, rh, preferences, context, fabricPrivacy, loop, rxBus, constraintChecker,
            aapsSchedulers, config, locationServiceHelper, dateUtil, activePlugin, timerUtil, receiverStatusStore, uel, profileRepository, sceneApi
        )
    }

    @Test
    fun `requiredPermissions should include location permissions`() {
        val allPermissions = automationPlugin.requiredPermissions().flatMap { it.permissions }
        assertThat(allPermissions).contains(Manifest.permission.ACCESS_FINE_LOCATION)
        assertThat(allPermissions).contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertThat(allPermissions).contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    /**
     * Regression guard for the IdentityList wrapper in `notifyChanged()`. The underlying list of
     * mutable AutomationEventObject references doesn't change identity on in-place field mutations
     * (`event.isEnabled = …`), so the default `MutableStateFlow.value =` setter — which uses
     * structural equality — would silently swallow the emission. IdentityList's identity-only
     * equals forces every notifyChanged() to publish. If a future refactor drops the wrapper or
     * overrides AutomationEventObject.equals structurally, this test fails.
     */
    @Test
    fun `notifyChanged always emits even when list contents are unchanged`() = runTest(UnconfinedTestDispatcher()) {
        val captured = mutableListOf<List<AutomationEvent>>()
        val job = automationPlugin.events.onEach { captured += it }.launchIn(this)

        // Three explicit notifyChanged() with no mutations between them — each one must reach the
        // collector. With Unconfined, emissions are observed synchronously on collection.
        automationPlugin.notifyChanged()
        automationPlugin.notifyChanged()
        automationPlugin.notifyChanged()

        job.cancel()
        // 1 seed + 3 explicit emits = 4 captured snapshots, all distinct references.
        assertThat(captured).hasSize(4)
        assertThat(captured.distinctBy { System.identityHashCode(it) }).hasSize(4)
    }

    // NOTE: the "EventWearUpdateTiles fires from plugin scope on notifyChanged" behavior is NOT
    // unit-tested here. AutomationPlugin.onStart() has heavy side effects (loadFromSP, the 1-min
    // processActions loop, preferences flow observation, location-service start) that would need
    // significant test scaffolding — not worth the fragility. The behavior is observable on a real
    // device: NS-sync an automation edit while on the Overview screen, watch the wear tile refresh
    // ~300ms later without opening the Automation screen first.
}
