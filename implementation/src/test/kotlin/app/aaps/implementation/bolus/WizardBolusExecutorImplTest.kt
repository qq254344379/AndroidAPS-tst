package app.aaps.implementation.bolus

import app.aaps.core.data.model.BCR
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.TE
import app.aaps.core.data.model.TT
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.bolus.BatchAction
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.constraints.Constraint
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.notifications.NotificationAction
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import app.aaps.core.interfaces.profile.EffectiveProfile
import app.aaps.core.interfaces.profile.ProfileStore
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.objects.runningMode.RunningModeGuard
import app.aaps.core.objects.wizard.BolusWizard
import app.aaps.core.objects.wizard.QuickWizard
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

/**
 * Focused tests for the canonical [WizardBolusExecutorImpl.deliverWizardBolus] entry point — the shared
 * end state that the phone wizard dialog and the watch quick-wizard both funnel into. Asserts the
 * recorded [DetailedBolusInfo] fields, the user-entry log values, and that the BCR is persisted exactly
 * once. `appScope` is `Unconfined` so the bolus coroutine runs synchronously within the call.
 */
class WizardBolusExecutorImplTest : TestBaseWithProfile() {

    @Mock lateinit var quickWizard: QuickWizard
    @Mock lateinit var bolusWizardProvider: Provider<BolusWizard>
    @Mock lateinit var runningModeGuard: RunningModeGuard
    @Mock lateinit var commandQueue: CommandQueue
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var uel: UserEntryLogger
    @Mock lateinit var loop: Loop
    @Mock lateinit var automation: Automation

    private fun create() = WizardBolusExecutorImpl(
        aapsLogger, rh, quickWizard, bolusWizardProvider, profileFunction, profileRepository, insulin, iobCobCalculator, constraintsChecker, activePlugin,
        runningModeGuard, commandQueue, persistenceLayer, uel, loop, dateUtil, decimalFormatter, profileUtil, automation, notificationManager,
        CoroutineScope(Dispatchers.Unconfined)
    )

    // The base mocks ConstraintsChecker without a default answer; the FIXED/batch cap path needs a passthrough.
    private fun stubPassthroughConstraints() {
        whenever(constraintsChecker.applyBolusConstraints(any())).thenAnswer { it.getArgument<Constraint<Double>>(0) }
        whenever(constraintsChecker.applyCarbsConstraints(any())).thenAnswer { it.getArgument<Constraint<Int>>(0) }
        // The merged-confirmation line builders (prepareBatch) call rh.gs(...) + getUnits() heavily; the @Mock rh /
        // profileFunction return null by default → NPE in ConfirmationLine / ProfileUtil. Stub them to non-null.
        whenever(rh.gs(any<Int>())).thenReturn("s")
        whenever(rh.gs(any<Int>(), anyOrNull())).thenReturn("s")
        whenever(rh.gs(any<Int>(), anyOrNull(), anyOrNull())).thenReturn("s")
        whenever(profileFunction.getUnits()).thenReturn(GlucoseUnit.MGDL)
    }

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

    // ---- confirm(): the consume-once delivery the remote-bolus commit (and local QuickWizard) relies on ----

    @Test
    fun confirm_drainsSlotAndDeliversOnce_secondConfirmIsNoPending() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val executor = create()
        executor.setPending(insulin = 2.0, carbs = 0, bolusCalculatorResult = null, bolusId = 999L)

        val first = executor.confirm(999L, Sources.NSClient, { })
        val second = executor.confirm(999L, Sources.NSClient, { })

