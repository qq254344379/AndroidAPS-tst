package app.aaps.core.interfaces.scenes

/**
 * Outbound channel for scene-control commands sent from a paired AAPSClient device to its master.
 *
 * Implemented in `:plugins:sync` (where the wire format and signing live) and consumed from `:ui`
 * so the UI module doesn't need a project dependency on the sync module — the cost of those
 * inter-module dependencies is high enough that CLAUDE.md flags them as worth avoiding.
 *
 * On master devices (config.AAPSCLIENT == false) `MainViewModel.executeConfirmableAction`
 * never reaches this — the local SceneExecutor path runs instead. The Hilt binding may still
 * exist; calling it on an unpaired device returns [ClientControlSendResult.NotPaired].
 */
interface ClientControlSceneSender {

    /**
     * Send a scene.start command to the master.
     * `durationMinutes = null` means use the scene's stored default on master.
     */
    suspend fun sendSceneStart(sceneId: String, durationMinutes: Int?): ClientControlSendResult

    /**
     * Send a scene.stop command to the master.
     * `triggerChain = true` mirrors master's "Skip to <ChainTarget>" — master re-checks
     * canChain at receipt time and activates the active scene's chain target if all conditions
     * are met. `false` is a plain stop.
     */
    suspend fun sendSceneStop(triggerChain: Boolean): ClientControlSendResult

    /**
     * Push the client's full scenes JSON to the master after a local scene edit. Master merges
     * per-scene by `lastModified` (last-writer-wins) and republishes via the running-config doc,
     * so other paired clients converge. `scenesJson` is the same array shape the local pref holds.
     */
    suspend fun sendScenesUpdate(scenesJson: String): ClientControlSendResult
}
