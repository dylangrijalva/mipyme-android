package co.mipyme

import co.mipyme.auth.AuthState
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.Location
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.data.notification.NotificationPermissionRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.FakeSyncEngine
import co.mipyme.testing.location
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

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
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")), locationRepository(authService))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `answering the prompt moves from notification permissions to home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val repository = repository(authService, granted = false)
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")), locationRepository(authService))
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
        val viewModel = MainViewModel(authService, repository, FakeSyncEngine(setOf("merch_1")), locationRepository(authService))
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

    @Test
    fun `staff with exactly one accessible location select it and go home without a screen`() = runTest(dispatcher) {
        val seen = mutableListOf<LaunchDestination>()
        val viewModel = viewModel(AuthState.Authenticated(account(merchantId = "merch_1")), granted = true)
        val job = launch { viewModel.stateFlow.collect { seen += it.launchDestination } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
        assertFalse(seen.contains(LaunchDestination.SelectLocation))
        assertFalse(seen.contains(LaunchDestination.NoLocationAccess))
    }

    @Test
    fun `staff with several accessible locations choose one`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(
            locations = listOf(location("loc_1"), location("loc_2")),
            employees = listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2"))),
        )
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)
    }

    @Test
    fun `an owner with exactly one location still chooses it`() = runTest(dispatcher) {
        val cache = ownerCache(listOf(location("loc_1")))
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)
    }

    @Test
    fun `an owner goes home once a location is selected`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val cache = ownerCache(listOf(location("loc_1"), location("loc_2")))
        val locationRepository = locationRepository(authService, cache)
        val viewModel = MainViewModel(
            authService,
            repository(authService, granted = true),
            FakeSyncEngine(setOf("merch_1")),
            locationRepository,
        )
        advanceUntilIdle()
        assertEquals(LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)

        locationRepository.select("loc_2")
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `a stored selection goes straight home for an owner`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val cache = ownerCache(listOf(location("loc_1"), location("loc_2")))
        val dataStore = FakePreferencesDataStore()
        locationRepository(authService, cache, dataStore).select("loc_2")
        advanceUntilIdle()

        val viewModel = MainViewModel(
            authService,
            repository(authService, granted = true),
            FakeSyncEngine(setOf("merch_1")),
            locationRepository(authService, cache, dataStore),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `staff without an employee record have no location access`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(locations = listOf(location("loc_1")))
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `staff whose employee is disabled or invited have no location access`() = runTest(dispatcher) {
        listOf(EmployeeStatus.DISABLED, EmployeeStatus.INVITED).forEach { status ->
            val cache = FakeMipymeCache(
                locations = listOf(location("loc_1")),
                employees = listOf(syncEmployee("emp_1", listOf("loc_1"), status)),
            )
            val viewModel = viewModel(
                FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
                granted = true,
                cache = cache,
            )
            advanceUntilIdle()

            assertEquals("status=$status", LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
        }
    }

    @Test
    fun `staff without locations assigned have no location access`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(
            locations = listOf(location("loc_1")),
            employees = listOf(syncEmployee("emp_1", emptyList())),
        )
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `an owner without any location has no location access`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = ownerCache(emptyList()),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `gaining a location leaves no location access`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(locations = listOf(location("loc_1")))
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()
        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)

        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_1")))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `losing the selected location routes through the location step again`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(
            locations = listOf(location("loc_1"), location("loc_2")),
            employees = listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2"))),
        )
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val locationRepository = locationRepository(authService, cache)
        val viewModel = MainViewModel(
            authService,
            repository(authService, granted = true),
            FakeSyncEngine(setOf("merch_1")),
            locationRepository,
        )
        advanceUntilIdle()
        locationRepository.select("loc_1")
        advanceUntilIdle()
        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)

        cache.locations = listOf(location("loc_1"), location("loc_2"), location("loc_3"))
        advanceUntilIdle()
        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_2", "loc_3")))
        advanceUntilIdle()

        assertEquals(LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)
    }

    @Test
    fun `losing every location moves from home to no location access`() = runTest(dispatcher) {
        val cache = defaultCache()
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()
        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)

        cache.employees = listOf(syncEmployee("emp_1", emptyList()))
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `the location step waits for the initial sync and the notification prompt`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine()
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = false,
            syncEngine = syncEngine,
            cache = FakeMipymeCache(),
        )
        advanceUntilIdle()
        assertEquals(LaunchDestination.InitialSync, viewModel.uiState.launchDestination)

        syncEngine.completed.value = setOf("merch_1")
        advanceUntilIdle()

        assertEquals(LaunchDestination.AskNotificationPermissions, viewModel.uiState.launchDestination)
    }

    @Test
    fun `switching to another employee does not reuse the selection`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(
            locations = listOf(location("loc_1"), location("loc_2")),
            employees = listOf(
                syncEmployee("emp_1", listOf("loc_1")),
                syncEmployee("emp_2", listOf("loc_1", "loc_2")),
            ),
        )
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val viewModel = viewModel(authService, granted = true, cache = cache)
        advanceUntilIdle()
        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)

        authService.authState.value = AuthState.Authenticated(account(merchantId = "merch_1", employeeId = "emp_2"))
        advanceUntilIdle()

        assertEquals(LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)
    }

    @Test
    fun `an owner whose employee record is missing has no location access`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = FakeMipymeCache(locations = listOf(location("loc_1")), employees = listOf(syncEmployee("other", emptyList(), isOwner = true))),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `a disabled or invited owner still chooses a location`() = runTest(dispatcher) {
        listOf(EmployeeStatus.DISABLED, EmployeeStatus.INVITED).forEach { status ->
            val viewModel = viewModel(
                FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
                granted = true,
                cache = FakeMipymeCache(
                    locations = listOf(location("loc_1")),
                    employees = listOf(syncEmployee("emp_1", emptyList(), status, isOwner = true)),
                ),
            )
            advanceUntilIdle()

            assertEquals("status=$status", LaunchDestination.SelectLocation, viewModel.uiState.launchDestination)
        }
    }

    @Test
    fun `an account without an employee id has no location access`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1", employeeId = null))),
            granted = true,
            cache = ownerCache(listOf(location("loc_1"))),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.NoLocationAccess, viewModel.uiState.launchDestination)
    }

    @Test
    fun `an unrelated cache emission does not move a persisted staff selection`() = runTest(dispatcher) {
        val cache = defaultCache()
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = cache,
        )
        advanceUntilIdle()

        cache.locations = listOf(location("loc_1"), location("loc_9"))
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `staff whose selection cannot be persisted still go home`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1")))
        val repository = LocationSelectionRepository(
            dataStore = FakePreferencesDataStore(failWrites = true),
            authService = authService,
            cache = defaultCache(),
            scope = locationScope,
        )
        val viewModel = MainViewModel(authService, repository(authService, granted = true), FakeSyncEngine(setOf("merch_1")), repository)
        advanceUntilIdle()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
    }

    @Test
    fun `completing the initial sync before the cache has synced data never shows no location access`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine()
        val cache = FakeMipymeCache(merchantId = null)
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            syncEngine = syncEngine,
            cache = cache,
        )
        val seen = mutableListOf<LaunchDestination>()
        val job = launch { viewModel.stateFlow.collect { seen += it.launchDestination } }
        advanceUntilIdle()

        syncEngine.completed.value = setOf("merch_1")
        advanceUntilIdle()
        assertFalse(seen.contains(LaunchDestination.NoLocationAccess))

        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_1")))
        cache.locations = listOf(location("loc_1"))
        cache.merchantId = "merch_1"
        advanceUntilIdle()
        job.cancel()

        assertEquals(LaunchDestination.Home, viewModel.uiState.launchDestination)
        assertFalse(seen.contains(LaunchDestination.NoLocationAccess))
        assertFalse(seen.contains(LaunchDestination.SelectLocation))
    }

    @Test
    fun `location data synced for another merchant is not used`() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakeAuthService(AuthState.Authenticated(account(merchantId = "merch_1"))),
            granted = true,
            cache = FakeMipymeCache(
                locations = listOf(location("loc_1")),
                employees = listOf(syncEmployee("emp_1", listOf("loc_1"))),
                merchantId = "merch_other",
            ),
        )
        advanceUntilIdle()

        assertEquals(LaunchDestination.Idle, viewModel.uiState.launchDestination)
    }

    private fun TestScope.viewModel(authState: AuthState, granted: Boolean = false): MainViewModel {
        return viewModel(FakeAuthService(authState), granted)
    }

    private fun TestScope.viewModel(
        authService: FakeAuthService,
        granted: Boolean = false,
        syncEngine: FakeSyncEngine = FakeSyncEngine(completedMerchants = setOf("merch_1")),
        cache: FakeMipymeCache = defaultCache(),
    ): MainViewModel {
        return MainViewModel(authService, repository(authService, granted), syncEngine, locationRepository(authService, cache))
    }

    private fun TestScope.locationRepository(
        authService: FakeAuthService,
        cache: FakeMipymeCache = this@MainViewModelTest.defaultCache(),
        dataStore: FakePreferencesDataStore = FakePreferencesDataStore(),
    ): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = dataStore,
            authService = authService,
            cache = cache,
            scope = locationScope,
        )
    }

    private fun ownerCache(locations: List<Location>): FakeMipymeCache {
        return FakeMipymeCache(
            locations = locations,
            employees = listOf(syncEmployee("emp_1", emptyList(), isOwner = true)),
        )
    }

    private fun defaultCache(): FakeMipymeCache {
        return FakeMipymeCache(
            locations = listOf(location("loc_1")),
            employees = listOf(syncEmployee("emp_1", listOf("loc_1"))),
        )
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

    private fun account(
        merchantId: String?,
        employeeId: String? = "emp_1",
    ): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Monkey D. Luffy",
            email = "luffy@example.com",
            pictureUrl = "",
            publicMetadata = IdentityPublicMetadata(merchantId = merchantId, employeeId = employeeId),
        )
    }
}
