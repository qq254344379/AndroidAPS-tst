package app.aaps.ui.compose.clientcontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.aaps.core.interfaces.clientcontrol.ActionProgress
import app.aaps.ui.R
import app.aaps.core.ui.R as CoreUiR

/**
 * Modal shown while a client→master round-trip action is in flight, and for its terminal
 * non-success states. [ActionProgress.Applied] is handled by the caller (dismiss + confirm), so it is
 * never passed here.
 *
 * While waiting ([ActionProgress.Sending] / [ActionProgress.MasterExecuting]) the only action is
 * "Stop waiting" — deliberately not "Cancel": the command may already have been uploaded and could
 * still apply on the master; dismissing only stops watching for the result.
 *
 * Modal: back-press and tap-outside are disabled, so the dialog can only be left via its buttons —
 * a stray tap can't silently drop a therapy action mid-flight.
 */
@Composable
fun ClientControlPendingDialog(
    progress: ActionProgress,
    onDismiss: () -> Unit
) {
    // Sending and MasterExecuting share one visual ("working") so the dialog doesn't resize mid-flight
    // as the state advances — only the terminal states differ.
    val waiting = progress is ActionProgress.Sending || progress is ActionProgress.MasterExecuting

    val title = when (progress) {
        is ActionProgress.Rejected    -> stringResource(R.string.clientcontrol_pending_rejected_title)
        is ActionProgress.Unconfirmed -> stringResource(R.string.clientcontrol_pending_unconfirmed_title)
        else                          -> stringResource(R.string.clientcontrol_pending_working_title) // Sending/MasterExecuting/(Applied)
    }

    AlertDialog(
        // Modal: only the buttons dismiss it; back-press / tap-outside are disabled so a therapy
        // action in flight can't be dropped by a stray tap.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            // Fixed width + min height keep the dialog a stable size across state transitions.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalArrangement = Arrangement.Center
            ) {
                when (progress) {
                    is ActionProgress.Rejected    -> Text(progress.reason ?: stringResource(R.string.clientcontrol_pending_rejected_text))
                    is ActionProgress.Unconfirmed -> Text(stringResource(R.string.clientcontrol_pending_unconfirmed_text))
                    else                          ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.clientcontrol_pending_working_text))
                        }
                }
            }
        },
        confirmButton = {
            if (!waiting) TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.ok)) }
        },
        dismissButton = {
            if (waiting) TextButton(onClick = onDismiss) { Text(stringResource(R.string.clientcontrol_pending_stop_waiting)) }
        }
    )
}
