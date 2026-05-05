package app.aaps.plugins.sync.nsclientV3.clientcontrol.compose

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.localmodel.clientcontrol.MasterPairing
import app.aaps.core.nssdk.localmodel.clientcontrol.PairingPayload
import app.aaps.plugins.sync.nsclientV3.clientcontrol.ClientControlPublisher
import app.aaps.plugins.sync.nsclientV3.clientcontrol.ClientPairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
@Stable
class PairWithMasterViewModel @Inject constructor(
    private val repository: ClientPairingRepository,
    private val publisher: ClientControlPublisher,
    private val dateUtil: DateUtil
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<UiState>(initialState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun initialState(): UiState =
        repository.currentPairing()?.let { UiState.AlreadyPaired(it) } ?: UiState.Scanning

    /**
     * Called from the camera analyzer. Idempotent: ignored unless the screen is in [UiState.Scanning].
     * Successful parse advances to [UiState.Confirming]; bad payloads advance to [UiState.Error]
     * which the UI can offer to retry.
     */
    fun onQrDecoded(raw: String) {
        if (_state.value !is UiState.Scanning) return
        val payload = parsePayload(raw)
        if (payload == null) {
            _state.value = UiState.Error(ErrorReason.MalformedQr)
            return
        }
        if (payload.expiresAt > 0L && payload.expiresAt < dateUtil.now()) {
            _state.value = UiState.Error(ErrorReason.QrExpired)
            return
        }
        _state.value = UiState.Confirming(payload)
    }

    /**
     * User confirmed the pairing. Persists locally, then publishes the signed hello
     * to NS in [UiState.Sending] (holds the screen alive — `viewModelScope` cancels
     * on pop, so the HTTP call must finish before we advance to Success).
     *
     * On hello upload failure we still advance to Success: the local pairing is valid,
     * the master will simply stay in Pending until a future retry. Surfacing a hard
     * error here would block the user even though they have a working pairing.
     */
    fun confirmPair() {
        val payload = (_state.value as? UiState.Confirming)?.payload ?: return
        repository.pair(payload)
        _state.value = UiState.Sending
        viewModelScope.launch {
            publisher.publishHello()
            _state.value = UiState.Success
        }
    }

    /** User cancelled the confirmation dialog. */
    fun cancelConfirmation() {
        if (_state.value is UiState.Confirming) _state.value = UiState.Scanning
    }

    /** User taps "Try again" on an error state. */
    fun resumeScanning() {
        _state.value = UiState.Scanning
    }

    /** Wipes the existing pairing and returns the user to the scanner. */
    fun unpair() {
        repository.unpair()
        _state.value = UiState.Scanning
    }

    private fun parsePayload(raw: String): PairingPayload? = try {
        val decoded = json.decodeFromString<PairingPayload>(raw)
        if (decoded.masterInstallId.isBlank() || decoded.clientId.isBlank() || decoded.secretHex.isBlank()) null
        else decoded
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    sealed class UiState {
        data object Scanning : UiState()
        data class Confirming(val payload: PairingPayload) : UiState()
        data object Sending : UiState()
        data object Success : UiState()
        data class Error(val reason: ErrorReason) : UiState()
        data class AlreadyPaired(val pairing: MasterPairing) : UiState()
    }

    enum class ErrorReason { MalformedQr, QrExpired }
}
