package co.mipyme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.merchantId
import co.mipyme.data.notification.NotificationPermissionRepository
import co.mipyme.sync.SyncEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val authService: AuthService,
    private val notificationPermissionRepository: NotificationPermissionRepository,
    private val syncEngine: SyncEngine,
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
            initialSyncProgressFlow(),
        ) { authState, answered, initialSync ->
            authState.toLaunchDestination(answered, initialSync)
        }.filterNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initialSyncProgressFlow(): Flow<InitialSyncProgress> {
        return authService.authStateFlow.flatMapLatest { authState ->
            val merchantId = authState.merchantId
            if (merchantId == null) {
                flowOf(InitialSyncProgress(merchantId = null, completed = false))
            } else {
                syncEngine.initialSyncCompletedFlow(merchantId).map { completed ->
                    InitialSyncProgress(merchantId = merchantId, completed = completed)
                }
            }
        }
    }

    private suspend fun AuthState.toLaunchDestination(
        answered: Boolean,
        initialSync: InitialSyncProgress,
    ): LaunchDestination? {
        return when (this) {
            AuthState.Initializing -> LaunchDestination.Idle
            is AuthState.Authenticated -> {
                val merchantId = account.publicMetadata.merchantId
                if (merchantId == null) {
                    LaunchDestination.Onboarding
                } else if (initialSync.merchantId != merchantId) {
                    null
                } else if (!initialSync.completed) {
                    LaunchDestination.InitialSync
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

    private data class InitialSyncProgress(val merchantId: String?, val completed: Boolean)
}
