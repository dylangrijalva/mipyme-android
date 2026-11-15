package co.mipyme.screen.tickets_settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TicketsSettingsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(TicketsSettingsScreenUiState)
    val stateFlow: StateFlow<TicketsSettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: TicketsSettingsScreenUiState
        get() = _stateFlow.value
}
