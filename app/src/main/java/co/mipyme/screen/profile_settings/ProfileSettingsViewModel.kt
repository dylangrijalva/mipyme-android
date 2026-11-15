package co.mipyme.screen.profile_settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileSettingsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(ProfileSettingsScreenUiState)
    val stateFlow: StateFlow<ProfileSettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: ProfileSettingsScreenUiState
        get() = _stateFlow.value
}
