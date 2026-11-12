package co.mipyme.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.toStringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(HomeScreenUiState())
    val stateFlow: StateFlow<HomeScreenUiState> = _stateFlow.asStateFlow()
    val state: HomeScreenUiState
        get() = _stateFlow.value

    fun onSignOut() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            val result = authService.signOut()
            _stateFlow.update { it.copy(isSigningOut = false) }
            result.onLeft { error ->
                _stateFlow.update { it.copy(pendingToastRes = error.toStringResource()) }
            }
        }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }
}
