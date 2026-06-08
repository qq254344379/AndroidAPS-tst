package app.aaps.implementation.scenes

import app.aaps.core.data.model.Scene
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.core.interfaces.clientcontrol.ClientControlActionDispatcher
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.scenes.SceneActions
import app.aaps.core.interfaces.scenes.SceneAutomationApi
import app.aaps.core.interfaces.scenes.SceneAutomationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SceneActions] implementation. Client → confirmed round-trip ([ClientControlActionDispatcher.run],
 * which drives the single app-level modal); master → local execution via [SceneAutomationApi].
 *
 * Stage A: lives in `:ui` next to the executor; Stage C moves it to `:implementation` (it already
 * depends only on core interfaces, so the move is a package change).
 */
@Singleton
class SceneActionsImpl @Inject constructor(
    private val config: Config,
    private val dispatcher: ClientControlActionDispatcher,
    private val sceneApi: SceneAutomationApi,
    private val sceneExecutor: SceneExecutor
) : SceneActions {

    override suspend fun validateActivation(scene: Scene): String? = sceneExecutor.validateActivation(scene)

    override suspend fun start(sceneId: String, durationMinutes: Int?): ActionProgress =
        if (config.AAPSCLIENT) dispatcher.run(ClientControlActionDispatcher.Command.SceneStart(sceneId, durationMinutes))
        else sceneApi.runScene(sceneId, durationMinutes).toProgress()

    override suspend fun stop(triggerChain: Boolean): ActionProgress =
        if (config.AAPSCLIENT) dispatcher.run(ClientControlActionDispatcher.Command.SceneStop(triggerChain))
        else (if (triggerChain) sceneApi.stopActiveSceneAndChain() else sceneApi.stopActiveScene()).toProgress()

    // Master-side mapping only (the client path's terminal comes from the master's ACK via run()).
    private fun SceneAutomationResult.toProgress(): ActionProgress = when (this) {
        SceneAutomationResult.Success,
        is SceneAutomationResult.ChainCompleted -> ActionProgress.Applied
        SceneAutomationResult.SceneNotFound     -> ActionProgress.Rejected("scene not found")
        SceneAutomationResult.SceneDisabled     -> ActionProgress.Rejected("scene disabled")
        is SceneAutomationResult.Failed         -> ActionProgress.Rejected(message)
    }
}
