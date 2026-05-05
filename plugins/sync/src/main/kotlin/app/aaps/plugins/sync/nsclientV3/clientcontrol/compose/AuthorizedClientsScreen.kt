package app.aaps.plugins.sync.nsclientV3.clientcontrol.compose

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.nssdk.localmodel.clientcontrol.AuthorizedClient
import app.aaps.core.nssdk.localmodel.clientcontrol.ClientState
import app.aaps.core.nssdk.localmodel.clientcontrol.PairingPayload
import app.aaps.core.ui.compose.AapsSpacing
import app.aaps.core.ui.compose.AapsTopAppBar
import app.aaps.plugins.sync.R
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import net.glxn.qrgen.android.QRCode
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizedClientsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthorizedClientsViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val pairingPayload by viewModel.pairingPayload.collectAsStateWithLifecycle()

    // Re-prune expired pending entries every second while any are pending,
    // so the countdown ticks down and expired ones drop without a manual refresh.
    val anyPending = clients.any { it.state == ClientState.Pending }
    LaunchedEffect(anyPending) {
        while (anyPending) {
            delay(1_000L)
            viewModel.pruneExpired()
        }
    }

    when (val state = dialogState) {
        AuthorizedClientsViewModel.DialogState.EnterName        -> EnterNameDialog(
            onConfirm = { viewModel.confirmAdd(it) },
            onDismiss = viewModel::dismissDialog
        )

        is AuthorizedClientsViewModel.DialogState.ConfirmDelete -> ConfirmDeleteDialog(
            clientName = state.client.name,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDialog
        )

        null                                                    -> Unit
    }

    pairingPayload?.let { payload ->
        PairingQrDialog(
            payload = payload,
            onDismiss = viewModel::dismissPairing
        )
    }

    Scaffold(
        topBar = {
            AapsTopAppBar(
                title = { Text(stringResource(R.string.authorized_clients_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(app.aaps.core.ui.R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::requestAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.authorized_clients_add))
            }
        }
    ) { paddingValues ->
        if (clients.isEmpty()) {
            EmptyState(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(AapsSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AapsSpacing.medium)
            ) {
                items(clients, key = { it.clientId }) { client ->
                    AuthorizedClientCard(
                        client = client,
                        viewModel = viewModel,
                        onDelete = { viewModel.requestDelete(client) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorizedClientCard(
    client: AuthorizedClient,
    viewModel: AuthorizedClientsViewModel,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AapsSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(AapsSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = when (client.state) {
                    ClientState.Active  -> MaterialTheme.colorScheme.primary
                    ClientState.Pending -> MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StateBadgeAndTimeRow(client = client, viewModel = viewModel)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(app.aaps.core.ui.R.string.delete))
            }
        }
    }
}

@Composable
private fun StateBadgeAndTimeRow(
    client: AuthorizedClient,
    viewModel: AuthorizedClientsViewModel
) {
    val timeText = when (client.state) {
        ClientState.Active  -> viewModel.lastSeenLabel(client)
        ClientState.Pending -> viewModel.pendingExpiresLabel(client)
    }
    val stateLabel = when (client.state) {
        ClientState.Active  -> stringResource(R.string.authorized_clients_state_active)
        ClientState.Pending -> stringResource(R.string.authorized_clients_state_pending)
    }
    val stateColor = when (client.state) {
        ClientState.Active  -> MaterialTheme.colorScheme.primary
        ClientState.Pending -> MaterialTheme.colorScheme.tertiary
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AapsSpacing.small)) {
        Text(text = stateLabel, style = MaterialTheme.typography.labelSmall, color = stateColor)
        Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AapsSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.authorized_clients_empty_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.authorized_clients_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AapsSpacing.medium)
        )
    }
}

@Composable
private fun EnterNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.authorized_clients_name_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(48) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.authorized_clients_name_placeholder)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(app.aaps.core.ui.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(app.aaps.core.ui.R.string.cancel)) }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    clientName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.authorized_clients_delete_dialog_title)) },
        text = { Text(stringResource(R.string.authorized_clients_delete_dialog_message, clientName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(app.aaps.core.ui.R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(app.aaps.core.ui.R.string.cancel)) }
        }
    )
}

private val pairingJson = Json { encodeDefaults = true }

@Composable
private fun PairingQrDialog(
    payload: PairingPayload,
    onDismiss: () -> Unit
) {
    // Sensitive content — block screenshots/recents previews while the QR is on screen.
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    var revealed by remember { mutableStateOf(false) }
    var msLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(payload.expiresAt) {
        while (true) {
            msLeft = payload.expiresAt - System.currentTimeMillis()
            if (msLeft <= 0L) {
                onDismiss()
                break
            }
            delay(1_000L)
        }
    }

    val qrBitmap = remember(payload) {
        QRCode.from(pairingJson.encodeToString(PairingPayload.serializer(), payload))
            .withErrorCorrection(ErrorCorrectionLevel.H)
            .withSize(512, 512)
            .bitmap()
    }
    val qrSizeDp = with(LocalConfiguration.current) { min(screenWidthDp, screenHeightDp) * 0.7 }.toInt().dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AapsSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AapsSpacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.authorized_clients_qr_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.authorized_clients_qr_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(qrSizeDp)
                        .clickable(enabled = !revealed) { revealed = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.authorized_clients_qr_dialog_title),
                        modifier = Modifier.fillMaxSize().run {
                            if (revealed) this else blur(28.dp)
                        }
                    )
                    if (!revealed) {
                        Text(
                            text = stringResource(R.string.authorized_clients_qr_tap_to_reveal),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.authorized_clients_qr_expires_in,
                        (msLeft / 1000L).coerceAtLeast(0L).toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.authorized_clients_qr_done))
                }
            }
        }
    }
}
