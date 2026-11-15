package co.mipyme.screen.printers_and_devices_settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrintersAndDevicesSettingsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(PrintersAndDevicesSettingsScreenUiState)
    val stateFlow: StateFlow<PrintersAndDevicesSettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: PrintersAndDevicesSettingsScreenUiState
        get() = _stateFlow.value
}
