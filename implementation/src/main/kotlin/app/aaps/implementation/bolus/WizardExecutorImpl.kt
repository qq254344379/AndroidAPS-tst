package app.aaps.implementation.bolus

import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.bolus.WizardExecutor
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.sync.NsClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [WizardExecutor] implementation — the role branch for the recompute bolus path, structurally identical to
 * [BatchExecutorImpl]. Client → the signed two-step `BolusPrepare`/`WizardPrepare` + `BolusCommit` round-trip
 * (gated on `masterReachable` per the offline-block decision); master → the local executor
 * (`prepareQuickWizard`/`prepareWizard` + `confirm`). The compute/deliver logic lives once in the executor; a
 * client's request lands in that same executor on the master, so both roles render the master's identical lines.
 */
@Singleton
class WizardExecutorImpl @Inject constructor(
    private val dispatcher: ClientControlActionDispatcher,
    private val wizardBolusExecutor: WizardBolusExecutor,
    private val nsClient: NsClient,
    private val config: Config
) : WizardExecutor {

    override suspend fun prepare(source: WizardExecutor.WizardSource, label: String): ActionProgress {
        if (config.AAPSCLIENT) {
            // Offline → block before the round-trip (pure SSOT — the dose is computed + delivered on the master).
            if (!nsClient.masterReachable.value) return ActionProgress.Rejected(FailureReason.NotReachable)
            val command = when (source) {
                is WizardExecutor.WizardSource.QuickWizard -> ClientControlActionDispatcher.Command.BolusPrepare(source.guid)
                is WizardExecutor.WizardSource.Manual      -> ClientControlActionDispatcher.Command.WizardPrepare(source.inputs)
            }
            return dispatcher.run(command, label)
        }
        // Master: compute + cap + park locally — NO app-level modal (the caller renders the returned lines).
        val result = when (source) {
            is WizardExecutor.WizardSource.QuickWizard -> wizardBolusExecutor.prepareQuickWizard(source.guid)
            is WizardExecutor.WizardSource.Manual      -> wizardBolusExecutor.prepareWizard(source.inputs)
        }
        return when (result) {
            is WizardBolusExecutor.PrepareResult.Preview -> ActionProgress.Prepared(result.bolusId, result.lines, result.advisorApplies, result.advisorLines)
            is WizardBolusExecutor.PrepareResult.Error   -> ActionProgress.Rejected(FailureReason.ExecutionFailed, result.message)
        }
    }

    override suspend fun commit(bolusId: Long, asAdvisor: Boolean, source: Sources, label: String): ActionProgress {
        if (config.AAPSCLIENT) {
            if (!nsClient.masterReachable.value) return ActionProgress.Rejected(FailureReason.NotReachable)
            return dispatcher.run(ClientControlActionDispatcher.Command.BolusCommit(bolusId, asAdvisor), label)
        }
        // Master: deliver the parked dose locally. The async delivery failure is alarmed centrally by the executor,
        // so a swallowed onError here doesn't double-report (mirrors BatchExecutorImpl).
        var error: String? = null
        val result = wizardBolusExecutor.confirm(bolusId, source, { error = it }, asAdvisor)
        val err = error
        return when {
            result is WizardBolusExecutor.ConfirmResult.NoPending -> ActionProgress.Rejected(FailureReason.NoPendingBolus)
            err != null                                           -> ActionProgress.Rejected(FailureReason.ExecutionFailed, err)
            else                                                  -> ActionProgress.Applied
        }
    }
}
