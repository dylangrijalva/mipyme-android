package co.mipyme.screen.checkout

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CheckoutViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(CheckoutScreenUiState)
    val stateFlow: StateFlow<CheckoutScreenUiState> = _stateFlow.asStateFlow()
    val state: CheckoutScreenUiState
        get() = _stateFlow.value
}
