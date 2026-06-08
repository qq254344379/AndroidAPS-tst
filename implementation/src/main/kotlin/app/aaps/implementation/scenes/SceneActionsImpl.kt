package app.aaps.implementation.scenes

import app.aaps.core.data.model.Scene
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.clientcontrol.FailureReason
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.scenes.SceneActions
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import app.aaps.core.ui.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SceneActions] implementation. start/stop go through [ClientControlActionDispatcher.execute], which
 * owns the role-branch (client = round-trip, master = the local [SceneAutomationApi] block) AND the
 * failure dialog. [validateActivation] is a pure query, untouched.
 */
@Singleton
class SceneActionsImpl @Inject constructor(
    private val dispatcher: ClientControlActionDispatcher,
    private val sceneApi: SceneAutomationApi,
    private val sceneExecutor: SceneExecutor,
    private val rh: ResourceHelper
) : SceneActions {

    override suspend fun validateActivation(scene: Scene): String? = sceneExecutor.validateActivation(scene)

    override suspend fun start(sceneId: String, durationMinutes: Int?): ActionProgress =
        dispatcher.execute(
            ClientControlActionDispatcher.Command.SceneStart(sceneId, durationMinutes),
            rh.gs(R.string.clientcontrol_action_activate_scene, sceneApi.getScene(sceneId)?.name ?: "")
        ) { sceneApi.runScene(sceneId, durationMinutes).toProgress() }

    override suspend fun stop(triggerChain: Boolean): ActionProgress =
        dispatcher.execute(
            ClientControlActionDispatcher.Command.SceneStop(triggerChain),
            rh.gs(R.string.clientcontrol_action_deactivate_scene)
        ) { (if (triggerChain) sceneApi.stopActiveSceneAndChain() else sceneApi.stopActiveScene()).toProgress() }

    // Master-side mapping only (the client path's terminal comes from the master's ACK).
    private fun SceneAutomationResult.toProgress(): ActionProgress = when (this) {
        SceneAutomationResult.Success,
        is SceneAutomationResult.ChainCompleted -> ActionProgress.Applied
        SceneAutomationResult.SceneNotFound     -> ActionProgress.Rejected(FailureReason.SceneNotFound)
        SceneAutomationResult.SceneDisabled     -> ActionProgress.Rejected(FailureReason.SceneDisabled)
        is SceneAutomationResult.Failed         -> ActionProgress.Rejected(FailureReason.ExecutionFailed, message)
    }
}
