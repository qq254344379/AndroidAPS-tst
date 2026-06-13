package app.aaps.plugins.sync.wear.wearintegration

import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ui.ConfirmationLine
import app.aaps.core.data.ui.ConfirmationRole
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.bolus.BatchAction
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.maintenance.ImportExportPrefs
import app.aaps.core.interfaces.nsclient.ProcessedDeviceStatusData
import app.aaps.core.interfaces.pump.PumpStatusProvider
import app.aaps.core.interfaces.pump.PumpWithConcentration
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.receivers.ReceiverStatusStore
import app.aaps.core.interfaces.rx.events.EventMobileToWear
import app.aaps.core.interfaces.rx.weardata.EventData
import app.aaps.core.interfaces.rx.weardata.EventData.RunningModeList.AvailableRunningMode
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.interfaces.utils.TrendCalculator
import app.aaps.core.objects.runningMode.RunningModeGuard
import app.aaps.core.objects.wizard.BolusWizard
import app.aaps.core.objects.wizard.QuickWizard
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import javax.inject.Provider

/**
 * Tests for the unified wear **manual-bolus** ([DataHandlerMobile.handleBolusPreCheck]) and
 * **eCarbs** ([DataHandlerMobile.handleECarbsPreCheck]) precheck handlers.
 *
 * These two were migrated off their bespoke local capping + concatenated confirm-strings + echoed
 * amounts onto the shared [WizardBolusExecutor.prepareBatch] path. The behaviour this locks in:
 * - the master caps/parks (executor is the single authority) — the handler only adapts the wear
 *   command into a [BatchAction.Bolus] and ships the master's [ConfirmAction.lines] verbatim,
 * - the confirm round-trip carries **only the parked `bolusId`** (consume-once), never an echoed
 *   amount that a stale watch could resend,
 * - a `Preview` is forwarded as a `ConfirmAction`; an `Error` becomes a `sendError`,
 * - a client ([Config.AAPSCLIENT]) is rejected before the executor for an insulin delivery, but a
 *   carbs-only entry is allowed through (it syncs).
 *
 * The handlers are `internal` (not private) so they can be driven directly here, without scaffolding
 * the full RxBus wiring around DataHandlerMobile's 30+ constructor deps. The emitted [EventMobileToWear]
 * is captured off the real [RxBusImpl] (a synchronous `onNext`).
 */
class DataHandlerMobileWearBolusTest : TestBaseWithProfile() {

    @Mock private lateinit var loop: Loop
    @Mock private lateinit var processedDeviceStatusData: ProcessedDeviceStatusData
    @Mock private lateinit var receiverStatusStore: ReceiverStatusStore
    @Mock private lateinit var quickWizard: QuickWizard
    @Mock private lateinit var trendCalculator: TrendCalculator
    @Mock private lateinit var commandQueue: CommandQueue
    @Mock private lateinit var uiInteraction: UiInteraction
    @Mock private lateinit var persistenceLayer: PersistenceLayer
    @Mock private lateinit var importExportPrefs: ImportExportPrefs
    @Mock private lateinit var bolusWizardProvider: Provider<BolusWizard>
    @Mock private lateinit var pumpStatusProvider: PumpStatusProvider
    @Mock private lateinit var runningModeGuard: RunningModeGuard
    @Mock private lateinit var wizardBolusExecutor: WizardBolusExecutor
    @Mock private lateinit var pump: PumpWithConcentration
    @Mock private lateinit var automation: Automation

    private lateinit var sut: DataHandlerMobile

