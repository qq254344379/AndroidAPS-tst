package app.aaps.implementation.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.TE
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.objects.runningMode.RunningModeGuard
import app.aaps.core.objects.wizard.QuickWizard
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Focused tests for the canonical [WizardBolusExecutorImpl.deliverWizardBolus] entry point — the shared
 * end state that the phone wizard dialog and the watch quick-wizard both funnel into. Asserts the
 * recorded [DetailedBolusInfo] fields, the user-entry log values, and that the BCR is persisted exactly
 * once. `appScope` is `Unconfined` so the bolus coroutine runs synchronously within the call.
 */
class WizardBolusExecutorImplTest : TestBaseWithProfile() {

    @Mock lateinit var quickWizard: QuickWizard
    @Mock lateinit var runningModeGuard: RunningModeGuard
    @Mock lateinit var commandQueue: CommandQueue
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var uel: UserEntryLogger
    @Mock lateinit var loop: Loop
    @Mock lateinit var automation: Automation

    private fun create() = WizardBolusExecutorImpl(
        aapsLogger, rh, quickWizard, profileFunction, iobCobCalculator, constraintsChecker, activePlugin,
        runningModeGuard, commandQueue, persistenceLayer, uel, loop, dateUtil, automation,
        CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun deliverWizardBolus_recordsCanonicalBolusWizardAndPersistsBcrOnce() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val bcr = mock<BCR>()
        val executor = create()

        val before = dateUtil.now()
        executor.deliverWizardBolus(
            insulin = 2.0,
            carbs = 30,
            carbTimeMinutes = 20,
            mgdlGlucose = 120.0,
            bolusCalculatorResult = bcr,
            notes = "lunch",
            source = Sources.WizardDialog,
            onError = { }
        )
        val after = dateUtil.now()

        val dbiCaptor = argumentCaptor<DetailedBolusInfo>()
        verify(commandQueue).bolus(dbiCaptor.capture())
        val dbi = dbiCaptor.firstValue
        assertThat(dbi.eventType).isEqualTo(TE.Type.BOLUS_WIZARD)
        assertThat(dbi.insulin).isEqualTo(2.0)
        assertThat(dbi.carbs).isEqualTo(30.0)
        assertThat(dbi.bolusType).isEqualTo(BS.Type.NORMAL)
        assertThat(dbi.mgdlGlucose).isEqualTo(120.0)
        assertThat(dbi.glucoseType).isEqualTo(TE.MeterType.MANUAL)
        assertThat(dbi.notes).isEqualTo("lunch")
        assertThat(dbi.bolusCalculatorResult).isEqualTo(bcr)
        val delay = T.mins(20).msecs()
        assertThat(dbi.carbsTimestamp).isAtLeast(before + delay)
        assertThat(dbi.carbsTimestamp).isAtMost(after + delay)

        val uelValues = argumentCaptor<List<ValueWithUnit>>()
        verify(uel).log(eq(Action.TREATMENT), eq(Sources.WizardDialog), eq("lunch"), uelValues.capture())
        assertThat(uelValues.firstValue).containsExactly(
            ValueWithUnit.TEType(TE.Type.BOLUS_WIZARD),
            ValueWithUnit.Insulin(2.0),
            ValueWithUnit.Gram(30),
            ValueWithUnit.Minute(20)
        ).inOrder()

        verify(persistenceLayer).insertOrUpdateBolusCalculatorResult(bcr)
    }

    @Test
    fun deliverWizardBolus_bolusOnly_omitsGlucoseAndDoesNotPersistNullBcr() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val executor = create()

        executor.deliverWizardBolus(
            insulin = 1.0,
            carbs = 0,
            carbTimeMinutes = 0,
            mgdlGlucose = null,
            bolusCalculatorResult = null,
            notes = null,
            source = Sources.QuickWizard,
            onError = { }
        )

        val dbiCaptor = argumentCaptor<DetailedBolusInfo>()
        verify(commandQueue).bolus(dbiCaptor.capture())
        val dbi = dbiCaptor.firstValue
        assertThat(dbi.eventType).isEqualTo(TE.Type.BOLUS_WIZARD)
        assertThat(dbi.insulin).isEqualTo(1.0)
        assertThat(dbi.carbs).isEqualTo(0.0)
        // mgdlGlucose null → glucoseType is left untouched (not forced to MANUAL)
        assertThat(dbi.mgdlGlucose).isNull()
        assertThat(dbi.glucoseType).isNull()

