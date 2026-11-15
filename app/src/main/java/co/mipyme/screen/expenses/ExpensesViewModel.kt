package co.mipyme.screen.expenses

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExpensesViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(ExpensesScreenUiState)
    val stateFlow: StateFlow<ExpensesScreenUiState> = _stateFlow.asStateFlow()
    val state: ExpensesScreenUiState
        get() = _stateFlow.value
}
