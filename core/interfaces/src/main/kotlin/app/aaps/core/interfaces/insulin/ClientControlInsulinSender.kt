package app.aaps.core.interfaces.insulin

import app.aaps.core.interfaces.scenes.ClientControlSendResult

/**
 * Outbound channel for a paired AAPSCLIENT to ask its master to **activate** an insulin. (Insulin
 * *config* now syncs via the generic `SyncSpec(Bidirectional)` channel — this remains only for the
 * activate *action*, which can't be a key sync.) Implemented in `:plugins:sync` (where the wire format
 * + signing live), so neither the implementation nor the sync module needs a project dependency on the
 * other. Mirrors [app.aaps.core.interfaces.scenes.ClientControlSceneSender].
 */
interface ClientControlInsulinSender {

    /**
     * Ask the master to activate the insulin described by [iCfgJson] (the JSON shape `ICfg` serializes
     * to) by creating a profile switch over the master's current profile. The master is authoritative
     * for the profile; only the insulin config is sent.
     */
    suspend fun sendInsulinActivate(iCfgJson: String): ClientControlSendResult
}
