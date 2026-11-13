package co.mipyme.screen.verify_otp_code

import androidx.annotation.StringRes
import co.mipyme.auth.AuthError

const val OTP_CODE_LENGTH = 6

data class VerifyOtpCodeScreenUiState(
    val email: String = "",
    val code: String = "",
    val resendSecondsRemaining: Int = 0,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val wasCodeResent: Boolean = false,
    val error: AuthError? = null,
    @param:StringRes val pendingToastRes: Int? = null,
    @param:StringRes val blockingErrorRes: Int? = null,
) {
    val isBusy: Boolean
        get() = isVerifying || isResending

    val isVerifyEnabled: Boolean
        get() = code.length == OTP_CODE_LENGTH && !isBusy

    val isResendEnabled: Boolean
        get() = resendSecondsRemaining == 0 && !isBusy
}