    @BeforeEach fun prepare() {
        sut = DataHandlerMobile(
            aapsSchedulers, context, rxBus, aapsLogger, rh, preferences, config,
            iobCobCalculator, processedTbrEbData, smbGlucoseStatusProvider, profileFunction, profileUtil,
            loop, processedDeviceStatusData, receiverStatusStore, quickWizard, trendCalculator, dateUtil,
            constraintsChecker, activePlugin, insulin, commandQueue, fabricPrivacy, uiInteraction,
            persistenceLayer, importExportPrefs, decimalFormatter, bolusWizardProvider, pumpStatusProvider,
            ch, runningModeGuard, wizardBolusExecutor
        )
        sut.automation = automation
        // Confirm-title + error-title + client-reject string all go through the single-arg gs().
        whenever(rh.gs(any<Int>())).thenReturn("CONFIRM")
        whenever(activePlugin.activePump).thenReturn(pump)
        whenever(pump.isInitialized()).thenReturn(true)
    }

    /** Capture the single [ConfirmAction] the handler ships to the watch via [EventMobileToWear]. */
    private inline fun capturedConfirm(block: () -> Unit): EventData.ConfirmAction {
        var captured: EventData? = null
        val d = rxBus.toObservable(EventMobileToWear::class.java).subscribe { captured = it.payload }
        block()
        d.dispose()
        return captured as EventData.ConfirmAction
    }

    private fun stubPreview(insulin: Double, carbs: Int, bolusId: Long, lines: List<ConfirmationLine>) =
        runBlocking {
            whenever(wizardBolusExecutor.prepareBatch(any()))
                .thenReturn(WizardBolusExecutor.PrepareResult.Preview(insulin, carbs, "", bolusId, lines))
        }

