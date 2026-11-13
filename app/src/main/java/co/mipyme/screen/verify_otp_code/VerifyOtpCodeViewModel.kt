package co.mipyme.screen.verify_otp_code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.toStringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyOtpCodeViewModel(
    email: String,
    private val authService: AuthService,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(
        VerifyOtpCodeScreenUiState(
            email = email,
            resendSecondsRemaining = authService.otpCooldownSecondsRemainingFlow.value,
        ),
    )
    val stateFlow: StateFlow<VerifyOtpCodeScreenUiState> = _stateFlow.asStateFlow()
    val state: VerifyOtpCodeScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            authService.otpCooldownSecondsRemainingFlow.collect { secondsRemaining ->
                _stateFlow.update { it.copy(resendSecondsRemaining = secondsRemaining) }
            }
        }
    }

    fun onCodeChange(code: String) {
        if (state.isVerifying) return
        val normalizedCode = code.filter(Char::isDigit).take(OTP_CODE_LENGTH)
        val shouldVerify = normalizedCode.length == OTP_CODE_LENGTH &&
            normalizedCode != state.code &&
            !state.isBusy

        _stateFlow.update {
            it.copy(code = normalizedCode, wasCodeResent = false, error = null)
        }

        if (shouldVerify) {
            onVerify()
        }
    }

    fun onVerify() {
        if (!state.isVerifyEnabled) return
        _stateFlow.update { it.copy(isVerifying = true, wasCodeResent = false, error = null) }
        viewModelScope.launch {
            authService.completeChallenge(state.code).fold(
                ifLeft = { error -> showError(error) },
                ifRight = { attempt ->
                    when (attempt) {
                        SignInAttempt.Completed -> Unit
                        is SignInAttempt.ChallengeRequired -> showError(AuthError.Unknown())
                    }
                },
            )
        }
    }

    fun onResend() {
        if (!state.isResendEnabled) return
        _stateFlow.update { it.copy(isResending = true, wasCodeResent = false, error = null) }
        viewModelScope.launch {
            authService.resendOtpCode().fold(
                ifLeft = { error -> showError(error) },
                ifRight = {
                    _stateFlow.update { it.copy(isResending = false, wasCodeResent = true) }
                },
            )
        }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    fun onDialogDismissed() {
        _stateFlow.update { it.copy(blockingErrorRes = null) }
    }

    private fun showError(error: AuthError) {
        _stateFlow.update {
            val idle = it.copy(isVerifying = false, isResending = false)
            when (error) {
                AuthError.InvalidOtpCode,
                AuthError.OtpCodeExpired,
                AuthError.MaxOtpVerificationAttemptsReached,
                AuthError.MissingOtpChallenge -> idle.copy(error = error)
                AuthError.UserLocked,
                AuthError.UserBanned -> idle.copy(blockingErrorRes = error.toStringResource())
                else -> idle.copy(pendingToastRes = error.toStringResource())
            }
        }
    }
}
