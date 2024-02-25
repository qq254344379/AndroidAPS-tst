package app.aaps.core.keys

enum class IntentKey(
    override val key: Int,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanKey? = null,
    override val negativeDependency: BooleanKey? = null,
    override val hideParentScreenIfHidden: Boolean = false
) : PreferenceKey {

    ApsLinkToDocs(key = R.string.key_openaps_link_to_docs),
    SmsOtpSetup(key = R.string.key_smscommunicator_otp_setup, dependency = BooleanKey.SmsAllowRemoteCommands),
}