package co.mipyme.screen.general_settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GeneralSettingsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(GeneralSettingsScreenUiState)
    val stateFlow: StateFlow<GeneralSettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: GeneralSettingsScreenUiState
        get() = _stateFlow.value
}
