package app.aaps.plugins.sync.nsclientV3.clientcontrol

import app.aaps.core.interfaces.configuration.ClientControlPreferencesSender
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.LongComposedKey
import app.aaps.core.keys.interfaces.BooleanNonPreferenceKey
import app.aaps.core.keys.interfaces.NonPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesClientPublisherTest {

    @Mock lateinit var preferences: Preferences
    @Mock lateinit var sender: ClientControlPreferencesSender
    @Mock lateinit var config: Config
    @Mock lateinit var aapsLogger: AAPSLogger

    private val changes = MutableSharedFlow<NonPreferenceKey>(extraBufferCapacity = 10)
    private val key = BooleanKey.GeneralInsulinConcentration

    private lateinit var sut: PreferencesClientPublisher

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(config.AAPSCLIENT).thenReturn(true)
        whenever(preferences.syncedLocalChanges).thenReturn(changes)
        // The publisher reads the value via the BooleanNonPreferenceKey overload (smart-cast), so stub that one.
        whenever(preferences.get(key as BooleanNonPreferenceKey)).thenReturn(true)
        whenever(preferences.get(LongComposedKey.SyncedPrefModified, key.key)).thenReturn(100L)
        sut = PreferencesClientPublisher(preferences, sender, config, aapsLogger)
    }

    @Test
    fun publishesLocalSyncedChangeAsValuePlusStamp() = runTest {
        whenever(sender.sendPreferencesUpdate(any())).thenReturn(ClientControlSendResult.Success)
        sut.start(backgroundScope)
        runCurrent()

        changes.emit(key)
        advanceTimeBy(2_100); runCurrent()

        verify(sender).sendPreferencesUpdate(eq(mapOf(key.key to ("true" to 100L))))
    }

    @Test
    fun doesNotPublishOnMaster() = runTest {
        whenever(config.AAPSCLIENT).thenReturn(false)
        sut.start(backgroundScope)

        changes.emit(key)
        advanceTimeBy(2_100); runCurrent()

        verify(sender, never()).sendPreferencesUpdate(any())
    }
}
