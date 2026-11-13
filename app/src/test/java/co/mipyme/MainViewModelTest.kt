package co.mipyme

import arrow.core.Either
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

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
    fun `starts idle before the auth state is known`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeAuthService(AuthState.Initializing))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Idle, viewModel.uiState.launchDestination)
    }

    @Test
    fun `authenticated account without a merchant goes to onboarding`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeAuthService(AuthState.Authenticated(account(merchantId = null))))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Onboarding, viewModel.uiState.launchDestination)
    }

    @Test
    fun `authenticated account with a merchant goes home`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `signed out states go to welcome`() = runTest(dispatcher) {
        val signedOutStates = listOf(AuthState.Unauthenticated, AuthState.SessionExpired, AuthState.Blocked)

        signedOutStates.forEach { authState ->
            val viewModel = MainViewModel(FakeAuthService(authState))
            advanceUntilIdle()

            assertEquals("state=$authState", LaunchDestination.Welcome, viewModel.uiState.launchDestination)
        }
    }

    @Test
    fun `gaining a merchant moves from onboarding to home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = null)))
        val viewModel = MainViewModel(authService)
        advanceUntilIdle()

        authService.authState.value = AuthState.Authenticated(account(merchantId = "merch_1"))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    private fun account(merchantId: String?): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Monkey D. Luffy",
            email = "luffy@example.com",
            pictureUrl = "",
            publicMetadata = IdentityPublicMetadata(merchantId = merchantId),
        )
    }

    private class FakeAuthService(initial: AuthState) : AuthService {
        val authState = MutableStateFlow(initial)

        override val authStateFlow: StateFlow<AuthState> = authState
        override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun getToken(): Either<AuthError, String> = error("Not used")
        override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> = error("Not used")
        override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> = error("Not used")
        override suspend fun resendOtpCode(): Either<AuthError, Unit> = error("Not used")
        override suspend fun refresh(): Either<AuthError, Unit> = error("Not used")
        override suspend fun signOut(): Either<AuthError, Unit> = error("Not used")
    }
}
