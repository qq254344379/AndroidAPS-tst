package app.aaps.implementation.insulin

import app.aaps.core.data.model.ICfg
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.insulin.InsulinActions
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.objects.extensions.toJsonObject
import app.aaps.core.ui.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [InsulinActions] implementation. Goes through [ClientControlActionDispatcher.execute], which owns the
 * role-branch and the failure dialog; this just maps domain → command + the master-local block.
 */
@Singleton
class InsulinActionsImpl @Inject constructor(
    private val dispatcher: ClientControlActionDispatcher,
    private val profileFunction: ProfileFunction,
    private val rh: ResourceHelper
) : InsulinActions {

    override suspend fun activate(iCfg: ICfg): ActionProgress =
        dispatcher.execute(
            ClientControlActionDispatcher.Command.InsulinActivate(iCfg.toJsonObject().toString()),
            rh.gs(R.string.clientcontrol_action_activate_insulin, iCfg.insulinLabel)
        ) {
            // Master: re-apply the active profile with this insulin. No active profile → can't apply.
            if (profileFunction.createProfileSwitchWithNewInsulin(iCfg, Sources.Insulin)) ActionProgress.Applied
            else ActionProgress.Rejected(FailureReason.NoActiveProfile)
        }
}
