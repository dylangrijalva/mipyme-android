package co.mipyme.auth.internal

import co.mipyme.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMappingTest {

    @Test
    fun network_failure_wins_over_error_code() {
        val error = mapAuthErrorCode(code = "user_locked", isNetworkFailure = true)

        assertEquals(AuthError.Network, error)
    }

    @Test
    fun identifier_not_found_maps_to_invalid_email() {
        assertEquals(
            AuthError.InvalidEmail,
            mapAuthErrorCode(code = "form_identifier_not_found", isNetworkFailure = false),
        )
    }

    @Test
    fun format_invalid_maps_to_invalid_email() {
        assertEquals(
            AuthError.InvalidEmail,
            mapAuthErrorCode(code = "form_param_format_invalid", isNetworkFailure = false),
        )
    }

    @Test
    fun incorrect_code_and_lockout_warning_map_to_invalid_otp_code() {
        assertEquals(
            AuthError.InvalidOtpCode,
            mapAuthErrorCode(code = "form_code_incorrect", isNetworkFailure = false),
        )
        assertEquals(
            AuthError.InvalidOtpCode,
            mapAuthErrorCode(code = "account_lockout_warning", isNetworkFailure = false),
        )
    }

    @Test
    fun expired_verification_maps_to_otp_code_expired() {
        assertEquals(
            AuthError.OtpCodeExpired,
            mapAuthErrorCode(code = "verification_expired", isNetworkFailure = false),
        )
    }

    @Test
    fun failed_verification_maps_to_max_attempts_reached() {
        assertEquals(
            AuthError.MaxOtpVerificationAttemptsReached,
            mapAuthErrorCode(code = "verification_failed", isNetworkFailure = false),
        )
    }

    @Test
    fun locked_and_banned_users_map_to_their_errors() {
        assertEquals(
            AuthError.UserLocked,
            mapAuthErrorCode(code = "user_locked", isNetworkFailure = false),
        )
        assertEquals(
            AuthError.UserBanned,
            mapAuthErrorCode(code = "user_banned", isNetworkFailure = false),
        )
    }

    @Test
    fun too_many_code_requests_map_to_resend_rate_limited() {
        assertEquals(
            AuthError.OtpResendRateLimited,
            mapAuthErrorCode(code = "verification_code_too_many_requests", isNetworkFailure = false),
        )
    }

    @Test
    fun unrecognised_code_maps_to_unknown() {
        val error = mapAuthErrorCode(code = "something_new", isNetworkFailure = false)

        assertEquals(
            AuthError.Unknown("An error with the auth provider has happened."),
            error,
        )
    }

    @Test
    fun missing_code_maps_to_unknown() {
        val error = mapAuthErrorCode(code = null, isNetworkFailure = false)

        assertEquals(
            AuthError.Unknown("An error with the auth provider has happened."),
            error,
        )
    }
}