    @Test fun `bolus precheck parks a fixed insulin plus carbs and ships bolusId plus master lines`() = runTest {
        stubPreview(
            1.0, 10, bolusId = 4242L,
            lines = listOf(ConfirmationLine(ConfirmationRole.BOLUS, "Bolus: 1.00 U"), ConfirmationLine(ConfirmationRole.CARBS, "Carbs: 10 g"))
        )

        val confirm = capturedConfirm { runBlocking { sut.handleBolusPreCheck(EventData.ActionBolusPreCheck(insulin = 1.0, carbs = 10)) } }

        // Executor is the single capping/parking authority — driven with a FIXED, immediate bolus.
        val captor = argumentCaptor<List<BatchAction>>()
        verifyBlocking(wizardBolusExecutor) { prepareBatch(captor.capture()) }
        val bolus = captor.firstValue.single() as BatchAction.Bolus
        assertThat(bolus.insulin).isEqualTo(1.0)
        assertThat(bolus.carbs).isEqualTo(10)
        assertThat(bolus.carbsTimeOffsetMinutes).isEqualTo(0)
        assertThat(bolus.carbsDurationHours).isEqualTo(0)
        assertThat(bolus.recordOnly).isFalse()

        // The round-trip carries only the parked bolusId + the master's lines — no echoed amount, no concatenated message.
        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionBolusConfirmed(4242L))
        assertThat(confirm.message).isEmpty()
        assertThat(confirm.lines).containsExactly(
            EventData.ConfirmActionLine("BOLUS", "Bolus: 1.00 U"),
            EventData.ConfirmActionLine("CARBS", "Carbs: 10 g")
        ).inOrder()
    }

    @Test fun `bolus precheck error from the executor becomes a sendError to the watch`() = runTest {
        runBlocking { whenever(wizardBolusExecutor.prepareBatch(any())).thenReturn(WizardBolusExecutor.PrepareResult.Error("boom")) }

        val sent = capturedConfirm { runBlocking { sut.handleBolusPreCheck(EventData.ActionBolusPreCheck(insulin = 1.0, carbs = 0)) } }

        assertThat(sent.returnCommand).isInstanceOf(EventData.Error::class.java)
        assertThat(sent.message).isEqualTo("boom")
    }

    @Test fun `insulin bolus precheck on a client is rejected before reaching the executor`() = runTest {
        whenever(config.AAPSCLIENT).thenReturn(true)

        val sent = capturedConfirm { runBlocking { sut.handleBolusPreCheck(EventData.ActionBolusPreCheck(insulin = 1.0, carbs = 0)) } }

        verifyBlocking(wizardBolusExecutor, never()) { prepareBatch(any()) }
        assertThat(sent.returnCommand).isInstanceOf(EventData.Error::class.java)
    }

    @Test fun `carbs-only bolus precheck on a client is allowed (no insulin to reject)`() = runTest {
        whenever(config.AAPSCLIENT).thenReturn(true)
        stubPreview(0.0, 10, bolusId = 7L, lines = listOf(ConfirmationLine(ConfirmationRole.CARBS, "Carbs: 10 g")))

        val confirm = capturedConfirm { runBlocking { sut.handleBolusPreCheck(EventData.ActionBolusPreCheck(insulin = 0.0, carbs = 10)) } }

        verifyBlocking(wizardBolusExecutor) { prepareBatch(any()) }
        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionBolusConfirmed(7L))
    }

    @Test fun `ecarbs precheck maps to a fixed carbs-only bolus carrying offset plus duration with an ECarbs confirm`() = runTest {
        stubPreview(
            0.0, 20, bolusId = 99L,
            lines = listOf(ConfirmationLine(ConfirmationRole.CARBS, "Carbs: 20 g"), ConfirmationLine(ConfirmationRole.NORMAL, "Duration: 3 h"))
        )

        val confirm = capturedConfirm { runBlocking { sut.handleECarbsPreCheck(EventData.ActionECarbsPreCheck(carbs = 20, carbsTimeShift = 15, duration = 3)) } }

        val captor = argumentCaptor<List<BatchAction>>()
        verifyBlocking(wizardBolusExecutor) { prepareBatch(captor.capture()) }
        val bolus = captor.firstValue.single() as BatchAction.Bolus
        assertThat(bolus.insulin).isEqualTo(0.0)
        assertThat(bolus.carbs).isEqualTo(20)
        assertThat(bolus.carbsTimeOffsetMinutes).isEqualTo(15)
        assertThat(bolus.carbsDurationHours).isEqualTo(3)

        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionECarbsConfirmed(99L))
        assertThat(confirm.lines).containsExactly(
            EventData.ConfirmActionLine("CARBS", "Carbs: 20 g"),
            EventData.ConfirmActionLine("NORMAL", "Duration: 3 h")
        ).inOrder()
    }

    // --- Temp target (fully unified onto the shared prepareBatch/confirm + applyTempTarget path) -----------

    @Test fun `manual temp target parks a TempTarget batch and ships its bolusId + master lines`() = runTest {
        whenever(profileFunction.getUnits()).thenReturn(GlucoseUnit.MGDL)
        stubPreview(0.0, 0, bolusId = 555L, lines = listOf(ConfirmationLine(ConfirmationRole.NORMAL, "Temporary target: 100 – 120 mg/dl (30 mins)")))

        val confirm = capturedConfirm {
            runBlocking {
                sut.handleTempTargetPreCheck(
                    EventData.ActionTempTargetPreCheck(EventData.ActionTempTargetPreCheck.TempTargetCommand.MANUAL, isMgdl = true, duration = 30, low = 100.0, high = 120.0)
                )
            }
        }

        // Driven through the shared batch path (mg/dL range, reason WEAR); the wear ✓ then confirms by bolusId
        // and the executor applies it via applyTempTarget — no bespoke doTempTarget.
        verifyBlocking(wizardBolusExecutor) { prepareBatch(listOf(BatchAction.TempTarget(TT.Reason.WEAR.text, 100.0, 120.0, 30, 0))) }
        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionTempTargetConfirmed(555L))
        assertThat(confirm.lines).containsExactly(EventData.ConfirmActionLine("NORMAL", "Temporary target: 100 – 120 mg/dl (30 mins)"))
    }

    @Test fun `cancel temp target parks a zero-duration TempTarget batch`() = runTest {
        whenever(profileFunction.getUnits()).thenReturn(GlucoseUnit.MGDL)
        stubPreview(0.0, 0, bolusId = 7L, lines = listOf(ConfirmationLine(ConfirmationRole.NORMAL, "Temporary target: Cancel")))

        val confirm = capturedConfirm {
            runBlocking { sut.handleTempTargetPreCheck(EventData.ActionTempTargetPreCheck(EventData.ActionTempTargetPreCheck.TempTargetCommand.CANCEL)) }
        }

        verifyBlocking(wizardBolusExecutor) { prepareBatch(listOf(BatchAction.TempTarget(TT.Reason.WEAR.text, 0.0, 0.0, 0, 0))) }
        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionTempTargetConfirmed(7L))
    }

    @Test fun `manual temp target with mismatched units sends an error, not a confirm`() = runTest {
        whenever(profileFunction.getUnits()).thenReturn(GlucoseUnit.MGDL) // profile mg/dL but the action says mmol

        val sent = capturedConfirm {
            runBlocking {
                sut.handleTempTargetPreCheck(
                    EventData.ActionTempTargetPreCheck(EventData.ActionTempTargetPreCheck.TempTargetCommand.MANUAL, isMgdl = false, duration = 30, low = 5.5, high = 6.5)
                )
            }
        }

        verifyBlocking(wizardBolusExecutor, never()) { prepareBatch(any()) }
        assertThat(sent.returnCommand).isInstanceOf(EventData.Error::class.java)
    }

    // --- Profile switch (unified onto the shared prepareBatch/confirm + applyProfileSwitch path) -----------

    @Test fun `profile switch precheck parks a ProfileSwitch batch and ships its bolusId + master lines`() = runTest {
        stubPreview(0.0, 0, bolusId = 321L, lines = listOf(ConfirmationLine(ConfirmationRole.PRIMARY, "Profile: Test")))

        val confirm = capturedConfirm {
            runBlocking { sut.handleProfileSwitchPreCheck(EventData.ActionProfileSwitchPreCheck(timeShift = 2, percentage = 120, duration = 60)) }
        }

        // The wear command maps to a single ProfileSwitch batch action; the executor validates/parks and the ✓ confirms by bolusId.
        verifyBlocking(wizardBolusExecutor) { prepareBatch(listOf(BatchAction.ProfileSwitch(120, 2, 60))) }
        assertThat(confirm.returnCommand).isEqualTo(EventData.ActionProfileSwitchConfirmed(321L))
        assertThat(confirm.lines).containsExactly(EventData.ConfirmActionLine("PRIMARY", "Profile: Test"))
    }

    @Test fun `profile switch precheck error from the executor becomes a sendError to the watch`() = runTest {
        runBlocking { whenever(wizardBolusExecutor.prepareBatch(any())).thenReturn(WizardBolusExecutor.PrepareResult.Error("no profile")) }

        val sent = capturedConfirm { runBlocking { sut.handleProfileSwitchPreCheck(EventData.ActionProfileSwitchPreCheck(timeShift = 0, percentage = 100, duration = 30)) } }

        assertThat(sent.returnCommand).isInstanceOf(EventData.Error::class.java)
        assertThat(sent.message).isEqualTo("no profile")
    }

    // --- Running mode (unified onto the shared negotiate → prepareBatch/confirm + applyRunningMode path) ---

    /** Negotiate the available modes and return the master-issued nonce + the wear-tile list (to pick an index). */
    private fun negotiateRunningModes(allowed: List<RM.Mode>): EventData.RunningModeList {
        whenever(pump.pumpDescription).thenReturn(PumpDescription())
        runBlocking {
            whenever(profileFunction.isProfileValid(any())).thenReturn(true)
            whenever(loop.allowedNextModes()).thenReturn(allowed)
        }
        var list: EventData.RunningModeList? = null
        val d = rxBus.toObservable(EventMobileToWear::class.java).subscribe { (it.payload as? EventData.RunningModeList)?.let { l -> list = l } }
        runBlocking { sut.handleAvailableRunningModes() }
        d.dispose()
        return list!!
    }

    @Test fun `running mode selected parks a RunningMode batch and ships its bolusId + master lines`() = runTest {
        val list = negotiateRunningModes(listOf(RM.Mode.CLOSED_LOOP, RM.Mode.OPEN_LOOP))
        val idx = list.states.indexOfFirst { it.state == AvailableRunningMode.RunningMode.LOOP_CLOSED }
        stubPreview(0.0, 0, bolusId = 555L, lines = listOf(ConfirmationLine(ConfirmationRole.PRIMARY, "Running mode: Closed Loop")))

        val confirm = capturedConfirm { runBlocking { sut.handleRunningModeSelected(EventData.RunningModeSelected(list.timeStamp, idx, null)) } }

        // The selected tile maps to a single RunningMode batch action; the executor re-validates/parks and the ✓ confirms by bolusId.
        verifyBlocking(wizardBolusExecutor) { prepareBatch(listOf(BatchAction.RunningMode(RM.Mode.CLOSED_LOOP, 0))) }
        assertThat(confirm.returnCommand).isEqualTo(EventData.RunningModeConfirmed(555L))
        assertThat(confirm.lines).containsExactly(EventData.ConfirmActionLine("PRIMARY", "Running mode: Closed Loop"))
    }

    @Test fun `running mode selected with a stale nonce is rejected before the executor`() = runTest {
        negotiateRunningModes(listOf(RM.Mode.CLOSED_LOOP))

        val sent = capturedConfirm { runBlocking { sut.handleRunningModeSelected(EventData.RunningModeSelected(timeStamp = 1L, index = 0, duration = null)) } }

        assertThat(sent.returnCommand).isInstanceOf(EventData.Error::class.java)
        verifyBlocking(wizardBolusExecutor, never()) { prepareBatch(any()) }
    }

    @Test fun `running mode confirmed commits the parked bolusId via the shared executor`() = runTest {
        // The post-confirm tile refresh short-circuits when no profile is valid (no pumpDescription stub needed).
        runBlocking {
            whenever(wizardBolusExecutor.confirm(any(), any(), any(), any())).thenReturn(WizardBolusExecutor.ConfirmResult.Delivered)
            whenever(profileFunction.isProfileValid(any())).thenReturn(false)
        }

        runBlocking { sut.handleRunningModeConfirmed(EventData.RunningModeConfirmed(777L)) }

        verifyBlocking(wizardBolusExecutor) { confirm(eq(777L), eq(Sources.Wear), any(), eq(false)) }
    }

    // --- Subscription wiring (the onEvent / onEventSync helpers actually register each type) --------------
    //
    // The tests above drive the handler methods directly. These two instead post the event onto the real
    // RxBus and assert the handler runs — locking in that the helper-based subscriptions in init {} are
    // wired (a dropped subscription is a mechanical refactor error the compiler can't catch).

    @Test fun `onEventSync dispatches a posted SnoozeAlert to its handler`() {
        // onEventSync subscribes directly; the trampoline io scheduler runs it inline.
        rxBus.send(EventData.SnoozeAlert(0L))

        verify(uiInteraction).stopAlarm("Muted from wear")
    }

    @Test fun `onEvent dispatches a posted ActionBolusPreCheck to the suspend handler`() {
        // onEvent wraps the handler in rxCompletable, which runs the coroutine off the posting thread —
        // hence a timeout verify rather than a synchronous capture.
        stubPreview(1.0, 10, bolusId = 11L, lines = listOf(ConfirmationLine(ConfirmationRole.BOLUS, "Bolus: 1.00 U")))

        rxBus.send(EventData.ActionBolusPreCheck(insulin = 1.0, carbs = 10))

        verifyBlocking(wizardBolusExecutor, timeout(2000)) { prepareBatch(any()) }
    }

    @Test fun `onEvent dispatches a posted ActionProfileSwitchPreCheck to the handler`() {
        stubPreview(0.0, 0, bolusId = 5L, lines = emptyList())

        rxBus.send(EventData.ActionProfileSwitchPreCheck(timeShift = 0, percentage = 110, duration = 30))

        verifyBlocking(wizardBolusExecutor, timeout(2000)) { prepareBatch(any()) }
    }
}
