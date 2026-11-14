package co.mipyme.auth.internal

import co.mipyme.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMappingTest {

    @Test
    fun `invalid account transfer is a recognised error`() {
        val error = mapAuthErrorCode(code = "account_transfer_invalid", isNetworkFailure = false)

        assertEquals(AuthError.AccountTransferInvalid, error)
    }

    @Test
    fun `network failure wins over error code`() {
        val error = mapAuthErrorCode(code = "user_locked", isNetworkFailure = true)

        assertEquals(AuthError.Network, error)
    }

    @Test
    fun `identifier not found maps to invalid email`() {
        assertEquals(
            AuthError.InvalidEmail,
            mapAuthErrorCode(code = "form_identifier_not_found", isNetworkFailure = false),
        )
    }

    @Test
    fun `format invalid maps to invalid email`() {
        assertEquals(
            AuthError.InvalidEmail,
            mapAuthErrorCode(code = "form_param_format_invalid", isNetworkFailure = false),
        )
    }

    @Test
    fun `incorrect code and lockout warning map to invalid otp code`() {
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
    fun `expired verification maps to otp code expired`() {
        assertEquals(
            AuthError.OtpCodeExpired,
            mapAuthErrorCode(code = "verification_expired", isNetworkFailure = false),
        )
    }

    @Test
    fun `failed verification maps to max attempts reached`() {
        assertEquals(
            AuthError.MaxOtpVerificationAttemptsReached,
            mapAuthErrorCode(code = "verification_failed", isNetworkFailure = false),
        )
    }

    @Test
    fun `locked and banned users map to their errors`() {
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
    fun `too many code requests map to resend rate limited`() {
        assertEquals(
            AuthError.OtpResendRateLimited,
            mapAuthErrorCode(code = "verification_code_too_many_requests", isNetworkFailure = false),
        )
    }

    @Test
    fun `unrecognised code maps to unknown`() {
        val error = mapAuthErrorCode(code = "something_new", isNetworkFailure = false)

        assertEquals(
            AuthError.Unknown("An error with the auth provider has happened."),
            error,
        )
    }

    @Test
    fun `missing code maps to unknown`() {
        val error = mapAuthErrorCode(code = null, isNetworkFailure = false)

        assertEquals(
            AuthError.Unknown("An error with the auth provider has happened."),
            error,
        )
    }
}
