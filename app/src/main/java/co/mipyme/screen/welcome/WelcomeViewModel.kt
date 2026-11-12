package co.mipyme.screen.welcome

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WelcomeViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(WelcomeScreenUiState())
    val stateFlow: StateFlow<WelcomeScreenUiState> = _stateFlow.asStateFlow()
    val state: WelcomeScreenUiState
        get() = _stateFlow.value

    fun onIntent(intent: WelcomeIntent) {
        when (intent) {
            is WelcomeIntent.TermsToggled -> {
                _stateFlow.update { it.copy(termsAccepted = intent.accepted) }
            }
            is WelcomeIntent.ActionSelected -> {
                _stateFlow.update { it.copy(pendingAction = intent.action) }
            }
            WelcomeIntent.ActionHandled -> {
                _stateFlow.update { it.copy(pendingAction = null) }
            }
        }
    }
}
