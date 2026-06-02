package app.aaps.core.interfaces.configuration

import app.aaps.core.interfaces.scenes.ClientControlSendResult

/**
 * Outbound channel for pushing locally-edited, bidirectionally-synced preference values from a paired
 * AAPSCLIENT device to its master. The generic analogue of the per-domain senders
 * ([app.aaps.core.interfaces.insulin.ClientControlInsulinSender] etc.) — one call carries every
 * changed plain preference key, so adding a new synced setting needs no new sender.
 *
 * Implemented in `:plugins:sync` (where the wire format + signing live). Values are passed as
 * primitives (`key → (serialized value, lastModified)`) so this interface stays free of any nssdk
 * dependency.
 */
interface ClientControlPreferencesSender {

    /**
     * Push the given synced preference changes to the master. Map key = the preference's key string;
     * value = (the value serialized to a string, its local edit timestamp). The master applies
     * per-key last-writer-wins by the timestamp and republishes.
     */
    suspend fun sendPreferencesUpdate(changes: Map<String, Pair<String, Long>>): ClientControlSendResult
}
