package app.aaps.plugins.sync.nsclientV3.clientcontrol.compose

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.StringNonKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.nssdk.localmodel.clientcontrol.AuthorizedClient
import app.aaps.core.nssdk.localmodel.clientcontrol.PairingPayload
import app.aaps.core.nssdk.utils.ClientControlCrypto
import app.aaps.plugins.sync.R
import app.aaps.plugins.sync.nsclientV3.clientcontrol.AuthorizedClientsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
@Stable
class AuthorizedClientsViewModel @Inject constructor(
    private val repository: AuthorizedClientsRepository,
    private val dateUtil: DateUtil,
    private val rh: ResourceHelper,
    private val preferences: Preferences
) : ViewModel() {

    companion object {

        const val QR_TTL_MS = 2L * 60L * 1000L
    }

    val clients: StateFlow<List<AuthorizedClient>> = repository.observe()
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    /**
     * Active pairing payload: non-null while the QR for a freshly-added client is on screen.
     * Holds the **plaintext secret hex**, intentionally not persisted — process death drops it,
     * and the user has to delete + re-add the pending entry to get a new QR.
     */
    private val _pairingPayload = MutableStateFlow<PairingPayload?>(null)
    val pairingPayload: StateFlow<PairingPayload?> = _pairingPayload.asStateFlow()

    fun requestAdd() {
        _dialogState.value = DialogState.EnterName
    }

    /** Adds a Pending entry to the repository AND seeds [pairingPayload] for the QR display. */
    fun confirmAdd(name: String) {
        _dialogState.value = null
        val now = dateUtil.now()
        val result = repository.addPending(name.trim(), QR_TTL_MS, now)
        _pairingPayload.value = PairingPayload(
            masterInstallId = ownInstallId(),
            clientId = result.entry.clientId,
            secretHex = result.secretHex,
            expiresAt = result.entry.qrExpiresAt
        )
    }

    /** Called when the QR dialog dismisses (user taps Done, presses back, or expiry hits). */
    fun dismissPairing() {
        _pairingPayload.value = null
    }

    fun requestDelete(client: AuthorizedClient) {
        _dialogState.value = DialogState.ConfirmDelete(client)
    }

    fun confirmDelete() {
        val state = _dialogState.value as? DialogState.ConfirmDelete ?: return
        repository.delete(state.client.clientId)
        _dialogState.value = null
    }

    fun dismissDialog() {
        _dialogState.value = null
    }

    fun pruneExpired() {
        repository.pruneExpired(dateUtil.now())
    }

    /** "Last seen 5m ago" / "Never used" — formatted from the entry's lastSeenAt. */
    fun lastSeenLabel(client: AuthorizedClient): String =
        if (client.lastSeenAt > 0L) rh.gs(R.string.authorized_clients_last_seen, dateUtil.minAgo(rh, client.lastSeenAt))
        else rh.gs(R.string.authorized_clients_never_seen)

    /** "QR expires in 95s" — re-evaluated on each composition while a Pending tick is running. */
    fun pendingExpiresLabel(client: AuthorizedClient): String {
        val secondsLeft = ((client.qrExpiresAt - dateUtil.now()) / 1000L).coerceAtLeast(0L)
        return rh.gs(R.string.authorized_clients_pending_expires_in, secondsLeft.toString())
    }

    /** Lazy UUID generated on first use; persists across app restarts so paired clients keep matching. */
    private fun ownInstallId(): String {
        val existing = preferences.get(StringNonKey.NsClientControlOwnInstallId)
        if (existing.isNotEmpty()) return existing
        val fresh = ClientControlCrypto.newClientId()
        preferences.put(StringNonKey.NsClientControlOwnInstallId, fresh)
        return fresh
    }

    sealed class DialogState {
        data object EnterName : DialogState()
        data class ConfirmDelete(val client: AuthorizedClient) : DialogState()
    }
}
