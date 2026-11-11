package app.aaps.pump.lenomed

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.profile.EffectiveProfile
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.rx.events.Event
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.collectResilient
import app.aaps.core.keys.interfaces.TextRef
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * Bridge helpers for the ported Lenomed (Embecta) driver.
 *
 * The driver was decompiled from a build that still used RxJava (`RxBus.toObservable(...)`
 * + `CompositeDisposable`). The dev branch replaced that with a `Flow`-based `RxBus`
 * (`RxBus.toFlow(KClass)`), which cannot be subscribed to from Java without fighting
 * suspend-lambda erasure. These helpers keep the Java side of the driver readable.
 */

/** IO scope for the plugin's long-lived event collectors, mirroring the old `Schedulers.io()`. */
fun lenomedIoScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

/**
 * Replaces the JADX decompile artifact `ArraysKt.joinToString$default(data, sep, ..., lambda)`.
 * The original Kotlin was `data.joinToString(sep) { String.format("%02X", it) }` - an uppercase
 * hex dump used in debug logs and (for the time/serial packets) to parse values back out.
 */
fun bytesToHexString(data: ByteArray, separator: String): String =
    data.joinToString(separator) { String.format("%02X", it) }

/**
 * Subscribes to [eventClass] on [rxBus] and runs [action] for every emission.
 *
 * Uses `CoroutineStart.UNDISPATCHED` because `RxBus` has no replay: a scheduled collector
 * would miss anything sent between this call and the coroutine actually starting.
 * The collector is resilient - a failure while handling one event is logged and does not
 * cancel the subscription.
 */
fun <T : Event> subscribeEvent(
    scope: CoroutineScope,
    rxBus: RxBus,
    aapsLogger: AAPSLogger,
    eventClass: KClass<T>,
    action: (T) -> Unit
) = rxBus.toFlow(eventClass)
    .collectResilient(scope, aapsLogger, LTag.PUMP, start = CoroutineStart.UNDISPATCHED) { action(it) }

/**
 * `CommandQueue.readStatus()` is `suspend` on dev. The decompiled driver called it from plain
 * methods (and once with a `null` Continuation, which crashes at runtime). This wraps it in a
 * short-lived IO scope so those call sites keep working; returns the [Job] so a caller that
 * has a longer-lived scope can cancel it.
 */
@JvmOverloads
fun readStatusAsync(commandQueue: CommandQueue, reason: String, scope: CoroutineScope? = null): Job {
    val target = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    return target.launch { commandQueue.readStatus(reason) }
}

/**
 * `CommandQueue.loadHistory()` / `setUserOptions()` are `suspend` on dev and used to take a
 * `Callback`. These run the command and hand the `PumpEnactResult` to [onResult] on the IO
 * dispatcher, which is where the old `Callback.run()` used to be invoked.
 */
fun loadHistoryAsync(commandQueue: CommandQueue, type: Byte, scope: CoroutineScope, onResult: (PumpEnactResult) -> Unit) {
    scope.launch { onResult(commandQueue.loadHistory(type)) }
}

fun setUserOptionsAsync(commandQueue: CommandQueue, scope: CoroutineScope, onResult: (PumpEnactResult) -> Unit) {
    scope.launch { onResult(commandQueue.setUserOptions()) }
}

/**
 * `PumpSync.expectedPumpState()` and `ProfileFunction.getProfile()` are `suspend` on dev but are
 * called from the driver's plain (non-suspend) BLE message handlers. These bridge them with
 * `runBlocking`, matching what the decompiled code did before the API became suspending.
 */
fun expectedPumpStateBlocking(pumpSync: PumpSync): PumpSync.PumpState = runBlocking { pumpSync.expectedPumpState() }

fun effectiveProfileBlocking(profileFunction: ProfileFunction): EffectiveProfile? = runBlocking { profileFunction.getProfile() }

/**
 * `EventPumpStatusChanged` exposes two `getStatus()` methods on the JVM - the `status` property
 * and the `EventStatus.getStatus(): TextRef` override - so a Java caller cannot disambiguate.
 * These two helpers name each one explicitly.
 */
fun pumpConnectionStatus(event: EventPumpStatusChanged): EventPumpStatusChanged.Status = event.status

fun pumpStatusText(event: EventPumpStatusChanged): TextRef = event.getStatus()

/** `PumpSync.insertAnnouncement()` / `syncCarbsWithTimestamp()` are suspend on dev. */
fun insertAnnouncementAsync(
    pumpSync: PumpSync,
    error: String,
    pumpType: app.aaps.core.data.pump.defs.PumpType,
    pumpSerial: String,
    scope: CoroutineScope
) {
    scope.launch { pumpSync.insertAnnouncement(error, null, pumpType, pumpSerial) }
}

fun syncCarbsAsync(
    pumpSync: PumpSync,
    timestamp: Long,
    amount: Int,
    pumpType: app.aaps.core.data.pump.defs.PumpType,
    pumpSerial: String,
    scope: CoroutineScope
) {
    scope.launch { pumpSync.syncCarbsWithTimestamp(timestamp, amount.toDouble(), null, pumpType, pumpSerial) }
}

/**
 * Bridges the Flow-based [RxBus] back to the RxJava `Observable` the decompiled driver was
 * written against, so `rxBus.toObservable(X.class).observeOn(scheduler)` keeps compiling.
 *
 * Events are forwarded into a [PublishSubject]; the upstream collection is resilient, so a
 * failure while handling one event does not kill the subscription. The returned stream is
 * cold in the sense that only emissions after this call are seen - same as `toFlow`.
 */
fun <T : Event> rxBusObservable(
    rxBus: RxBus,
    aapsLogger: AAPSLogger,
    eventClass: KClass<T>,
    scope: CoroutineScope,
    scheduler: Scheduler
): Observable<T> {
    val subject = PublishSubject.create<T>()
    rxBus.toFlow(eventClass)
        .collectResilient(scope, aapsLogger, LTag.PUMP) { subject.onNext(it) }
    return subject.observeOn(scheduler)
}
