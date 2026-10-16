package app.aaps.pump.lenomed.di

import android.content.Context
import app.aaps.core.interfaces.constraints.ConstraintsChecker
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.pump.DetailedBolusInfoStorage
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.TemporaryBasalStorage
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.interfaces.di.PumpDriver
import app.aaps.pump.lenomed.LenomedPlugin
import app.aaps.pump.lenomed.LenomedPump
import app.aaps.pump.lenomed.database.LenomedHistoryDatabase
import app.aaps.pump.lenomed.database.LenomedHistoryDatabases
import javax.inject.Provider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntKey
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * DI for the ported Lenomed pump driver.
 *
 * `LenomedPlugin` is still Java (it came out of the APK decompilation that way), and Metro generates a
 * factory from an `@Inject` constructor only when it has Kotlin IR to read. So we build the instance by
 * hand here, the same way `ErosJavaBindings` owns the still-Java Omnipod Eros managers. The scope makes
 * Metro the owner, so the plugin is a single instance shared by every graph that asks for it.
 *
 * The plugin is filed **only** into the `@PumpDriver`-qualified int map (key 1200). The class also carries
 * `@PumpDriver` and `@IntKey(1200)` so `PumpDriverBucketTest` finds it on the classpath and verifies the
 * bucket holds exactly the drivers compiled into this build. It must not also land in the unqualified
 * `contributedPlugins` bucket, or it would appear in follower builds that have no pump at all.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object LenomedPluginBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideLenomedPump(
        aapsLogger: AAPSLogger,
        dateUtil: DateUtil,
        rh: ResourceHelper,
        decimalFormatter: DecimalFormatter
    ): LenomedPump = LenomedPump(aapsLogger, dateUtil, rh, decimalFormatter)

    @Provides
    @SingleIn(AppScope::class)
    fun provideLenomedHistoryDatabase(context: Context): LenomedHistoryDatabase =
        LenomedHistoryDatabases.get(context).also { LenomedHistoryDatabases.install(it) }

    @Provides
    @SingleIn(AppScope::class)
    fun provideLenomedPlugin(
        aapsLogger: AAPSLogger,
        rxBus: RxBus,
        context: Context,
        rh: ResourceHelper,
        preferences: Preferences,
        constraintChecker: ConstraintsChecker,
        profileFunction: ProfileFunction,
        sp: SP,
        commandQueue: CommandQueue,
        lenomedPump: app.aaps.pump.lenomed.LenomedPump,
        pumpSync: PumpSync,
        detailedBolusInfoStorage: DetailedBolusInfoStorage,
        temporaryBasalStorage: TemporaryBasalStorage,
        fabricPrivacy: FabricPrivacy,
        dateUtil: DateUtil,
        aapsSchedulers: AapsSchedulers,
        uiInteraction: UiInteraction,
        diaconnHistoryDatabase: LenomedHistoryDatabase,
        decimalFormatter: DecimalFormatter,
        notificationManager: NotificationManager
    ): LenomedPlugin {
        // The driver asks this for a fresh PumpEnactResult per operation. It is deliberately not a
        // @Provides parameter: Metro cannot code-generate a factory parameter of type
        // `Provider<PumpEnactResult>` (it fails with "unexpected error while processing type"), so the
        // provider is built here and only its result type reaches the graph. Every call returns a new
        // instance, which is what the driver expects - it fills one in with setters and hands it back.
        val pumpEnactResultProvider = Provider<PumpEnactResult> { LenomedPumpEnactResult(rh) }
        return LenomedPlugin(
            aapsLogger, rxBus, context, rh, preferences, constraintChecker, profileFunction,
            sp, commandQueue, lenomedPump, pumpSync, detailedBolusInfoStorage,
            temporaryBasalStorage, fabricPrivacy, dateUtil, aapsSchedulers, uiInteraction,
            diaconnHistoryDatabase, decimalFormatter, pumpEnactResultProvider, notificationManager
        )
    }

    @Provides
    @IntoMap
    @IntKey(1200)
    @PumpDriver
    fun lenomedPluginEntry(plugin: LenomedPlugin): PluginBase = plugin
}
