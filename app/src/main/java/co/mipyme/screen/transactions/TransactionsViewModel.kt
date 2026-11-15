package co.mipyme.screen.transactions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(TransactionsScreenUiState)
    val stateFlow: StateFlow<TransactionsScreenUiState> = _stateFlow.asStateFlow()
    val state: TransactionsScreenUiState
        get() = _stateFlow.value
}
