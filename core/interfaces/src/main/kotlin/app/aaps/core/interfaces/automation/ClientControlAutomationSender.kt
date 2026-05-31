package app.aaps.core.interfaces.automation

import app.aaps.core.interfaces.scenes.ClientControlSendResult

/**
 * Outbound channel for pushing automation-definition edits from a paired AAPSCLIENT device to its
 * master. Implemented in `:plugins:sync` (where the wire format + signing live) and consumed by the
 * automation client publisher, so neither the automation nor the sync module needs a project
 * dependency on the other. Mirrors [app.aaps.core.interfaces.scenes.ClientControlSceneSender].
 */
interface ClientControlAutomationSender {

    /**
     * Push the client's full automation-events JSON plus its local edit [version] to the master.
     * The master applies whole-list last-writer-wins by [version] — a strictly-newer list replaces,
     * a stale one is dropped — then republishes via the running-config doc so devices converge.
     */
    suspend fun sendAutomationUpdate(automationJson: String, version: Long): ClientControlSendResult
}
