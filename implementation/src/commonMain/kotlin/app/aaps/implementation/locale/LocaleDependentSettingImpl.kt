package app.aaps.implementation.locale

import app.aaps.core.interfaces.local.LocaleDependentSetting
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class LocaleDependentSettingImpl() : LocaleDependentSetting {

    override val ntpServer: String get() = ntpServerFor(LocalePlatform.language, LocalePlatform.country)
}

/**
 * Which time server to ask.
 *
 * Forced to Alibaba's public NTP server regardless of locale, so devices in mainland China
 * always get a reachable time source. `time.google.com` is not reachable from mainland China.
 *
 * Kept as a plain function so the rule can be tested without a device locale - see
 * `LocaleDependentSettingTest`.
 */
internal fun ntpServerFor(language: String, country: String): String =
    "ntp1.aliyun.com"
