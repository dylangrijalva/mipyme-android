package co.mipyme.screen.home

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SessionReadiness
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.auth.toStringResource
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.location
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
    private val locationScope = CoroutineScope(dispatcher + SupervisorJob())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        locationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `successful sign out shows no toast and clears signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = Unit.right())
        val viewModel = viewModel(authService)

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(result = AuthError.Network.left())
        val viewModel = viewModel(authService)

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `double tap while signing out calls sign out once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Either<AuthError, Unit>>()
        val authService = FakeAuthService(gate = gate)
        val viewModel = viewModel(authService)

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
        val viewModel = viewModel(FakeAuthService(gate = gate))

        viewModel.onSignOut()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)

        gate.complete(Unit.right())
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `authenticated account email is exposed`() = runTest(dispatcher) {
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Authenticated(account(email = "ada@example.com")))
        val viewModel = viewModel(FakeAuthService(result = Unit.right(), authStateFlow = authStateFlow))

        advanceUntilIdle()

        assertEquals("ada@example.com", viewModel.state.email)
    }

    @Test
    fun `authenticated account email is available before collection starts`() = runTest(dispatcher) {
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Authenticated(account(email = "ada@example.com")))
        val viewModel = viewModel(FakeAuthService(result = Unit.right(), authStateFlow = authStateFlow))

        assertEquals("ada@example.com", viewModel.state.email)
    }

    @Test
    fun `email is cleared when no longer authenticated`() = runTest(dispatcher) {
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Authenticated(account(email = "ada@example.com")))
        val viewModel = viewModel(FakeAuthService(result = Unit.right(), authStateFlow = authStateFlow))
        advanceUntilIdle()

        authStateFlow.value = AuthState.Unauthenticated
        advanceUntilIdle()

        assertNull(viewModel.state.email)
    }

    @Test
    fun `account without email exposes no email`() = runTest(dispatcher) {
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Authenticated(account(email = null)))
        val viewModel = viewModel(FakeAuthService(result = Unit.right(), authStateFlow = authStateFlow))

        advanceUntilIdle()

        assertNull(viewModel.state.email)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(result = AuthError.Network.left()))

        viewModel.onSignOut()
        advanceUntilIdle()
        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `the selected location name is exposed`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            result = Unit.right(),
            authStateFlow = MutableStateFlow(AuthState.Authenticated(account(email = null))),
        )
        val cache = FakeMipymeCache(
            locations = listOf(location("loc_1", name = "Downtown"), location("loc_2")),
            employees = listOf(syncEmployee("emp_1", emptyList(), isOwner = true)),
        )
        val repository = locationRepository(authService, cache)
        val viewModel = HomeViewModel(authService, repository)
        advanceUntilIdle()
        assertNull(viewModel.state.locationName)

        repository.select("loc_1")
        advanceUntilIdle()

        assertEquals("Downtown", viewModel.state.locationName)
    }

    private fun TestScope.viewModel(authService: FakeAuthService): HomeViewModel {
        return HomeViewModel(authService, locationRepository(authService, FakeMipymeCache()))
    }

    private fun TestScope.locationRepository(
        authService: FakeAuthService,
        cache: FakeMipymeCache,
    ): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = FakePreferencesDataStore(),
            authService = authService,
            cache = cache,
            scope = locationScope,
        )
    }

    private fun account(email: String?): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Ada Lovelace",
            email = email,
            pictureUrl = "https://img.example.com/ada.png",
            publicMetadata = IdentityPublicMetadata(employeeId = "emp_1"),
        )
    }

    private class FakeAuthService(
        private val result: Either<AuthError, Unit>? = null,
        private val gate: CompletableDeferred<Either<AuthError, Unit>>? = null,
        override val authStateFlow: StateFlow<AuthState> = MutableStateFlow(AuthState.Initializing),
    ) : AuthService {
        var signOutCalls = 0
            private set

        override val sessionReadinessFlow: StateFlow<SessionReadiness> = MutableStateFlow(SessionReadiness.Ready)
        override val otpCooldownSecondsRemainingFlow: StateFlow<Int> = MutableStateFlow(0)
        override val otpCooldownEmailFlow: StateFlow<String?> = MutableStateFlow(null)

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
