package co.mipyme

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(MainScreenUiState())
    val stateFlow = _stateFlow.asStateFlow()
    val uiState: MainScreenUiState
        get() = _stateFlow.value
}
