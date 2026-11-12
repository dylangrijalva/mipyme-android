package co.mipyme.screen.welcome

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.auth.toStringResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful google sign in shows no toast and clears signing in`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = SignInAttempt.Completed.right())
        val viewModel = WelcomeViewModel(authService)

        viewModel.onGoogleSignIn()
        advanceUntilIdle()

        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningIn)
    }

    @Test
    fun `oauth cancelled shows no toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = AuthError.OAuthCancelled.left())
        val viewModel = WelcomeViewModel(authService)

        viewModel.onGoogleSignIn()
        advanceUntilIdle()

        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningIn)
    }

    @Test
    fun `other sign in error shows mapped toast`() = runTest(dispatcher) {
        val error = AuthError.Network
        val authService = FakeAuthService(result = error.left())
        val viewModel = WelcomeViewModel(authService)

        viewModel.onGoogleSignIn()
        advanceUntilIdle()

        assertEquals(error.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningIn)
    }

    @Test
    fun `double tap while signing in calls sign in once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Either<AuthError, SignInAttempt>>()
        val authService = FakeAuthService(gate = gate)
        val viewModel = WelcomeViewModel(authService)

        viewModel.onGoogleSignIn()
        viewModel.onGoogleSignIn()
        advanceUntilIdle()

        assertEquals(1, authService.signInCalls)
        gate.complete(SignInAttempt.Completed.right())
        advanceUntilIdle()
    }

    @Test
    fun `signing in is true while the call is suspended and false afterwards`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Either<AuthError, SignInAttempt>>()
        val authService = FakeAuthService(gate = gate)
        val viewModel = WelcomeViewModel(authService)

        viewModel.onGoogleSignIn()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningIn)

        gate.complete(SignInAttempt.Completed.right())
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningIn)
    }

    private class FakeAuthService(
        private val result: Either<AuthError, SignInAttempt>? = null,
        private val gate: CompletableDeferred<Either<AuthError, SignInAttempt>>? = null,
    ) : AuthService {
        var signInCalls = 0
            private set

        override val authStateFlow: StateFlow<AuthState> = MutableStateFlow(AuthState.Initializing)
        override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> {
            signInCalls++
            return gate?.await() ?: checkNotNull(result)
        }

        override suspend fun getToken(): Either<AuthError, String> = error("Not used")
        override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> = error("Not used")
        override suspend fun resendOtpCode(): Either<AuthError, Unit> = error("Not used")
        override suspend fun refresh(): Either<AuthError, Unit> = error("Not used")
        override suspend fun signOut(): Either<AuthError, Unit> = error("Not used")
    }
}
