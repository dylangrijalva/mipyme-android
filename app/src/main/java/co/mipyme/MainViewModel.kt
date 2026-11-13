package co.mipyme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(MainScreenUiState())
    val stateFlow = _stateFlow.asStateFlow()
    val uiState: MainScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            authService.authStateFlow.collect { authState ->
                _stateFlow.update { it.copy(launchDestination = authState.toLaunchDestination()) }
            }
        }
    }

    private fun AuthState.toLaunchDestination(): LaunchDestination {
        return when (this) {
            AuthState.Initializing -> LaunchDestination.Idle
            is AuthState.Authenticated -> {
                if (account.publicMetadata.merchantId == null) {
                    LaunchDestination.Onboarding
                } else {
                    LaunchDestination.Home
                }
            }
            AuthState.Unauthenticated,
            AuthState.SessionExpired,
            AuthState.Blocked -> LaunchDestination.Welcome
        }
    }
}
