package app.aaps.core.interfaces.insulin

import app.aaps.core.interfaces.scenes.ClientControlSendResult

/**
 * Outbound channel for pushing insulin-definition edits from a paired AAPSCLIENT device to its
 * master. Implemented in `:plugins:sync` (where the wire format + signing live) and consumed by the
 * insulin client publisher, so neither the implementation nor the sync module needs a project
 * dependency on the other. Mirrors [app.aaps.core.interfaces.scenes.ClientControlSceneSender].
 */
interface ClientControlInsulinSender {

    /**
     * Push the client's full insulin-configuration JSON plus its local edit [version] to the master.
     * The master applies whole-list last-writer-wins by [version] — a strictly-newer config replaces,
     * a stale one is dropped — then republishes via the running-config doc so devices converge.
     */
    suspend fun sendInsulinUpdate(insulinJson: String, version: Long): ClientControlSendResult

    /**
     * Ask the master to activate the insulin described by [iCfgJson] (the JSON shape `ICfg` serializes
     * to) by creating a profile switch over the master's current profile. The master is authoritative
     * for the profile; only the insulin config is sent.
     */
    suspend fun sendInsulinActivate(iCfgJson: String): ClientControlSendResult
}
