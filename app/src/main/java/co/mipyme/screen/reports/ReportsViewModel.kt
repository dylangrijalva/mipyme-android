package co.mipyme.screen.reports

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportsViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(ReportsScreenUiState)
    val stateFlow: StateFlow<ReportsScreenUiState> = _stateFlow.asStateFlow()
    val state: ReportsScreenUiState
        get() = _stateFlow.value
}
