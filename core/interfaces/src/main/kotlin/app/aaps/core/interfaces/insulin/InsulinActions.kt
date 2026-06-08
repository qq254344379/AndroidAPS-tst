package app.aaps.core.interfaces.insulin

import app.aaps.core.data.model.ICfg
import app.aaps.core.interfaces.clientcontrol.ActionProgress

/**
 * Per-domain execution facade for "activate this insulin", hiding the master/client split from the VM
 * (mirrors [app.aaps.core.interfaces.scenes.SceneActions]):
 *  - **master** re-applies its current profile with [iCfg] locally and returns the terminal outcome;
 *  - **client** goes through the confirmed client→master round-trip, which drives the single app-level
 *    pending modal and returns the master's terminal [ActionProgress].
 */
interface InsulinActions {

    /** Activate [iCfg] as the current insulin. Returns the terminal [ActionProgress]. */
    suspend fun activate(iCfg: ICfg): ActionProgress
}
