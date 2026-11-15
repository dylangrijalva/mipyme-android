package co.mipyme.screen.business_information_settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BusinessInformationSettingsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(BusinessInformationSettingsScreenUiState)
    val stateFlow: StateFlow<BusinessInformationSettingsScreenUiState> = _stateFlow.asStateFlow()
    val state: BusinessInformationSettingsScreenUiState
        get() = _stateFlow.value
}
