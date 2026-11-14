package co.mipyme.sync.testing

import arrow.core.Either
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SessionReadiness
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthService(initialState: AuthState = AuthState.Initializing) : AuthService {
    val authState = MutableStateFlow(initialState)
    private val otpCooldown = MutableStateFlow(0)
    private val otpCooldownEmail = MutableStateFlow<String?>(null)

    val sessionReadiness = MutableStateFlow<SessionReadiness>(SessionReadiness.Ready)

    override val authStateFlow: StateFlow<AuthState> = authState
    override val sessionReadinessFlow: StateFlow<SessionReadiness> = sessionReadiness
    override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = otpCooldown
    override val otpCooldownEmailFlow: StateFlow<String?> = otpCooldownEmail

    override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> = error("Not used")

    override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> = error("Not used")

    override suspend fun resendOtpCode(): Either<AuthError, Unit> = error("Not used")

    override suspend fun refresh(): Either<AuthError, Unit> = error("Not used")

    override suspend fun signOut(): Either<AuthError, Unit> = error("Not used")

    override suspend fun getToken(): Either<AuthError, String> = error("Not used")
}
