package co.mipyme.screen.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val BACK_STACK_KEY = "back_stack"

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authService: AuthService,
    private val locationSelectionRepository: LocationSelectionRepository,
    private val currentEmployeeRepository: CurrentEmployeeRepository,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(
        HomeScreenUiState(email = emailOf(authService.authStateFlow.value)),
    )
    val stateFlow: StateFlow<HomeScreenUiState> = _stateFlow.asStateFlow()
    val state: HomeScreenUiState
        get() = _stateFlow.value

    private val restoredCurrent: HomeDestination? = savedStateHandle.get<ArrayList<String>>(BACK_STACK_KEY)
        ?.lastOrNull()
        ?.let { name -> HomeDestination.entries.firstOrNull { it.name == name } }

    init {
        viewModelScope.launch {
            authService.authStateFlow.collect { authState ->
                _stateFlow.update { it.copy(email = emailOf(authState)) }
            }
        }
        viewModelScope.launch {
            locationSelectionRepository.selectedLocationFlow.collect { location ->
                _stateFlow.update { it.copy(locationName = location?.name) }
            }
        }
        viewModelScope.launch {
            visibleDestinationsFlow().collect { visible -> onVisibleDestinationsChanged(visible) }
        }
    }

    fun onDestinationSelected(destination: HomeDestination) {
        val current = state
        if (destination !in current.visibleDestinations || destination == current.backStack.lastOrNull()) return
        setBackStack(stackFor(destination, current.visibleDestinations.first()))
    }

    fun onBack() {
        val current = state
        if (current.backStack.size > 1) {
            setBackStack(listOf(current.visibleDestinations.first()))
        }
    }

    fun onLostAccessToastShown() {
        _stateFlow.update { it.copy(pendingLostAccessDestination = null) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun visibleDestinationsFlow(): Flow<List<HomeDestination>> {
        return authService.authStateFlow
            .map { it is AuthState.Authenticated }
            .distinctUntilChanged()
            .flatMapLatest { isAuthenticated ->
                if (isAuthenticated) currentEmployeeRepository.currentEmployeeFlow else emptyFlow()
            }
            .map { it.visibleHomeDestinations() }
            .distinctUntilChanged()
    }

    private fun onVisibleDestinationsChanged(visible: List<HomeDestination>) {
        val anchor = visible.first()
        val current = state.backStack.lastOrNull() ?: restoredCurrent
        val hasLostAccess = current != null && current !in visible
        val backStack = stackFor(current.takeIf { it in visible } ?: anchor, anchor)
        savedStateHandle[BACK_STACK_KEY] = ArrayList(backStack.map { it.name })
        _stateFlow.update {
            it.copy(
                visibleDestinations = visible,
                backStack = backStack,
                pendingLostAccessDestination = if (hasLostAccess) current else it.pendingLostAccessDestination,
            )
        }
    }

    private fun setBackStack(backStack: List<HomeDestination>) {
        savedStateHandle[BACK_STACK_KEY] = ArrayList(backStack.map { it.name })
        _stateFlow.update { it.copy(backStack = backStack) }
    }

    private fun stackFor(destination: HomeDestination, anchor: HomeDestination): List<HomeDestination> {
        return if (destination == anchor) listOf(anchor) else listOf(anchor, destination)
    }

    private fun emailOf(authState: AuthState): String? {
        return (authState as? AuthState.Authenticated)?.account?.email
    }
}
