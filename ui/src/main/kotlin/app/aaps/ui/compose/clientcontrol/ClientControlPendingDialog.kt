package app.aaps.ui.compose.clientcontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
    val waiting = progress is ActionProgress.Sending || progress is ActionProgress.MasterExecuting

    val title = when (progress) {
        is ActionProgress.Sending        -> stringResource(R.string.clientcontrol_pending_sending_title)
        is ActionProgress.MasterExecuting -> stringResource(R.string.clientcontrol_pending_executing_title)
        is ActionProgress.Rejected       -> stringResource(R.string.clientcontrol_pending_rejected_title)
        is ActionProgress.Unconfirmed    -> stringResource(R.string.clientcontrol_pending_unconfirmed_title)
        is ActionProgress.Applied        -> stringResource(R.string.clientcontrol_pending_executing_title) // not expected
    }

    AlertDialog(
        // Modal: only the buttons dismiss it; back-press / tap-outside are disabled so a therapy
        // action in flight can't be dropped by a stray tap.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            when (progress) {
                is ActionProgress.Sending        ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.clientcontrol_pending_sending_text))
                    }

                is ActionProgress.MasterExecuting ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.clientcontrol_pending_executing_text))
                    }

                is ActionProgress.Rejected       ->
                    Text(progress.reason ?: stringResource(R.string.clientcontrol_pending_rejected_text))

                is ActionProgress.Unconfirmed    ->
                    Text(stringResource(R.string.clientcontrol_pending_unconfirmed_text))

                is ActionProgress.Applied        -> Unit
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
