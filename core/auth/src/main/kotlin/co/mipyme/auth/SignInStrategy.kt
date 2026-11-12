package co.mipyme.auth

sealed interface SignInStrategy {
    data class EmailCode(val email: String) : SignInStrategy
    data object Google : SignInStrategy
}
