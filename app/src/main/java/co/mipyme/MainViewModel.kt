package co.mipyme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.merchantId
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.data.location.LocationSelectionState
import co.mipyme.data.location.locationUserId
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
    private val locationSelectionRepository: LocationSelectionRepository,
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
            locationSelectionRepository.locationSelectionFlow,
        ) { authState, answered, initialSync, locationSelection ->
            authState.toLaunchDestination(answered, initialSync, locationSelection)
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
        locationSelection: LocationSelectionState,
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
                } else if (locationUserId == null) {
                    LaunchDestination.NoLocationAccess
                } else if (locationSelection.userId != locationUserId || locationSelection.merchantId != merchantId) {
                    null
                } else {
                    toLocationDestination(locationSelection)
                }
            }
            AuthState.Unauthenticated,
            AuthState.SessionExpired,
            AuthState.Blocked -> LaunchDestination.Welcome
        }
    }

    private fun toLocationDestination(locationSelection: LocationSelectionState): LaunchDestination {
        return when {
            locationSelection.accessibleLocations.isEmpty() -> LaunchDestination.NoLocationAccess
            locationSelection.selectedLocation != null -> LaunchDestination.Home
            else -> LaunchDestination.SelectLocation
        }
    }

    private data class InitialSyncProgress(val merchantId: String?, val completed: Boolean)
}