        val uelValues = argumentCaptor<List<ValueWithUnit>>()
        verify(uel).log(eq(Action.BOLUS), eq(Sources.QuickWizard), anyOrNull(), uelValues.capture())
        assertThat(uelValues.firstValue).containsExactly(
            ValueWithUnit.TEType(TE.Type.BOLUS_WIZARD),
            ValueWithUnit.Insulin(1.0)
        ).inOrder()

        verify(persistenceLayer, never()).insertOrUpdateBolusCalculatorResult(any())
    }

    @Test
    fun deliverBolusAdvisor_recordsCorrectionBolus_schedulesEatReminderOnSuccess() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val bcr = mock<BCR>()
        val executor = create()
        var errored = false

        executor.deliverBolusAdvisor(
            insulin = 1.5,
            mgdlGlucose = 200.0,
            bolusCalculatorResult = bcr,
            notes = "correction",
            source = Sources.WizardDialog,
            onError = { errored = true }
        )

        val dbiCaptor = argumentCaptor<DetailedBolusInfo>()
        verify(commandQueue).bolus(dbiCaptor.capture())
        val dbi = dbiCaptor.firstValue
        assertThat(dbi.eventType).isEqualTo(TE.Type.CORRECTION_BOLUS)
        assertThat(dbi.insulin).isEqualTo(1.5)
        assertThat(dbi.carbs).isEqualTo(0.0)
        assertThat(dbi.mgdlGlucose).isEqualTo(200.0)
        assertThat(dbi.glucoseType).isEqualTo(TE.MeterType.MANUAL)
        assertThat(dbi.bolusCalculatorResult).isEqualTo(bcr)

        val uelValues = argumentCaptor<List<ValueWithUnit>>()
        verify(uel).log(eq(Action.BOLUS_ADVISOR), eq(Sources.WizardDialog), eq("correction"), uelValues.capture())
        assertThat(uelValues.firstValue).containsExactly(
            ValueWithUnit.TEType(TE.Type.CORRECTION_BOLUS),
            ValueWithUnit.Insulin(1.5)
        ).inOrder()

        // BCR rides the DBI but is NOT persisted to the BCR table (matches the legacy advisor paths)
        verify(persistenceLayer, never()).insertOrUpdateBolusCalculatorResult(any())
        // Eat reminder fires on delivery success
        verify(automation).scheduleAutomationEventEatReminder()
        assertThat(errored).isFalse()
    }

    @Test
    fun deliverBolusAdvisor_onFailure_routesToErrorAndSkipsEatReminder() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(false))
        val executor = create()
        var errorMsg: String? = null

        executor.deliverBolusAdvisor(
            insulin = 1.5,
            mgdlGlucose = 200.0,
            bolusCalculatorResult = null,
            notes = null,
            source = Sources.WizardDialog,
            onError = { errorMsg = it }
        )

        assertThat(errorMsg).isNotNull()
        verify(automation, never()).scheduleAutomationEventEatReminder()
    }

    @Test
    fun deliverInsulin_recordsCorrectionBolusWithNoteOnUserEntry() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val executor = create()

        executor.deliverInsulin(
            insulin = 1.0,
            note = "Lunch",
            source = Sources.QuickWizard,
            onError = { }
        )

        val dbiCaptor = argumentCaptor<DetailedBolusInfo>()
        verify(commandQueue).bolus(dbiCaptor.capture())
        val dbi = dbiCaptor.firstValue
        assertThat(dbi.eventType).isEqualTo(TE.Type.CORRECTION_BOLUS)
        assertThat(dbi.insulin).isEqualTo(1.0)
        assertThat(dbi.carbs).isEqualTo(0.0)
        // legacy path left the treatment notes unset; the note rides the user entry only
        assertThat(dbi.notes).isNull()

        val uelValues = argumentCaptor<List<ValueWithUnit>>()
        verify(uel).log(eq(Action.BOLUS), eq(Sources.QuickWizard), eq("Lunch"), uelValues.capture())
        assertThat(uelValues.firstValue).containsExactly(ValueWithUnit.Insulin(1.0)).inOrder()

        verify(persistenceLayer, never()).insertOrUpdateBolusCalculatorResult(any())
    }
}
