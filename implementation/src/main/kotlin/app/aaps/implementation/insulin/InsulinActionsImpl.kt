package app.aaps.implementation.insulin

import app.aaps.core.data.model.ICfg
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.insulin.InsulinActions
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.objects.extensions.toJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [InsulinActions] implementation. Client → confirmed round-trip ([ClientControlActionDispatcher.run],
 * which drives the single app-level modal); master → local profile switch over the active profile.
 */
@Singleton
class InsulinActionsImpl @Inject constructor(
    private val config: Config,
    private val dispatcher: ClientControlActionDispatcher,
    private val profileFunction: ProfileFunction
) : InsulinActions {

    override suspend fun activate(iCfg: ICfg): ActionProgress =
        if (config.AAPSCLIENT)
            // Follower: send only the iCfg; the master re-applies its OWN current profile with it and the
            // resulting profile switch syncs back. The round-trip drives the modal + returns the ACK.
            dispatcher.run(ClientControlActionDispatcher.Command.InsulinActivate(iCfg.toJsonObject().toString()))
        else
            if (profileFunction.createProfileSwitchWithNewInsulin(iCfg, Sources.Insulin)) ActionProgress.Applied
            else ActionProgress.Rejected(null)
}
