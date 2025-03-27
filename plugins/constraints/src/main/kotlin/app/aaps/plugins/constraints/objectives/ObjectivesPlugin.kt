package app.aaps.plugins.constraints.objectives

import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.constraints.Constraint
import app.aaps.core.interfaces.constraints.Objectives
import app.aaps.core.interfaces.constraints.Objectives.Companion.AUTOSENS_OBJECTIVE
import app.aaps.core.interfaces.constraints.Objectives.Companion.AUTO_OBJECTIVE
import app.aaps.core.interfaces.constraints.Objectives.Companion.FIRST_OBJECTIVE
import app.aaps.core.interfaces.constraints.Objectives.Companion.MAXBASAL_OBJECTIVE
import app.aaps.core.interfaces.constraints.Objectives.Companion.MAXIOB_ZERO_CL_OBJECTIVE
import app.aaps.core.interfaces.constraints.Objectives.Companion.SMB_OBJECTIVE
import app.aaps.core.interfaces.constraints.PluginConstraints
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.plugin.PluginBaseWithPreferences
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.BooleanNonKey
import app.aaps.core.keys.IntNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.plugins.constraints.R
import app.aaps.plugins.constraints.objectives.keys.ObjectivesBooleanComposedKey
import app.aaps.plugins.constraints.objectives.keys.ObjectivesLongComposedKey
import app.aaps.plugins.constraints.objectives.objectives.Objective
import app.aaps.plugins.constraints.objectives.objectives.Objective0
import app.aaps.plugins.constraints.objectives.objectives.Objective1
import app.aaps.plugins.constraints.objectives.objectives.Objective10
import app.aaps.plugins.constraints.objectives.objectives.Objective2
import app.aaps.plugins.constraints.objectives.objectives.Objective3
import app.aaps.plugins.constraints.objectives.objectives.Objective4
import app.aaps.plugins.constraints.objectives.objectives.Objective5
import app.aaps.plugins.constraints.objectives.objectives.Objective6
import app.aaps.plugins.constraints.objectives.objectives.Objective7
import app.aaps.plugins.constraints.objectives.objectives.Objective9
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectivesPlugin @Inject constructor(
    private val injector: HasAndroidInjector,
    aapsLogger: AAPSLogger,
    rh: ResourceHelper,
    preferences: Preferences
) : PluginBaseWithPreferences(
    pluginDescription = PluginDescription()
        .mainType(PluginType.CONSTRAINTS)
        .fragmentClass(ObjectivesFragment::class.qualifiedName)
        .pluginIcon(app.aaps.core.ui.R.drawable.ic_graduation)
        .pluginName(app.aaps.core.ui.R.string.objectives)
        .shortName(R.string.objectives_shortname)
        .description(R.string.description_objectives),
    ownPreferences = listOf(ObjectivesBooleanComposedKey::class.java, ObjectivesLongComposedKey::class.java),
    aapsLogger, rh, preferences
), PluginConstraints, Objectives {

    var objectives: MutableList<Objective> = ArrayList()

    init {
        setupObjectives()
    }

    private fun setupObjectives() {
        objectives.clear()
        objectives.add(Objective0(injector))
        objectives.add(Objective1(injector))
        objectives.add(Objective2(injector))
        objectives.add(Objective3(injector))
        objectives.add(Objective4(injector))
        objectives.add(Objective5(injector))
        objectives.add(Objective6(injector))
        objectives.add(Objective7(injector))
        objectives.add(Objective9(injector))
        objectives.add(Objective10(injector))
        // edit companion object if you remove/add Objective
    }

    fun reset() {
        for (objective in objectives) {
            objective.startedOn = 0
            objective.accomplishedOn = 0
        }
        preferences.put(BooleanNonKey.ObjectivesBgIsAvailableInNs, false)
        preferences.put(BooleanNonKey.ObjectivesPumpStatusIsAvailableInNS, false)
        preferences.put(IntNonKey.ObjectivesManualEnacts, 0)
        preferences.put(BooleanNonKey.ObjectivesProfileSwitchUsed, false)
        preferences.put(BooleanNonKey.ObjectivesDisconnectUsed, false)
        preferences.put(BooleanNonKey.ObjectivesReconnectUsed, false)
        preferences.put(BooleanNonKey.ObjectivesTempTargetUsed, false)
        preferences.put(BooleanNonKey.ObjectivesActionsUsed, false)
        preferences.put(BooleanNonKey.ObjectivesLoopUsed, false)
        preferences.put(BooleanNonKey.ObjectivesScaleUsed, false)
    }

    fun allPriorAccomplished(position: Int): Boolean {
        var accomplished = true
        for (i in 0 until position) {
            accomplished = accomplished && objectives[i].isAccomplished
        }
        return accomplished
    }

    /**
     * Constraints interface
     */
    override fun isLoopInvocationAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[FIRST_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, FIRST_OBJECTIVE + 1), this)
        return value
    }

    override fun isLgsAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[MAXBASAL_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, MAXBASAL_OBJECTIVE + 1), this)
        return value
    }

    override fun isClosedLoopAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[MAXIOB_ZERO_CL_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, MAXIOB_ZERO_CL_OBJECTIVE + 1), this)
        return value
    }

    override fun isAutosensModeEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[AUTOSENS_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, AUTOSENS_OBJECTIVE + 1), this)
        return value
    }

    override fun isSMBModeEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[SMB_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, SMB_OBJECTIVE + 1), this)
        return value
    }

    override fun applyMaxIOBConstraints(maxIob: Constraint<Double>): Constraint<Double> {
        if (objectives[MAXIOB_ZERO_CL_OBJECTIVE].isStarted && !objectives[MAXIOB_ZERO_CL_OBJECTIVE].isAccomplished)
            maxIob.set(0.0, rh.gs(R.string.objectivenotfinished, MAXIOB_ZERO_CL_OBJECTIVE + 1), this)
        return maxIob
    }

    override fun isAutomationEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        if (!objectives[AUTO_OBJECTIVE].isStarted)
            value.set(false, rh.gs(R.string.objectivenotstarted, AUTO_OBJECTIVE + 1), this)
        return value
    }

    override fun isAccomplished(index: Int) = objectives[index].isAccomplished
    override fun isStarted(index: Int): Boolean = objectives[index].isStarted
}
