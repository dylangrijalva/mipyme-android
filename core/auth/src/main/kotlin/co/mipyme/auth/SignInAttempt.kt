package co.mipyme.auth

sealed interface SignInAttempt {
    data class ChallengeRequired(val email: String) : SignInAttempt
    data object Completed : SignInAttempt
}
