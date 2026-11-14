package co.mipyme.screen.sign_in_with_email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.auth.toStringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInWithEmailViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(SignInWithEmailScreenUiState())
    val stateFlow: StateFlow<SignInWithEmailScreenUiState> = _stateFlow.asStateFlow()
    val state: SignInWithEmailScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            authService.otpCooldownSecondsRemainingFlow.collect { cooldown ->
                _stateFlow.update { it.copy(otpCooldownSecondsRemaining = cooldown) }
            }
        }
        viewModelScope.launch {
            authService.otpCooldownEmailFlow.collect { email ->
                _stateFlow.update { it.copy(cooldownEmail = email) }
            }
        }
    }

    fun onEmailChange(email: String) {
        _stateFlow.update { it.copy(email = email, error = null) }
    }

    fun onSubmit() {
        if (!state.isContinueEnabled) return
        if (state.showsCooldownHint) {
            _stateFlow.update { it.copy(error = null, pendingChallengeEmail = it.cooldownEmail) }
            return
        }
        _stateFlow.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authService.signIn(SignInStrategy.EmailCode(state.email.trim())).fold(
                ifLeft = { error -> showError(error) },
                ifRight = { attempt ->
                    when (attempt) {
                        is SignInAttempt.ChallengeRequired -> {
                            _stateFlow.update {
                                it.copy(
                                    isLoading = false,
                                    pendingChallengeEmail = attempt.email,
                                )
                            }
                        }
                        SignInAttempt.Completed -> Unit
                    }
                },
            )
        }
    }

    fun consumeChallenge() {
        _stateFlow.update { it.copy(pendingChallengeEmail = null) }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    fun onDialogDismissed() {
        _stateFlow.update { it.copy(blockingErrorRes = null) }
    }

    private fun showError(error: AuthError) {
        _stateFlow.update {
            when (error) {
                AuthError.InvalidEmail,
                AuthError.OtpResendRateLimited -> it.copy(isLoading = false, error = error)
                AuthError.UserLocked,
                AuthError.UserBanned -> it.copy(isLoading = false, blockingErrorRes = error.toStringResource())
                else -> it.copy(isLoading = false, pendingToastRes = error.toStringResource())
            }
        }
    }
}
