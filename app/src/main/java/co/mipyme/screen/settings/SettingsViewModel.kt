package co.mipyme.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.R
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.Employee
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.data.theme.ThemeMode
import co.mipyme.data.theme.ThemeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authService: AuthService,
    private val currentEmployeeRepository: CurrentEmployeeRepository,
    private val locationSelectionRepository: LocationSelectionRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(SettingsScreenUiState())
    val stateFlow: StateFlow<SettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: SettingsScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            visibleItemsFlow().collect { items -> _stateFlow.update { it.copy(visibleItems = items) } }
        }
        viewModelScope.launch {
            canSwitchLocationFlow().collect { canSwitch ->
                _stateFlow.update { it.copy(canSwitchLocation = canSwitch) }
            }
        }
        viewModelScope.launch {
            themeRepository.themeModeFlow.collect { mode -> _stateFlow.update { it.copy(themeMode = mode) } }
        }
    }

    fun onThemeClick() {
        _stateFlow.update { it.copy(isThemeSheetVisible = true) }
    }

    fun onThemeSheetDismissed() {
        _stateFlow.update { it.copy(isThemeSheetVisible = false) }
    }

    fun onThemeSelected(mode: ThemeMode) {
        _stateFlow.update { it.copy(isThemeSheetVisible = false) }
        viewModelScope.launch {
            themeRepository.setThemeMode(mode).onLeft {
                _stateFlow.update { it.copy(pendingToastRes = R.string.settings_theme_error) }
            }
        }
    }

    fun onSwitchLocationClick() {
        if (!state.canSwitchLocation || state.isSwitchingLocation || state.isSigningOut) return
        _stateFlow.update { it.copy(isSwitchingLocation = true) }
        viewModelScope.launch {
            val result = locationSelectionRepository.clearSelection()
            _stateFlow.update { it.copy(isSwitchingLocation = false) }
            result.onLeft {
                _stateFlow.update { it.copy(pendingToastRes = R.string.settings_switch_location_error) }
            }
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
    private fun authenticatedEmployeeFlow(): Flow<Employee?> {
        return authService.authStateFlow
            .map { it is AuthState.Authenticated }
            .distinctUntilChanged()
            .flatMapLatest { isAuthenticated ->
                if (isAuthenticated) currentEmployeeRepository.currentEmployeeFlow else emptyFlow()
            }
    }

    private fun visibleItemsFlow(): Flow<List<SettingsItem>> {
        return authenticatedEmployeeFlow()
            .map { it.visibleSettingsItems() }
            .distinctUntilChanged()
    }

    private fun canSwitchLocationFlow(): Flow<Boolean> {
        return combine(
            authenticatedEmployeeFlow(),
            locationSelectionRepository.accessibleLocationsFlow,
        ) { employee, locations ->
            employee?.isOwner == true || locations.size > 1
        }
            .distinctUntilChanged()
    }
}