        assertThat(first).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        assertThat(second).isEqualTo(WizardBolusExecutor.ConfirmResult.NoPending)
        // The consume-once slot guarantees the pump fires EXACTLY once — no double-dose on a re-sent commit.
        verify(commandQueue, times(1)).bolus(anyOrNull())
    }

    @Test
    fun confirm_wrongBolusId_isNoPending_andDeliversNothing() = runTest {
        val executor = create()
        executor.setPending(insulin = 2.0, carbs = 0, bolusCalculatorResult = null, bolusId = 999L)

        val result = executor.confirm(123L, Sources.NSClient, { })

        assertThat(result).isEqualTo(WizardBolusExecutor.ConfirmResult.NoPending)
        verify(commandQueue, never()).bolus(anyOrNull())
    }

    @Test
    fun confirm_parkedDosesAreIsolatedByBolusId_aSecondPrepareDoesNotClobberTheFirst() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val executor = create()
        // bolusId is a timestamp; use realistic recent ids so evictStalePending's TTL window keeps them parked.
        executor.setPending(insulin = 1.0, carbs = 0, bolusCalculatorResult = null, bolusId = now)
        // A second actor's prepare (different bolusId) must NOT clobber the first — per-id slots, not one shared var.
        executor.setPending(insulin = 2.0, carbs = 0, bolusCalculatorResult = null, bolusId = now - 1000)

        // The first parked dose still delivers (not overwritten), and a re-sent commit of it finds the slot drained.
        assertThat(executor.confirm(now, Sources.NSClient, { })).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        assertThat(executor.confirm(now, Sources.NSClient, { })).isEqualTo(WizardBolusExecutor.ConfirmResult.NoPending)
        // The second parked dose is intact and delivers on its own commit.
        assertThat(executor.confirm(now - 1000, Sources.NSClient, { })).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        verify(commandQueue, times(2)).bolus(anyOrNull())
    }

    // ---- prepareBatch() → confirm(): the two-step multi-action transaction (decision-B ordering) ----

    @Test
    fun prepareBatch_raisingTtFirstThenBolus_bothAppliedOnConfirm() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        stubPassthroughConstraints()
        val executor = create()
        val actions = listOf(
            BatchAction.TempTarget(reason = TT.Reason.HYPOGLYCEMIA.text, lowMgdl = 120.0, highMgdl = 120.0, durationMinutes = 60, startOffsetMinutes = 0),
            BatchAction.Bolus(insulin = 1.0, carbs = 0, carbsTimeOffsetMinutes = 0, carbsDurationHours = 0, recordOnly = false, notes = "", timestamp = 0L, iCfg = null)
        )

        val prepared = executor.prepareBatch(actions) as WizardBolusExecutor.PrepareResult.Preview
        val result = executor.confirm(prepared.bolusId, Sources.NSClient, { })

        assertThat(result).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        verify(persistenceLayer).insertAndCancelCurrentTemporaryTarget(any(), any(), any(), anyOrNull(), any()) // target-raising TT applied unconditionally
        verify(commandQueue).bolus(anyOrNull()) // bolus delivered
    }

    @Test
    fun prepareBatch_eatingSoonTtSkipped_whenBolusRejectedAtCommit() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null) // prepare passes the gate
        stubPassthroughConstraints()
        val executor = create()
        val actions = listOf(
            BatchAction.TempTarget(reason = TT.Reason.EATING_SOON.text, lowMgdl = 90.0, highMgdl = 90.0, durationMinutes = 30, startOffsetMinutes = 0),
            BatchAction.Bolus(insulin = 2.0, carbs = 0, carbsTimeOffsetMinutes = 0, carbsDurationHours = 0, recordOnly = false, notes = "", timestamp = 0L, iCfg = null)
        )
        val prepared = executor.prepareBatch(actions) as WizardBolusExecutor.PrepareResult.Preview

        // The loop suspends between prepare and commit → the bolus is mode-rejected at delivery.
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn("loop suspended")
        var err: String? = null
        executor.confirm(prepared.bolusId, Sources.NSClient, { err = it })

        assertThat(err).isNotNull()
        // Decision B: a target-LOWERING eating-soon TT is NOT applied when the bolus fails (else the loop chases the low).
        verify(persistenceLayer, never()).insertAndCancelCurrentTemporaryTarget(any(), any(), any(), anyOrNull(), any())
        verify(commandQueue, never()).bolus(anyOrNull())
    }

    @Test
    fun prepareBatch_profileSwitch_parksAndConfirmAppliesViaCreateProfileSwitch() = runTest {
        stubPassthroughConstraints()
        whenever(profileFunction.getProfile()).thenReturn(mock<EffectiveProfile>())
        whenever(profileFunction.getOriginalProfileName()).thenReturn("Test")
        val executor = create()

        val prepared = executor.prepareBatch(listOf(BatchAction.ProfileSwitch(percentage = 120, timeShiftHours = 2, durationMinutes = 60))) as WizardBolusExecutor.PrepareResult.Preview
        val result = executor.confirm(prepared.bolusId, Sources.NSClient, { })

        assertThat(result).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        // PS-only batch: no pump bolus, no TT — only the dialog-free profile switch applied with the parked values.
        verify(commandQueue, never()).bolus(anyOrNull())
        verify(profileFunction).createProfileSwitch(eq(60), eq(120), eq(2), eq(Action.PROFILE_SWITCH), eq(Sources.NSClient), anyOrNull(), any())
    }

    @Test
    fun prepareBatch_profileSwitch_outOfRange_returnsErrorAndAppliesNothing() = runTest {
        stubPassthroughConstraints()
        whenever(profileFunction.getProfile()).thenReturn(mock<EffectiveProfile>())
        val executor = create()

        val result = executor.prepareBatch(listOf(BatchAction.ProfileSwitch(percentage = 9999, timeShiftHours = 0, durationMinutes = 30)))

        assertThat(result).isInstanceOf(WizardBolusExecutor.PrepareResult.Error::class.java)
        verify(profileFunction, never()).createProfileSwitch(any(), any(), any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun prepareBatch_namedProfileSwitch_appliesViaNamedCreateProfileSwitchFromMasterStore() = runTest {
        stubPassthroughConstraints()
        val store = mock<ProfileStore>()
        whenever(store.getSpecificProfile("Lunch")).thenReturn(mock())
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(store))
        whenever(insulin.iCfg).thenReturn(mock())
        val executor = create()

        val prepared = executor.prepareBatch(listOf(BatchAction.ProfileSwitch(110, 0, 60, profileName = "Lunch"))) as WizardBolusExecutor.PrepareResult.Preview
        val result = executor.confirm(prepared.bolusId, Sources.NSClient, { })

        assertThat(result).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        // The named overload resolves the target from the MASTER's store (a client may relay a name the master owns).
        verify(profileFunction).createProfileSwitch(eq(store), eq("Lunch"), eq(60), eq(110), eq(0), any(), eq(Action.PROFILE_SWITCH), eq(Sources.NSClient), anyOrNull(), any(), any())
    }

    @Test
    fun prepareBatch_namedProfileSwitch_notInMasterStore_returnsError() = runTest {
        stubPassthroughConstraints()
        val store = mock<ProfileStore>()
        whenever(store.getSpecificProfile(any())).thenReturn(null)
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(store))
        val executor = create()

        val result = executor.prepareBatch(listOf(BatchAction.ProfileSwitch(100, 0, 0, profileName = "Ghost")))

        assertThat(result).isInstanceOf(WizardBolusExecutor.PrepareResult.Error::class.java)
    }

    @Test
    fun confirm_asAdvisor_deliversCorrectionAdvisorNotCarbWizardBolus() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(true))
        val executor = create()
        // Parked WITH carbs, but the user took the high-BG advisor branch → correction-only delivery.
        executor.setPending(insulin = 1.5, carbs = 40, bolusCalculatorResult = null, bolusId = 999L)

        val result = executor.confirm(999L, Sources.NSClient, { }, asAdvisor = true)

        assertThat(result).isEqualTo(WizardBolusExecutor.ConfirmResult.Delivered)
        val dbiCaptor = argumentCaptor<DetailedBolusInfo>()
        verify(commandQueue).bolus(dbiCaptor.capture())
        val dbi = dbiCaptor.firstValue
        assertThat(dbi.eventType).isEqualTo(TE.Type.CORRECTION_BOLUS)
        assertThat(dbi.insulin).isEqualTo(1.5)
        assertThat(dbi.carbs).isEqualTo(0.0) // advisor = correction only; the parked carbs are NOT delivered
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
    fun bolus_onCommandFailure_postsUrgentBolusFailedAlarm() = runTest {
        whenever(runningModeGuard.rejectionMessage(any())).thenReturn(null)
        whenever(commandQueue.bolus(anyOrNull())).thenReturn(pumpEnactResultProvider.get().success(false))
        val executor = create()

        executor.deliverWizardBolus(
            insulin = 1.0, carbs = 0, carbTimeMinutes = 0, mgdlGlucose = null,
            bolusCalculatorResult = null, notes = null, source = Sources.QuickWizard, onError = { }
        )

        // The async delivery failure raises the single URGENT alarm from the executor (not the now-gone dialog).
        verify(notificationManager).post(
            eq(NotificationId.BOLUS_DELIVERY_FAILED), any<String>(), any<NotificationLevel>(), any<Int>(),
            anyOrNull<Int>(), any<List<NotificationAction>>(), anyOrNull<() -> Boolean>()
        )
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
