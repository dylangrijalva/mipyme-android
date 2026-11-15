package co.mipyme.screen.inventory

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventoryViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(InventoryScreenUiState)
    val stateFlow: StateFlow<InventoryScreenUiState> = _stateFlow.asStateFlow()
    val state: InventoryScreenUiState
        get() = _stateFlow.value
}
