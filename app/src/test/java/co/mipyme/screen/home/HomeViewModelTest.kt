package co.mipyme.screen.home

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
class HomeViewModelTest {

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
    fun `successful sign out shows no toast and clears signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = Unit.right())
        val viewModel = HomeViewModel(authService)

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = AuthError.Network.left())
        val viewModel = HomeViewModel(authService)

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `double tap while signing out calls sign out once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Either<AuthError, Unit>>()
        val authService = FakeAuthService(gate = gate)
        val viewModel = HomeViewModel(authService)

        viewModel.onSignOut()
        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        gate.complete(Unit.right())
        advanceUntilIdle()
    }

    @Test
    fun `signing out is true while the call is suspended and false afterwards`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Either<AuthError, Unit>>()
        val viewModel = HomeViewModel(FakeAuthService(gate = gate))

        viewModel.onSignOut()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)

        gate.complete(Unit.right())
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeAuthService(result = AuthError.Network.left()))

        viewModel.onSignOut()
        advanceUntilIdle()
        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    private class FakeAuthService(
        private val result: Either<AuthError, Unit>? = null,
        private val gate: CompletableDeferred<Either<AuthError, Unit>>? = null,
    ) : AuthService {
        var signOutCalls = 0
            private set

        override val authStateFlow: StateFlow<AuthState> = MutableStateFlow(AuthState.Initializing)
        override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun signOut(): Either<AuthError, Unit> {
            signOutCalls++
            return gate?.await() ?: checkNotNull(result)
        }

        override suspend fun getToken(): Either<AuthError, String> = error("Not used")
        override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> = error("Not used")
        override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> = error("Not used")
        override suspend fun resendOtpCode(): Either<AuthError, Unit> = error("Not used")
        override suspend fun refresh(): Either<AuthError, Unit> = error("Not used")
    }
}
