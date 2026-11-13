package co.mipyme.screen.sign_in_with_email

import androidx.annotation.StringRes
import co.mipyme.auth.AuthError
import co.mipyme.util.isValidEmail

data class SignInWithEmailScreenUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val pendingChallengeEmail: String? = null,
    val otpCooldownSecondsRemaining: Int = 0,
    val cooldownEmail: String? = null,
    @param:StringRes val pendingToastRes: Int? = null,
    @param:StringRes val blockingErrorRes: Int? = null,
) {
    val isContinueEnabled: Boolean
        get() = isValidEmail(email) && !isLoading

    val showsCooldownHint: Boolean
        get() = otpCooldownSecondsRemaining > 0 &&
            cooldownEmail != null &&
            email.trim().equals(cooldownEmail, ignoreCase = true)
}
