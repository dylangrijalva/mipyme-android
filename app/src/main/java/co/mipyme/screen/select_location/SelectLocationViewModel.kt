package co.mipyme.screen.select_location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.R
import co.mipyme.auth.AuthService
import co.mipyme.auth.toStringResource
import co.mipyme.data.location.LocationSelectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectLocationViewModel(
    private val locationSelectionRepository: LocationSelectionRepository,
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(SelectLocationScreenUiState())
    val stateFlow: StateFlow<SelectLocationScreenUiState> = _stateFlow.asStateFlow()
    val state: SelectLocationScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            locationSelectionRepository.accessibleLocationsFlow.collect { locations ->
                _stateFlow.update { current ->
                    current.copy(locations = locations.map { it.toSelectLocationItem() })
                }
            }
        }
    }

    fun onLocationClick(locationId: String) {
        if (state.isSelecting || state.isSigningOut || state.isSelectionComplete) return
        _stateFlow.update { it.copy(isSelecting = true) }
        viewModelScope.launch {
            val result = locationSelectionRepository.select(locationId)
            _stateFlow.update { it.copy(isSelecting = false, isSelectionComplete = result.isRight()) }
            result.onLeft {
                _stateFlow.update { current -> current.copy(pendingToastRes = R.string.select_location_error) }
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
}
