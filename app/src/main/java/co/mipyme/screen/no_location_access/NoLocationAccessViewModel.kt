package co.mipyme.screen.no_location_access

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.R
import co.mipyme.auth.AuthService
import co.mipyme.auth.toStringResource
import co.mipyme.sync.SyncEngine
import co.mipyme.sync.SyncError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoLocationAccessViewModel(
    private val syncEngine: SyncEngine,
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(NoLocationAccessScreenUiState())
    val stateFlow: StateFlow<NoLocationAccessScreenUiState> = _stateFlow.asStateFlow()
    val state: NoLocationAccessScreenUiState
        get() = _stateFlow.value

    fun onRefresh() {
        if (state.isRefreshing || state.isSigningOut) return
        _stateFlow.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = syncEngine.synchronize()
            _stateFlow.update { it.copy(isRefreshing = false) }
            result.onLeft { error ->
                _stateFlow.update { it.copy(pendingToastRes = error.toStringResource()) }
            }
        }
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

    @StringRes
    private fun SyncError.toStringResource(): Int {
        return when (this) {
            SyncError.Network -> R.string.no_location_access_error_network
            SyncError.Server -> R.string.no_location_access_error_server
            SyncError.Unauthorized -> R.string.no_location_access_error_unauthorized
            SyncError.Forbidden -> R.string.no_location_access_error_forbidden
            SyncError.Unknown -> R.string.no_location_access_error_unknown
        }
    }
}
