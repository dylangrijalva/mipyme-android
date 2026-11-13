package co.mipyme.screen.welcome

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.SignInStrategy
import co.mipyme.auth.toStringResource
import co.mipyme.network.MipymeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class WelcomeViewModel(
    private val authService: AuthService,
    private val api: MipymeApi,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(WelcomeScreenUiState())
    val stateFlow: StateFlow<WelcomeScreenUiState> = _stateFlow.asStateFlow()
    val state: WelcomeScreenUiState
        get() = _stateFlow.value

    init {
        checkApiHealth()
        fetchCurrentUser()
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _stateFlow.update { it.copy(termsAccepted = accepted) }
    }

    fun onGoogleSignIn() {
        if (state.isSigningIn) return
        _stateFlow.update { it.copy(isSigningIn = true) }
        viewModelScope.launch {
            val result = authService.signIn(SignInStrategy.Google)
            _stateFlow.update { it.copy(isSigningIn = false) }
            result.onLeft { error ->
                if (error != AuthError.OAuthCancelled) {
                    showToast(error.toStringResource())
                }
            }
        }
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

    private fun checkApiHealth() {
        viewModelScope.launch {
            if (api.isHealthy()) {
                Timber.i("The API is reachable and healthy")
            } else {
                Timber.w("The API health check failed")
                showToast(R.string.welcome_api_unreachable)
            }
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            api.accounts.currentUser().fold(
                ifLeft = { error -> Timber.w("GET /me failed: %s", error) },
                ifRight = { Timber.i("GET /me succeeded") },
            )
        }
    }

    private fun showToast(@StringRes labelRes: Int) {
        _stateFlow.update { it.copy(pendingToastRes = labelRes) }
    }
}
