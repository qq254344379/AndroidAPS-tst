package app.aaps.ui.compose.scenes

import app.aaps.core.data.model.Scene
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.StringNonPreferenceKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

internal class SceneRepositoryTest {

    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var dateUtil: DateUtil

    private lateinit var sut: SceneRepository

    // Stand-in for the pref store — captures puts so subsequent gets reflect the write.
    private var stored = "[]"
    private val flow = MutableStateFlow(stored)
    private val now = 1_700_000_000_000L

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(preferences.get(StringNonKey.SceneDefinitions)).thenAnswer { stored }
        whenever(preferences.put(any<StringNonPreferenceKey>(), any<String>())).thenAnswer { invocation ->
            stored = invocation.arguments[1] as String
            flow.value = stored
        }
        whenever(preferences.observe(StringNonKey.SceneDefinitions)).thenReturn(flow)
        whenever(dateUtil.now()).thenReturn(now)
        sut = SceneRepository(preferences, dateUtil)
    }

    // -- saveScene -------------------------------------------------------------------------

    @Test
    fun saveScene_stampsLastModifiedAndForcesValid() {
        sut.saveScene(Scene(id = "a", name = "Exercise"))

        val saved = sut.getScenes().single()
        assertThat(saved.id).isEqualTo("a")
        assertThat(saved.lastModified).isEqualTo(now)
        assertThat(saved.isValid).isTrue()
    }

    @Test
    fun saveScene_ignoresCallerSuppliedLastModifiedAndIsValid() {
        // Caller passes stale timestamp + isValid=false — saveScene is the editor entry point
        // and must overwrite both, otherwise master would receive a publish carrying stale data
        // disguised as a fresh edit.
        sut.saveScene(Scene(id = "a", name = "x", lastModified = 1_000L, isValid = false))

        val saved = sut.getScenes().single()
        assertThat(saved.lastModified).isEqualTo(now)
        assertThat(saved.isValid).isTrue()
    }

    @Test
    fun saveScene_revivesTombstoneInPlace() {
        // First save → tombstone → re-save with the same id should reuse the slot, not append a
        // duplicate. Important for the user "I deleted it, then changed my mind" flow.
        sut.saveScene(Scene(id = "a", name = "x"))
        sut.deleteScene("a")
        // Bump the clock so the revive carries a newer lastModified than the tombstone.
        whenever(dateUtil.now()).thenReturn(now + 1_000L)
        sut.saveScene(Scene(id = "a", name = "x-revived"))

        // getScenes filters tombstones; verifies the slot now holds the revived entry only.
        val visible = sut.getScenes()
        assertThat(visible).hasSize(1)
        assertThat(visible[0].id).isEqualTo("a")
        assertThat(visible[0].name).isEqualTo("x-revived")
        assertThat(visible[0].isValid).isTrue()
        assertThat(visible[0].lastModified).isEqualTo(now + 1_000L)
    }

    @Test
    fun saveScene_addsNewEntryWhenIdAbsent() {
        sut.saveScene(Scene(id = "a", name = "x"))
        sut.saveScene(Scene(id = "b", name = "y"))

        assertThat(sut.getScenes().map { it.id }).containsExactly("a", "b").inOrder()
    }

    // -- deleteScene -----------------------------------------------------------------------

    @Test
    fun deleteScene_softDeletesByFlippingIsValid() {
        sut.saveScene(Scene(id = "a", name = "x"))
        whenever(dateUtil.now()).thenReturn(now + 5_000L)
        sut.deleteScene("a")

        // Visible list filters tombstones → empty.
        assertThat(sut.getScenes()).isEmpty()
        // But the raw pref still carries the entry as a tombstone (proves no physical removal).
        val raw = stored
        assertThat(raw).contains("\"id\":\"a\"")
        assertThat(raw).contains("\"isValid\":false")
        // lastModified bumped so the tombstone outlives any stale upsert that might race in.
        assertThat(raw).contains("\"lastModified\":${now + 5_000L}")
    }

    @Test
    fun deleteScene_unknownIdIsNoOp() {
        sut.saveScene(Scene(id = "a", name = "x"))
        val before = stored
        sut.deleteScene("nope")
        assertThat(stored).isEqualTo(before)
    }

    // -- getScene[s] -----------------------------------------------------------------------

    @Test
    fun getScenes_filtersTombstones() {
        sut.saveScene(Scene(id = "a", name = "x"))
        sut.saveScene(Scene(id = "b", name = "y"))
        sut.deleteScene("a")

        assertThat(sut.getScenes().map { it.id }).containsExactly("b")
    }

    @Test
    fun getScene_returnsNullForTombstone() {
        sut.saveScene(Scene(id = "a", name = "x"))
        sut.deleteScene("a")

        assertThat(sut.getScene("a")).isNull()
    }

    // -- purgeInvalid ----------------------------------------------------------------------

    @Test
    fun purgeInvalid_physicallyRemovesTombstones() {
        sut.saveScene(Scene(id = "a", name = "x"))
        sut.saveScene(Scene(id = "b", name = "y"))
        sut.deleteScene("a")

        // Pre-purge: tombstone is in the raw pref.
        assertThat(stored).contains("\"isValid\":false")

        sut.purgeInvalid()

        // Post-purge: tombstone gone from raw pref too.
        assertThat(stored).doesNotContain("\"isValid\":false")
        assertThat(sut.getScenes().map { it.id }).containsExactly("b")
    }

    @Test
    fun purgeInvalid_noOpWhenAllValid() {
        sut.saveScene(Scene(id = "a", name = "x"))
        val before = stored
        sut.purgeInvalid()
        // No tombstones → no write, pref string unchanged byte-for-byte.
        assertThat(stored).isEqualTo(before)
    }
}
