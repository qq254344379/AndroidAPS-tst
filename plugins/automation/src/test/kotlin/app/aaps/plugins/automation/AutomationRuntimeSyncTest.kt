package app.aaps.plugins.automation

import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.receivers.ReceiverStatusStore
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.keys.StringNonKey
import app.aaps.plugins.automation.services.LocationServiceHelper
import app.aaps.plugins.automation.triggers.Trigger
import app.aaps.plugins.automation.triggers.TriggerConnector
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Prototype proof for moving Automation onto the generic `SyncSpec(Bidirectional)` channel
 * (scope steps 1–3 + the `storeToSP` band-aid), WITHOUT deleting the dedicated channel yet. Verifies:
 *
 *  1. **The band-aid converges the self-observe loop.** With Automation self-observing its own key,
 *     a local write re-triggers the observer (put → observe → loadFromSP → notifyChanged → store).
 *     The band-aid (skip the write when the serialized list already equals what's stored) makes the
 *     re-store a no-op, so the loop self-terminates after one write instead of spinning. A reload of
 *     identical content is therefore a fixed point — zero further writes.
 *  2. **Self-observe applies a master push, and applying it does not echo a write back.**
 *
 * Persistence is debounced on the runtime's internal scope, so [AutomationRuntime.start] is given an
 * injectable scope bound to the test scheduler (so `advanceUntilIdle()` drives the 300 ms debounce).
 * The fake below layers an in-memory `AutomationEvents` value over the `@Mock preferences`: `observe()`
 * feeds the self-observe subscription; `put()` records a LOCAL write and updates the observed flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutomationRuntimeSyncTest : TestBaseWithProfile() {

    @Mock lateinit var constraintChecker: ConstraintsChecker
    @Mock lateinit var loop: Loop
    @Mock lateinit var locationServiceHelper: LocationServiceHelper
    @Mock lateinit var timerUtil: TimerUtil
    @Mock lateinit var receiverStatusStore: ReceiverStatusStore
    @Mock lateinit var uel: UserEntryLogger
    @Mock lateinit var sceneApi: SceneAutomationApi

    private lateinit var automationRuntime: AutomationRuntime

    // Seed with an empty JSON array (not "") so loadFromSP doesn't inject the EMPTY_EVENT default,
    // whose TriggerBg/TriggerDelta need more injected fields than this test wires. Our own events use
    // an empty TriggerConnector, which (de)serializes with just rh/aapsLogger.
    private val autoFlow = MutableStateFlow("[]")
    private var localPutCount = 0

    init {
        addInjector {
            if (it is Trigger) {
                it.rh = rh
                it.aapsLogger = aapsLogger
            }
        }
    }

    @BeforeEach fun prepare() {
        // In-memory fake for StringNonKey.AutomationEvents (overrides the base's generic observe stub).
        whenever(preferences.observe(StringNonKey.AutomationEvents)).thenReturn(autoFlow)
        whenever(preferences.get(StringNonKey.AutomationEvents)).thenAnswer { autoFlow.value }
        doAnswer { autoFlow.value = it.getArgument(1); localPutCount++; null }
            .whenever(preferences).put(eq(StringNonKey.AutomationEvents), any<String>())

        whenever(config.APS).thenReturn(false)       // client: start() returns right after the persistence subs
        whenever(config.AAPSCLIENT).thenReturn(true) // enables the new self-observe subscription

        automationRuntime = AutomationRuntime(
            injector, aapsLogger, rh, preferences, context, fabricPrivacy, loop, rxBus, constraintChecker,
            aapsSchedulers, config, locationServiceHelper, dateUtil, activePlugin, timerUtil, receiverStatusStore,
            uel, profileRepository, sceneApi
        )
    }

    private fun event(title: String) = AutomationEventObject(injector).apply {
        this.title = title
        trigger = TriggerConnector(injector)
    }

    @Test
    fun `band-aid converges the apply-observe-store loop and reload writes nothing`() = runTest {
        val sutScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        automationRuntime.start(sutScope)
        advanceUntilIdle() // collectors subscribe; drop(1) consumes the load seed

        // A genuine edit persists once. Its own write re-triggers the observer; the band-aid skips the
        // identical re-store, so this CONVERGES (advanceUntilIdle returns) with exactly one write.
        automationRuntime.add(event("t1"))
        advanceUntilIdle()

        assertThat(automationRuntime.events.value.map { it.title }).contains("t1")
        val writes = localPutCount
        assertThat(writes).isEqualTo(1)
        assertThat(autoFlow.value).contains("t1")

        // Reload of identical content (the master-apply path) is a fixed point — zero new writes.
        automationRuntime.loadFromSP()
        advanceUntilIdle()
        assertThat(localPutCount).isEqualTo(writes)

        sutScope.cancel()
    }

    @Test
    fun `self-observe applies an external master push without echoing back`() = runTest {
        val sutScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        automationRuntime.start(sutScope)
        advanceUntilIdle()

        automationRuntime.add(event("only-on-master"))
        advanceUntilIdle()
        val masterConfig = autoFlow.value                       // canonical config, as the master would hold it
        val masterSize = automationRuntime.events.value.size

        // Client diverges locally to a longer list.
        automationRuntime.add(event("local-extra"))
        advanceUntilIdle()
        assertThat(automationRuntime.events.value.size).isEqualTo(masterSize + 1)
        val writesBeforePush = localPutCount

        // Master pushes its (different) config: a putRemote-style external write feeds observe() WITHOUT
        // counting as a local put.
        autoFlow.value = masterConfig
        advanceUntilIdle()

        // Self-observe applied it (in-memory list reverted to the master's size)…
        assertThat(automationRuntime.events.value.size).isEqualTo(masterSize)
        // …and the apply did NOT echo a local write back (band-aid: re-serialized == pushed → store skipped).
        assertThat(localPutCount).isEqualTo(writesBeforePush)

        sutScope.cancel()
    }
}
