package co.mipyme.auth.internal

import co.mipyme.auth.AuthError

internal fun mapAuthErrorCode(code: String?, isNetworkFailure: Boolean): AuthError {
    if (isNetworkFailure) return AuthError.Network
    return when (code) {
        "account_transfer_invalid" -> AuthError.AccountTransferInvalid
        "form_identifier_not_found", "form_param_format_invalid" -> AuthError.InvalidEmail
        "form_code_incorrect", "account_lockout_warning" -> AuthError.InvalidOtpCode
        "verification_expired" -> AuthError.OtpCodeExpired
        "verification_failed" -> AuthError.MaxOtpVerificationAttemptsReached
        "user_locked" -> AuthError.UserLocked
        "user_banned" -> AuthError.UserBanned
        "verification_code_too_many_requests" -> AuthError.OtpResendRateLimited
        else -> AuthError.Unknown("An error with the auth provider has happened.")
    }
}
