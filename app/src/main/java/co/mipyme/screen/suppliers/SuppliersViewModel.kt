package co.mipyme.screen.suppliers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SuppliersViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(SuppliersScreenUiState)
    val stateFlow: StateFlow<SuppliersScreenUiState> = _stateFlow.asStateFlow()
    val state: SuppliersScreenUiState
        get() = _stateFlow.value
}
