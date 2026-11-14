package co.mipyme.auth

import androidx.annotation.StringRes

sealed interface AuthError {
    data class Unknown(val message: String? = null) : AuthError
    data object InvalidOtpCode : AuthError
    data object InvalidEmail : AuthError
    data object OtpCodeExpired : AuthError
    data object UserLocked : AuthError
    data object UserBanned : AuthError
    data object MaxOtpVerificationAttemptsReached : AuthError
    data object OtpResendRateLimited : AuthError
    data object MissingOtpChallenge : AuthError
    data object AccountTransferInvalid : AuthError
    data object OAuthCancelled : AuthError
    data object Network : AuthError
    data object NotAuthenticated : AuthError
}

@StringRes
fun AuthError.toStringResource(): Int {
    return when (this) {
        is AuthError.Unknown -> R.string.auth_error_unknown
        AuthError.InvalidOtpCode -> R.string.auth_error_invalid_otp_code
        AuthError.InvalidEmail -> R.string.auth_error_invalid_email
        AuthError.OtpCodeExpired -> R.string.auth_error_otp_code_expired
        AuthError.UserLocked -> R.string.auth_error_user_locked
        AuthError.UserBanned -> R.string.auth_error_user_banned
        AuthError.MaxOtpVerificationAttemptsReached -> R.string.auth_error_max_otp_attempts
        AuthError.OtpResendRateLimited -> R.string.auth_error_otp_resend_rate_limited
        AuthError.MissingOtpChallenge -> R.string.auth_error_missing_otp_challenge
        AuthError.AccountTransferInvalid -> R.string.auth_error_account_transfer_invalid
        AuthError.OAuthCancelled -> R.string.auth_error_unknown
        AuthError.Network -> R.string.auth_error_network
        AuthError.NotAuthenticated -> R.string.auth_error_not_authenticated
    }
}
