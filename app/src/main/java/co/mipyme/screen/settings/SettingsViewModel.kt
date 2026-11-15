package co.mipyme.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.data.employee.CurrentEmployeeRepository
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

class SettingsViewModel(
    private val authService: AuthService,
    private val currentEmployeeRepository: CurrentEmployeeRepository,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(SettingsScreenUiState())
    val stateFlow: StateFlow<SettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: SettingsScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            visibleItemsFlow().collect { items -> _stateFlow.update { it.copy(visibleItems = items) } }
        }
    }

    fun onSignOutClick() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = true) }
    }

    fun onSignOutConfirmed() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            val result = authService.signOut()
            _stateFlow.update { it.copy(isSigningOut = false, isSignOutConfirmationVisible = false) }
            result.onLeft { error ->
                _stateFlow.update { it.copy(pendingToastRes = error.toStringResource()) }
            }
        }
    }

    fun onSignOutDismissed() {
        if (state.isSigningOut) return
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = false) }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun visibleItemsFlow(): Flow<List<SettingsItem>> {
        return authService.authStateFlow
            .map { it is AuthState.Authenticated }
            .distinctUntilChanged()
            .flatMapLatest { isAuthenticated ->
                if (isAuthenticated) currentEmployeeRepository.currentEmployeeFlow else emptyFlow()
            }
            .map { it.visibleSettingsItems() }
            .distinctUntilChanged()
    }
}
