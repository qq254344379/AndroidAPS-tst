package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.insulin.ClientControlInsulinSender
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.keys.LongNonKey
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InsulinDefinitionsClientPublisherTest {

    @Mock lateinit var preferences: Preferences
    @Mock lateinit var sender: ClientControlInsulinSender
    @Mock lateinit var config: Config
    @Mock lateinit var aapsLogger: AAPSLogger

    private val configFlow = MutableStateFlow("{}")
    private var version = 0L

    private lateinit var sut: InsulinDefinitionsClientPublisher

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(config.AAPSCLIENT).thenReturn(true)
        whenever(preferences.observe(StringNonKey.InsulinConfiguration)).thenReturn(configFlow)
        whenever(preferences.get(StringNonKey.InsulinConfiguration)).thenAnswer { configFlow.value }
        whenever(preferences.get(LongNonKey.InsulinConfigurationModified)).thenAnswer { version }
        sut = InsulinDefinitionsClientPublisher(preferences, sender, config, aapsLogger)
    }

    @Test
    fun publishesGenuineEditButNotApplyEcho() = runTest {
        whenever(sender.sendInsulinUpdate(any(), any())).thenReturn(ClientControlSendResult.Success)
        sut.start(backgroundScope)
        runCurrent() // subscribe + drop(1) the initial replay

        // Genuine local edit: version bumped + config changed → should publish.
        version = 100L
        configFlow.value = """{"insulin":[{"insulinLabel":"edited"}]}"""
        advanceTimeBy(2_100); runCurrent()
        verify(sender).sendInsulinUpdate(eq("""{"insulin":[{"insulinLabel":"edited"}]}"""), eq(100L))

        // A master-pushed config applied locally rewrites the pref WITHOUT bumping the version —
        // must NOT echo back (this is the loop the version-gate prevents).
        configFlow.value = """{"insulin":[{"insulinLabel":"normalized-by-apply"}]}"""
        advanceTimeBy(2_100); runCurrent()
        verify(sender, times(1)).sendInsulinUpdate(any(), any())

        // A subsequent genuine edit (version increases again) publishes.
        version = 200L
        configFlow.value = """{"insulin":[{"insulinLabel":"edited-again"}]}"""
        advanceTimeBy(2_100); runCurrent()
        verify(sender).sendInsulinUpdate(any(), eq(200L))
    }

    @Test
    fun doesNotPublishOnMaster() = runTest {
        whenever(config.AAPSCLIENT).thenReturn(false)
        sut.start(backgroundScope)
        version = 100L
        configFlow.value = """{"insulin":[{"insulinLabel":"edited"}]}"""
        advanceTimeBy(2_100); runCurrent()
        verify(sender, never()).sendInsulinUpdate(any(), any())
    }
}
