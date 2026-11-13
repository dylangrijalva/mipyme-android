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
    var signInResult: Either<AuthError, SignInAttempt>? = null,
    var completeChallengeResult: Either<AuthError, SignInAttempt>? = null,
    var resendOtpCodeResult: Either<AuthError, Unit>? = null,
    var requestGate: CompletableDeferred<Unit>? = null,
) : AuthService {
    var refreshCalls = 0
        private set
    var signOutCalls = 0
        private set
    val signInStrategies = mutableListOf<SignInStrategy>()
    val completedCodes = mutableListOf<String>()
    var resendOtpCodeCalls = 0
        private set

    val authState = MutableStateFlow(initialState)

    val otpCooldown = MutableStateFlow(0)

    override val authStateFlow: StateFlow<AuthState> = authState
    override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = otpCooldown

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

    override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> {
        signInStrategies.add(strategy)
        requestGate?.await()
        return checkNotNull(signInResult)
    }

    override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> {
        completedCodes.add(code)
        requestGate?.await()
        return checkNotNull(completeChallengeResult)
    }

    override suspend fun resendOtpCode(): Either<AuthError, Unit> {
        resendOtpCodeCalls++
        requestGate?.await()
        return checkNotNull(resendOtpCodeResult)
    }
}
