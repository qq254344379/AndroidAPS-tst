package app.aaps.implementation.tempTargets

import app.aaps.core.data.model.TT
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.data.ue.ValueWithUnit
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.tempTargets.TempTargetActions
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.ui.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TempTargetActions] implementation. Both methods go through [ClientControlActionDispatcher.execute],
 * which owns the role-branch (client = round-trip, master = the local block) AND surfaces any failure on
 * the single app-level modal. So this just maps domain → command + provides the master-local block.
 */
@Singleton
class TempTargetActionsImpl @Inject constructor(
    private val dispatcher: ClientControlActionDispatcher,
    private val persistenceLayer: PersistenceLayer,
    private val dateUtil: DateUtil,
    private val rh: ResourceHelper,
    private val aapsLogger: AAPSLogger
) : TempTargetActions {

    override suspend fun set(
        reason: TT.Reason,
        lowMgdl: Double,
        highMgdl: Double,
        durationMinutes: Int,
        timestamp: Long,
        source: Sources,
        note: String?
    ): ActionProgress =
        dispatcher.execute(
            ClientControlActionDispatcher.Command.TempTargetSet(timestamp, lowMgdl, highMgdl, durationMinutes, reason.text),
            rh.gs(R.string.clientcontrol_action_set_temp_target)
        ) {
            runCatching {
                persistenceLayer.insertAndCancelCurrentTemporaryTarget(
                    temporaryTarget = TT(timestamp = timestamp, reason = reason, lowTarget = lowMgdl, highTarget = highMgdl, duration = durationMinutes * 60_000L),
                    action = Action.TT,
                    source = source,
                    note = note,
                    listValues = listOf(ValueWithUnit.Mgdl(lowMgdl), ValueWithUnit.Minute(durationMinutes))
                )
            }.toProgress("temp target set")
        }

    override suspend fun cancel(source: Sources, note: String?): ActionProgress =
        dispatcher.execute(
            ClientControlActionDispatcher.Command.TempTargetCancel,
            rh.gs(R.string.clientcontrol_action_cancel_temp_target)
        ) {
            runCatching {
                persistenceLayer.cancelCurrentTemporaryTargetIfAny(dateUtil.now(), Action.CANCEL_TT, source, note, emptyList())
            }.toProgress("temp target cancel")
        }

    private fun <T> Result<T>.toProgress(what: String): ActionProgress =
        fold(
            onSuccess = { ActionProgress.Applied },
            onFailure = { aapsLogger.error(LTag.UI, "$what failed", it); ActionProgress.Rejected(FailureReason.ExecutionFailed, it.message) }
        )
}
