package co.mipyme.screen.welcome

import androidx.annotation.StringRes

data class WelcomeScreenUiState(
    val termsAccepted: Boolean = false,
    val showEmailSignInRecovery: Boolean = false,
    val isSigningIn: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
