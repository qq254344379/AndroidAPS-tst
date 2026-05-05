package app.aaps.plugins.sync.nsclientV3.clientcontrol.compose

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.nssdk.localmodel.clientcontrol.PairingPayload
import app.aaps.core.ui.compose.AapsSpacing
import app.aaps.core.ui.compose.AapsTopAppBar
import app.aaps.plugins.sync.R
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairWithMasterScreen(
    onNavigateBack: () -> Unit,
    viewModel: PairWithMasterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // While the hello upload is in flight, swallow Back to keep the ViewModel (and therefore
    // viewModelScope) alive until publishHello() completes — otherwise the master never sees
    // the hello and the client is stuck Pending on the master side.
    BackHandler(enabled = state is PairWithMasterViewModel.UiState.Sending) { /* swallow */ }

    // After successful pair, drop back to the previous screen so the user lands wherever they came from.
    LaunchedEffect(state) {
        if (state is PairWithMasterViewModel.UiState.Success) onNavigateBack()
    }

    Scaffold(
        topBar = {
            AapsTopAppBar(
                title = { Text(stringResource(R.string.pair_with_master_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(app.aaps.core.ui.R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val s = state) {
                is PairWithMasterViewModel.UiState.AlreadyPaired -> AlreadyPairedContent(
                    masterInstallId = s.pairing.masterInstallId,
                    onUnpair = viewModel::unpair,
                    onCancel = onNavigateBack
                )

                PairWithMasterViewModel.UiState.Scanning         ->
                    if (hasCameraPermission) ScannerContent(onDecoded = viewModel::onQrDecoded)
                    else PermissionDeniedContent(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })

                is PairWithMasterViewModel.UiState.Confirming    -> ConfirmPairingDialog(
                    payload = s.payload,
                    onConfirm = viewModel::confirmPair,
                    onCancel = viewModel::cancelConfirmation
                )

                PairWithMasterViewModel.UiState.Sending          -> SendingContent()

                is PairWithMasterViewModel.UiState.Error         -> ErrorContent(
                    reason = s.reason,
                    onRetry = viewModel::resumeScanning,
                    onCancel = onNavigateBack
                )

                PairWithMasterViewModel.UiState.Success          -> Unit // LaunchedEffect handles back-nav
            }
        }
    }
}

@Composable
private fun ScannerContent(onDecoded: (String) -> Unit) {
    LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                                                     val cameraProvider = cameraProviderFuture.get()
                                                     val preview = Preview.Builder().build().apply {
                                                         surfaceProvider = previewView.surfaceProvider
                                                     }
                                                     val analysis = ImageAnalysis.Builder()
                                                         .setTargetResolution(Size(1280, 720))
                                                         .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                         .build().apply {
                                                             setAnalyzer(analysisExecutor, QrCodeAnalyzer(onDecoded))
                                                         }
                                                     cameraProvider.unbindAll()
                                                     cameraProvider.bindToLifecycle(
                                                         lifecycleOwner,
                                                         CameraSelector.DEFAULT_BACK_CAMERA,
                                                         preview,
                                                         analysis
                                                     )
                                                 }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )
        Text(
            text = stringResource(R.string.pair_with_master_point_camera),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AapsSpacing.large)
        )
    }
}

@Composable
private fun SendingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AapsSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.pair_with_master_sending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = AapsSpacing.large)
        )
    }
}

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AapsSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.pair_with_master_camera_required_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.pair_with_master_camera_required_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AapsSpacing.medium)
        )
        Button(
            onClick = onRequest,
            modifier = Modifier.padding(top = AapsSpacing.large)
        ) {
            Text(stringResource(R.string.pair_with_master_grant_camera))
        }
    }
}

@Composable
private fun ConfirmPairingDialog(
    payload: PairingPayload,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.pair_with_master_confirm_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pair_with_master_confirm_master, payload.masterInstallId),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.pair_with_master_confirm_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = AapsSpacing.medium)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.pair_with_master_confirm_pair))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(app.aaps.core.ui.R.string.cancel))
            }
        }
    )
}

@Composable
private fun ErrorContent(
    reason: PairWithMasterViewModel.ErrorReason,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val message = when (reason) {
        PairWithMasterViewModel.ErrorReason.MalformedQr -> stringResource(R.string.pair_with_master_error_malformed)
        PairWithMasterViewModel.ErrorReason.QrExpired   -> stringResource(R.string.pair_with_master_error_expired)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AapsSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.pair_with_master_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = AapsSpacing.medium)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = AapsSpacing.large)
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.pair_with_master_retry))
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(app.aaps.core.ui.R.string.cancel))
        }
    }
}

@Composable
private fun AlreadyPairedContent(
    masterInstallId: String,
    onUnpair: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AapsSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.pair_with_master_already_paired_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.pair_with_master_already_paired_master, masterInstallId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AapsSpacing.medium)
        )
        Button(
            onClick = onUnpair,
            modifier = Modifier
                .padding(top = AapsSpacing.large)
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.pair_with_master_unpair))
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(app.aaps.core.ui.R.string.close))
        }
    }
}
