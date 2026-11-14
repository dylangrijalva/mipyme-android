package co.mipyme.screen.initial_sync

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.R
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.sync.SyncEngine
import co.mipyme.sync.SyncError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InitialSyncViewModel(
    private val syncEngine: SyncEngine,
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(InitialSyncScreenUiState())
    val stateFlow: StateFlow<InitialSyncScreenUiState> = _stateFlow.asStateFlow()
    val state: InitialSyncScreenUiState
        get() = _stateFlow.value

    init {
        synchronize()
    }

    fun onRetry() {
        if (state.isSyncing || state.isSigningOut) return
        synchronize()
    }

    fun onSignOutClick() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = true) }
    }

    fun onSignOutConfirmed() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSigningOut = true, isSignOutConfirmationVisible = false) }
        viewModelScope.launch {
            val result = authService.signOut()
            _stateFlow.update { it.copy(isSigningOut = false) }
            result.onLeft { error ->
                _stateFlow.update { it.copy(pendingToastRes = error.toStringResource()) }
            }
        }
    }

    fun onSignOutDismissed() {
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = false) }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    private fun synchronize() {
        _stateFlow.update { it.copy(isSyncing = true, errorRes = null) }
        viewModelScope.launch {
            val result = syncEngine.synchronize()
            val errorRes = result.fold(
                ifLeft = { it.toStringResource() },
                ifRight = { if (isInitialSyncCompleted()) null else R.string.initial_sync_error_unknown },
            )
            _stateFlow.update { current -> current.copy(isSyncing = false, errorRes = errorRes) }
        }
    }

    private suspend fun isInitialSyncCompleted(): Boolean {
        val merchantId = (authService.authStateFlow.value as? AuthState.Authenticated)
            ?.account
            ?.publicMetadata
            ?.merchantId ?: return false
        return syncEngine.initialSyncCompletedFlow(merchantId).first()
    }

    @StringRes
    private fun SyncError.toStringResource(): Int {
        return when (this) {
            SyncError.Network -> R.string.initial_sync_error_network
            SyncError.Server -> R.string.initial_sync_error_server
            SyncError.Unauthorized -> R.string.initial_sync_error_unauthorized
            SyncError.Forbidden -> R.string.initial_sync_error_forbidden
            SyncError.Unknown -> R.string.initial_sync_error_unknown
        }
    }
}
