package app.aaps.implementation.bolus

import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.bolus.BatchAction
import app.aaps.core.interfaces.bolus.BatchExecutor
import app.aaps.core.interfaces.bolus.WizardBolusExecutor
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.sync.NsClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [BatchExecutor] implementation — the role branch. Client → the signed two-step `BatchPrepare`/`BolusCommit`
 * round-trip (prepare gated on `masterReachable` per the offline-block decision); master → the local executor
 * (`prepareBatch`/`confirm`). The prepare/deliver logic lives once in the executor; a client's batch lands in
 * that same executor on the master, so both roles render the master's identical confirmation.
 */
@Singleton
class BatchExecutorImpl @Inject constructor(
    private val dispatcher: ClientControlActionDispatcher,
    private val wizardBolusExecutor: WizardBolusExecutor,
    private val nsClient: NsClient,
    private val config: Config
) : BatchExecutor {

    override suspend fun prepare(actions: List<BatchAction>, source: Sources, label: String): ActionProgress {
        if (config.AAPSCLIENT) {
            // Offline → block before the round-trip (pure SSOT; a batch fuses a needs-master bolus with a TT).
            if (!nsClient.masterReachable.value) return ActionProgress.Rejected(FailureReason.NotReachable)
            return dispatcher.run(ClientControlActionDispatcher.Command.BatchPrepare(actions), label)
        }
        // Master: prepare locally — NO app-level modal (the dialog renders the returned lines as the confirmation).
        return when (val r = wizardBolusExecutor.prepareBatch(actions)) {
            is WizardBolusExecutor.PrepareResult.Preview -> ActionProgress.Prepared(r.bolusId, r.lines, r.advisorApplies, r.advisorLines)
            is WizardBolusExecutor.PrepareResult.Error   -> ActionProgress.Rejected(FailureReason.ExecutionFailed, r.message)
        }
    }

    override suspend fun commit(bolusId: Long, source: Sources, label: String): ActionProgress {
        if (config.AAPSCLIENT) {
            if (!nsClient.masterReachable.value) return ActionProgress.Rejected(FailureReason.NotReachable)
            return dispatcher.run(ClientControlActionDispatcher.Command.BolusCommit(bolusId), label)
        }
        // Master: deliver the parked bundle locally (decision-B order lives in the executor's confirm()).
        var error: String? = null
        val result = wizardBolusExecutor.confirm(bolusId, source, { error = it })
        val err = error
        return when {
            result is WizardBolusExecutor.ConfirmResult.NoPending -> ActionProgress.Rejected(FailureReason.NoPendingBolus)
            err != null                                           -> ActionProgress.Rejected(FailureReason.ExecutionFailed, err)
            else                                                  -> ActionProgress.Applied
        }
    }
}
