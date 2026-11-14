package co.mipyme

import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.data.notification.NotificationPermissionRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.FakeSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
        val viewModel = viewModel(AuthState.Initializing)
        advanceUntilIdle()

        assertEquals(LaunchDestination.Idle, viewModel.uiState.launchDestination)
    }

    @Test
    fun `authenticated account without a merchant goes to onboarding`() = runTest(dispatcher) {
        val viewModel = viewModel(AuthState.Authenticated(account(merchantId = null)))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Onboarding, viewModel.uiState.launchDestination)
    }

    @Test
    fun `authenticated account with a merchant goes home`() = runTest(dispatcher) {
        val viewModel = viewModel(AuthState.Authenticated(account(merchantId = "merch_1")), granted = true)
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `signed out states go to welcome`() = runTest(dispatcher) {
        val signedOutStates = listOf(AuthState.Unauthenticated, AuthState.SessionExpired, AuthState.Blocked)

        signedOutStates.forEach { authState ->
            val viewModel = viewModel(authState)
            advanceUntilIdle()

            assertEquals("state=$authState", LaunchDestination.Welcome, viewModel.uiState.launchDestination)
        }
    }

    @Test
    fun `gaining a merchant moves from onboarding to home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = null)))
        val viewModel = viewModel(authService, granted = true)
        advanceUntilIdle()

        authService.authState.value = AuthState.Authenticated(account(merchantId = "merch_1"))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `merchant not answered and not granted asks for notification permissions`() = runTest(dispatcher) {
        val viewModel = viewModel(AuthState.Authenticated(account(merchantId = "merch_1")), granted = false)
        advanceUntilIdle()

        assertEquals(LaunchDestination.AskNotificationPermissions, viewModel.uiState.launchDestination)
    }

    @Test
    fun `answered prompt goes home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val repository = repository(authService, granted = false)
        repository.markAnswered()
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `answering the prompt moves from notification permissions to home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val repository = repository(authService, granted = false)
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")))
        advanceUntilIdle()
        assertEquals(LaunchDestination.AskNotificationPermissions, viewModel.uiState.launchDestination)

        repository.markAnswered()
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `signing out and back in asks for notification permissions again`() = runTest(dispatcher) {
        val signedIn = AuthState.Authenticated(account(merchantId = "merch_1"))
        val authService = FakeAuthService(signedIn)
        val repository = repository(authService, granted = false)
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")))
        advanceUntilIdle()
        repository.markAnswered()
        advanceUntilIdle()
        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)

        authService.authState.value = AuthState.Unauthenticated
        advanceUntilIdle()
        runCurrent()
        authService.authState.value = signedIn
        advanceUntilIdle()
        runCurrent()

        assertEquals(LaunchDestination.AskNotificationPermissions, viewModel.uiState.launchDestination)
    }

    @Test
    fun `merchant without a completed initial sync goes to initial sync`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            syncEngine = FakeSyncEngine(),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)
    }

    @Test
    fun `completing the initial sync moves on to home`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine()
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            syncEngine = syncEngine,
        )
        advanceUntilIdle()

        syncEngine.completed.value = setOf("merch_1")
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `completing the initial sync asks for notification permissions before home`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine()
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = false,
            syncEngine = syncEngine,
        )
        advanceUntilIdle()
        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)

        syncEngine.completed.value = setOf("merch_1")
        advanceUntilIdle()

        assertEquals(LaunchDestination.AskNotificationPermissions, viewModel.uiState.launchDestination)
    }

    @Test
    fun `signing out and back in shows the initial sync again`() = runTest(dispatcher) {
        val signedIn = AuthState.Authenticated(account(merchantId = "merch_1"))
        val authService = FakeAuthService(signedIn)
        val syncEngine = FakeSyncEngine(completedMerchants = setOf("merch_1"))
        val viewModel = viewModel(authService, granted = true, syncEngine = syncEngine)
        advanceUntilIdle()
        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)

        authService.authState.value = AuthState.Unauthenticated
        advanceUntilIdle()
        syncEngine.completed.value = emptySet()
        authService.authState.value = signedIn
        advanceUntilIdle()

        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)
    }

    @Test
    fun `a sync completed for another merchant still requires the initial sync`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            syncEngine = FakeSyncEngine(completedMerchants = setOf("merch_other")),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)
    }

    @Test
    fun `finishing onboarding goes to initial sync instead of home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = null)))
        val viewModel = viewModel(authService, granted = true, syncEngine = FakeSyncEngine())
        advanceUntilIdle()

        authService.authState.value = AuthState.Authenticated(account(merchantId = "merch_1"))
        advanceUntilIdle()

        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)
    }

    private fun TestScope.viewModel(authState: AuthState, granted: Boolean = false): MainViewModel {
        return viewModel(FakeAuthService(authState), granted)
    }

    private fun TestScope.viewModel(
        authService: FakeAuthService,
        granted: Boolean = false,
        syncEngine: FakeSyncEngine = FakeSyncEngine(completedMerchants = setOf("merch_1")),
    ): MainViewModel {
        return MainViewModel(authService, repository(authService, granted), syncEngine)
    }

    private fun TestScope.repository(
        authService: FakeAuthService,
        granted: Boolean,
    ): NotificationPermissionRepository {
        return NotificationPermissionRepository(
            dataStore = FakePreferencesDataStore(),
            permissionChecker = { granted },
            authService = authService,
            scope = backgroundScope,
            ioDispatcher = dispatcher,
        )
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
}
