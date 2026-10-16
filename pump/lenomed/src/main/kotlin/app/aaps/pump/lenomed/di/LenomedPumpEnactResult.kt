package app.aaps.pump.lenomed.di

import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.resources.TextResolver
import app.aaps.core.keys.interfaces.TextRef

/**
 * Mutable [PumpEnactResult] for the ported driver.
 *
 * `LenomedPlugin` was written against the old mutable `PumpEnactResult` data class and fills a result
 * with setters (`setSuccess`, `setComment`, ...) before returning it, so it needs a mutable
 * implementation. dev moved to an immutable builder (`PumpEnactResultObject`) that lives in
 * `:implementation` - a module a pump driver must not depend on, and the wrong shape anyway.
 *
 * Defaults match `PumpEnactResultObject` so the values the driver does not set read the same as they
 * do for every other driver.
 */
internal class LenomedPumpEnactResult(private val rh: TextResolver) : PumpEnactResult {

    override var success = false
    override var enacted = false
    override var comment = ""

    override var duration = -1
    override var absolute = -1.0
    override var percent = -1
    override var isPercent = false
    override var isTempCancel = false

    override var bolusDelivered = 0.0
    override var queued = false
    override var cancelled = false

    override fun toString() =
        "LenomedPumpEnactResult(success=$success,enacted=$enacted,comment=$comment," +
            "duration=$duration,absolute=$absolute,percent=$percent,isPercent=$isPercent," +
            "isTempCancel=$isTempCancel,bolusDelivered=$bolusDelivered,queued=$queued)"

    override fun success(success: Boolean): PumpEnactResult = also { it.success = success }
    override fun cancelled(cancelled: Boolean): PumpEnactResult = also { it.cancelled = cancelled }
    override fun enacted(enacted: Boolean): PumpEnactResult = also { it.enacted = enacted }
    override fun comment(comment: String): PumpEnactResult = also { it.comment = comment }
    override fun comment(ref: TextRef): PumpEnactResult = also { it.comment = rh.gs(ref) }
    override fun duration(duration: Int): PumpEnactResult = also { it.duration = duration }
    override fun absolute(absolute: Double): PumpEnactResult = also { it.absolute = absolute }
    override fun percent(percent: Int): PumpEnactResult = also { it.percent = percent }
    override fun isPercent(isPercent: Boolean): PumpEnactResult = also { it.isPercent = isPercent }
    override fun isTempCancel(isTempCancel: Boolean): PumpEnactResult =
        also { it.isTempCancel = isTempCancel }

    override fun bolusDelivered(bolusDelivered: Double): PumpEnactResult =
        also { it.bolusDelivered = bolusDelivered }

    override fun queued(queued: Boolean): PumpEnactResult = also { it.queued = queued }
}
