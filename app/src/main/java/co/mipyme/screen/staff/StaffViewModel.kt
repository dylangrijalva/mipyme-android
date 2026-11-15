package co.mipyme.screen.staff

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StaffViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(StaffScreenUiState)
    val stateFlow: StateFlow<StaffScreenUiState> = _stateFlow.asStateFlow()
    val state: StaffScreenUiState
        get() = _stateFlow.value
}
