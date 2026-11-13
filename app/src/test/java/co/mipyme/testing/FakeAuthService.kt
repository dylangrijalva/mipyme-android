package co.mipyme.testing

import arrow.core.Either
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthService(
    initialState: AuthState = AuthState.Initializing,
    var refreshResult: Either<AuthError, Unit> = Unit.right(),
    var signOutResult: Either<AuthError, Unit> = Unit.right(),
    var signOutGate: CompletableDeferred<Unit>? = null,
) : AuthService {
    var refreshCalls = 0
        private set
    var signOutCalls = 0
        private set

    val authState = MutableStateFlow(initialState)

    override val authStateFlow: StateFlow<AuthState> = authState
    override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = MutableStateFlow(0)

    override suspend fun refresh(): Either<AuthError, Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun signOut(): Either<AuthError, Unit> {
        signOutCalls++
        signOutGate?.await()
        return signOutResult
    }

    override suspend fun getToken(): Either<AuthError, String> = error("Not used")
    override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> = error("Not used")
    override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> = error("Not used")
    override suspend fun resendOtpCode(): Either<AuthError, Unit> = error("Not used")
}
