package co.mipyme.auth

import arrow.core.Either
import co.mipyme.contract.IdentityAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthService : TokenProvider {
    val authStateFlow: StateFlow<AuthState>
    val otpCooldownSecondsRemainingFlow: StateFlow<Int>
    suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt>
    suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt>
    suspend fun resendOtpCode(): Either<AuthError, Unit>
    suspend fun refresh(): Either<AuthError, Unit>
    suspend fun signOut(): Either<AuthError, Unit>
}
