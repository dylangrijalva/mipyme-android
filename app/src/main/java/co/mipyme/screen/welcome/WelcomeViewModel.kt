package co.mipyme.screen.welcome

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import co.mipyme.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WelcomeViewModel : ViewModel() {
    private val _stateFlow = MutableStateFlow(WelcomeScreenUiState())
    val stateFlow: StateFlow<WelcomeScreenUiState> = _stateFlow.asStateFlow()
    val state: WelcomeScreenUiState
        get() = _stateFlow.value

    fun onTermsAcceptedChange(accepted: Boolean) {
        _stateFlow.update { it.copy(termsAccepted = accepted) }
    }

    fun onGoogleSignIn() {
        showToast(R.string.welcome_continue_with_google)
    }

    fun onEmailSignIn() {
        showToast(R.string.welcome_continue_with_email)
    }

    fun onTermsClick() {
        showToast(R.string.welcome_terms_of_service)
    }

    fun onPrivacyClick() {
        showToast(R.string.welcome_privacy_policy)
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    private fun showToast(@StringRes labelRes: Int) {
        _stateFlow.update { it.copy(pendingToastRes = labelRes) }
    }
}
