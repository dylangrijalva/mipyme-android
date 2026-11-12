package co.mipyme.screen.welcome

enum class WelcomeAction {
    ContinueWithGoogle,
    ContinueWithEmail,
    TermsOfService,
    PrivacyPolicy,
}

data class WelcomeScreenUiState(
    val termsAccepted: Boolean = false,
    val pendingAction: WelcomeAction? = null,
)

sealed interface WelcomeIntent {
    data class TermsToggled(val accepted: Boolean) : WelcomeIntent

    data class ActionSelected(val action: WelcomeAction) : WelcomeIntent

    data object ActionHandled : WelcomeIntent
}
