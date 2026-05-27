package app.aaps.ui.compose.scenes

import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventShowDialog
import app.aaps.core.interfaces.scenes.ClientControlSendResult
import app.aaps.core.ui.R

/**
 * Routes a [ClientControlSendResult] to a user-visible error dialog for the failure branches.
 * No-op on [ClientControlSendResult.Success] — the master's settings-doc republish is the
 * user-visible "it worked" signal, so the happy path needs nothing.
 *
 * Two failure cases get distinct dialogs because the user remediation differs:
 * - [NotPaired][ClientControlSendResult.NotPaired]: needs to complete pairing first.
 * - [PublishFailed][ClientControlSendResult.PublishFailed]: transient — retry later.
 */
fun ClientControlSendResult.surfaceErrorDialog(rxBus: RxBus, rh: ResourceHelper) {
    when (this) {
        ClientControlSendResult.Success           -> Unit
        ClientControlSendResult.NotPaired         -> rxBus.send(
            EventShowDialog.Error(
                title = rh.gs(R.string.clientcontrol_error_not_paired_title),
                message = rh.gs(R.string.clientcontrol_error_not_paired_message)
            )
        )
        is ClientControlSendResult.PublishFailed  -> rxBus.send(
            EventShowDialog.Error(
                title = rh.gs(R.string.clientcontrol_error_publish_failed_title),
                message = rh.gs(R.string.clientcontrol_error_publish_failed_message)
            )
        )
    }
}
