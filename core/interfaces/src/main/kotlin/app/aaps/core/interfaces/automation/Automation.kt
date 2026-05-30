package app.aaps.core.interfaces.automation

import app.aaps.core.interfaces.configuration.ConfigExportImport
import kotlinx.coroutines.flow.StateFlow

interface Automation : ConfigExportImport {

    /**
     * Live snapshot of the automation event list. Emits on user edits, ordering changes, and
     * NS-synced reloads. Replaces the prior `EventAutomationDataChanged` RxBus broadcast.
     */
    val events: StateFlow<List<AutomationEvent>>

    fun findEventById(id: String): AutomationEvent?
    suspend fun processEvent(someEvent: AutomationEvent)

    /**
     * Generate reminder via [app.aaps.plugins.automation.TimerUtil]
     *
     */
    fun scheduleAutomationEventBolusReminder()

    /**
     * Remove scheduled reminder from automations
     *
     */
    fun removeAutomationEventBolusReminder()

    /**
     * Generate reminder via [app.aaps.plugins.automation.TimerUtil]
     *
     * @param seconds seconds to the future
     */
    fun scheduleTimeToEatReminder(seconds: Int)

    /**
     * Remove Automation event
     */
    fun removeAutomationEventEatReminder()

    /**
     * Create new Automation event to alarm when is time to eat
     */
    fun scheduleAutomationEventEatReminder()
}