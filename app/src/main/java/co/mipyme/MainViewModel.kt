package co.mipyme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.data.notification.NotificationPermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val authService: AuthService,
    private val notificationPermissionRepository: NotificationPermissionRepository,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(MainScreenUiState())
    val stateFlow = _stateFlow.asStateFlow()
    val uiState: MainScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            launchDestinationFlow().collect { destination ->
                _stateFlow.update { it.copy(launchDestination = destination) }
            }
        }
    }

    private fun launchDestinationFlow(): Flow<LaunchDestination> {
        return combine(
            authService.authStateFlow,
            notificationPermissionRepository.answeredFlow,
        ) { authState, answered ->
            authState.toLaunchDestination(answered)
        }
    }

    private suspend fun AuthState.toLaunchDestination(answered: Boolean): LaunchDestination {
        return when (this) {
            AuthState.Initializing -> LaunchDestination.Idle
            is AuthState.Authenticated -> {
                if (account.publicMetadata.merchantId == null) {
                    LaunchDestination.Onboarding
                } else if (!answered && !notificationPermissionRepository.isPermissionGranted()) {
                    LaunchDestination.AskNotificationPermissions
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
