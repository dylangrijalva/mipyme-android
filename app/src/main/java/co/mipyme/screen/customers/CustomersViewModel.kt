package co.mipyme.screen.customers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomersViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(CustomersScreenUiState)
    val stateFlow: StateFlow<CustomersScreenUiState> = _stateFlow.asStateFlow()
    val state: CustomersScreenUiState
        get() = _stateFlow.value
}
