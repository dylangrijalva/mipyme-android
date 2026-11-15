package co.mipyme.screen.home

import androidx.lifecycle.SavedStateHandle
import co.mipyme.auth.AuthState
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.SyncEmployee
import co.mipyme.contract.PosPermission
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.location
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val dataScope = CoroutineScope(dispatcher + SupervisorJob())

    private val owner = syncEmployee("emp_1", emptyList(), isOwner = true)
    private val cashier = syncEmployee("emp_1", emptyList(), permissions = listOf(PosPermission.CHECKOUT))
    private val manager = syncEmployee(
        "emp_1",
        emptyList(),
        permissions = listOf(PosPermission.MANAGE_CATALOG, PosPermission.VIEW_TRANSACTIONS),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        dataScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `authenticated account email is exposed`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = authService(email = "ada@example.com"))

        advanceUntilIdle()

        assertEquals("ada@example.com", viewModel.state.email)
    }

    @Test
    fun `authenticated account email is available before collection starts`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = authService(email = "ada@example.com"))

        assertEquals("ada@example.com", viewModel.state.email)
    }

    @Test
    fun `email is cleared when no longer authenticated`() = runTest(dispatcher) {
        val authService = authService(email = "ada@example.com")
        val viewModel = viewModel(authService = authService)
        advanceUntilIdle()

        authService.authState.value = AuthState.Unauthenticated
        advanceUntilIdle()

        assertNull(viewModel.state.email)
    }

    @Test
    fun `account without email exposes no email`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = authService(email = null))

        advanceUntilIdle()

        assertNull(viewModel.state.email)
    }

    @Test
    fun `the selected location name is exposed`() = runTest(dispatcher) {
        val authService = authService()
        val cache = FakeMipymeCache(listOf(location("loc_1", name = "Downtown"), location("loc_2")), listOf(owner))
        val repository = locationRepository(authService, cache)
        val viewModel = HomeViewModel(
            SavedStateHandle(),
            authService,
            repository,
            CurrentEmployeeRepository(authService, cache, dataScope),
        )
        advanceUntilIdle()
        assertNull(viewModel.state.locationName)

        repository.select("loc_1")
        advanceUntilIdle()

        assertEquals("Downtown", viewModel.state.locationName)
    }

    @Test
    fun `the back stack is empty before the current employee is known`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        assertEquals(emptyList<HomeDestination>(), viewModel.state.backStack)
        assertEquals(emptyList<HomeDestination>(), viewModel.state.visibleDestinations)
    }

    @Test
    fun `an owner starts on checkout and sees every destination`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        advanceUntilIdle()

        assertEquals(HomeDestination.entries, viewModel.state.visibleDestinations)
        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
    }

    @Test
    fun `the anchor is the first visible destination`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(manager))

        advanceUntilIdle()

        assertEquals(
            listOf(HomeDestination.INVENTORY, HomeDestination.TRANSACTIONS, HomeDestination.SETTINGS),
            viewModel.state.visibleDestinations,
        )
        assertEquals(listOf(HomeDestination.INVENTORY), viewModel.state.backStack)
    }

    @Test
    fun `a missing employee starts on settings`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = emptyList())

        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.SETTINGS), viewModel.state.visibleDestinations)
        assertEquals(listOf(HomeDestination.SETTINGS), viewModel.state.backStack)
        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `selecting a destination stacks it above the anchor`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()

        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS), viewModel.state.backStack)
    }

    @Test
    fun `selecting another destination replaces the previous one above the anchor`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()

        viewModel.onDestinationSelected(HomeDestination.REPORTS)
        viewModel.onDestinationSelected(HomeDestination.STAFF)

        assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.STAFF), viewModel.state.backStack)
    }

    @Test
    fun `selecting the anchor resets the stack to the anchor`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        viewModel.onDestinationSelected(HomeDestination.CHECKOUT)

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
    }

    @Test
    fun `selecting the current destination keeps the stack`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS), viewModel.state.backStack)
    }

    @Test
    fun `selecting a destination that is not visible is ignored`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(cashier))
        advanceUntilIdle()

        viewModel.onDestinationSelected(HomeDestination.STAFF)

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
    }

    @Test
    fun `selecting a destination before the employee is known is ignored`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        viewModel.onDestinationSelected(HomeDestination.SETTINGS)

        assertEquals(emptyList<HomeDestination>(), viewModel.state.backStack)
    }

    @Test
    fun `back pops to the anchor`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        viewModel.onBack()

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
    }

    @Test
    fun `back on the anchor keeps the stack`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))
        advanceUntilIdle()

        viewModel.onBack()

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
    }

    @Test
    fun `back before the employee is known keeps the stack empty`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        viewModel.onBack()

        assertEquals(emptyList<HomeDestination>(), viewModel.state.backStack)
    }

    @Test
    fun `a still visible current destination stays when the visible set changes`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.TRANSACTIONS)

        cache.employees = listOf(manager)
        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.INVENTORY, HomeDestination.TRANSACTIONS), viewModel.state.backStack)
        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `a current destination that becomes the anchor leaves a single entry`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.INVENTORY)

        cache.employees = listOf(manager)
        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.INVENTORY), viewModel.state.backStack)
    }

    @Test
    fun `losing access to the current destination goes to the new anchor and reports the loss`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.STAFF)

        cache.employees = listOf(cashier)
        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
        assertEquals(HomeDestination.STAFF, viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `losing every permission moves to settings`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(cashier))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()

        cache.employees = listOf(syncEmployee("emp_1", emptyList(), EmployeeStatus.DISABLED))
        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.SETTINGS), viewModel.state.backStack)
        assertEquals(HomeDestination.CHECKOUT, viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `an unrelated employee change keeps the stack`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        cache.employees = listOf(owner.copy(name = "Renamed"))
        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS), viewModel.state.backStack)
        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `the lost access toast is cleared once shown`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.STAFF)
        cache.employees = listOf(cashier)
        advanceUntilIdle()

        viewModel.onLostAccessToastShown()

        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `the back stack is saved when it changes`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(employees = listOf(owner), savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        viewModel.onDestinationSelected(HomeDestination.REPORTS)

        assertEquals(arrayListOf("CHECKOUT", "REPORTS"), savedStateHandle.get<ArrayList<String>>("back_stack"))
    }

    @Test
    fun `a saved back stack is restored once the employee is known`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("back_stack" to arrayListOf("CHECKOUT", "REPORTS")))
        val viewModel = viewModel(employees = listOf(owner), savedStateHandle = savedStateHandle)
        assertEquals(emptyList<HomeDestination>(), viewModel.state.backStack)

        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS), viewModel.state.backStack)
        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `a saved destination that is no longer visible falls back to the anchor and reports the loss`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("back_stack" to arrayListOf("CHECKOUT", "STAFF")))
        val viewModel = viewModel(employees = listOf(cashier), savedStateHandle = savedStateHandle)

        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
        assertEquals(HomeDestination.STAFF, viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `an unknown saved destination is ignored`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("back_stack" to arrayListOf("CHECKOUT", "RETIRED")))
        val viewModel = viewModel(employees = listOf(owner), savedStateHandle = savedStateHandle)

        advanceUntilIdle()

        assertEquals(listOf(HomeDestination.CHECKOUT), viewModel.state.backStack)
        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `losing the session keeps the stack and reports no lost access`() = runTest(dispatcher) {
        listOf(AuthState.SessionExpired, AuthState.Unauthenticated, AuthState.Blocked).forEach { authState ->
            val authService = authService()
            val viewModel = viewModel(employees = listOf(owner), authService = authService)
            advanceUntilIdle()
            viewModel.onDestinationSelected(HomeDestination.REPORTS)

            authService.authState.value = authState
            advanceUntilIdle()

            assertEquals(
                "authState=$authState",
                listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS),
                viewModel.state.backStack,
            )
            assertNull("authState=$authState", viewModel.state.pendingLostAccessDestination)
        }
    }

    @Test
    fun `a session that expires and recovers before collection keeps the stack and reports no lost access`() =
        runTest(dispatcher) {
            val authService = authService()
            val viewModel = viewModel(employees = listOf(owner), authService = authService)
            advanceUntilIdle()
            viewModel.onDestinationSelected(HomeDestination.REPORTS)
            val authenticated = authService.authState.value

            authService.authState.value = AuthState.SessionExpired
            authService.authState.value = authenticated
            advanceUntilIdle()

            assertEquals(listOf(HomeDestination.CHECKOUT, HomeDestination.REPORTS), viewModel.state.backStack)
            assertNull(viewModel.state.pendingLostAccessDestination)
        }

    @Test
    fun `the lost access toast is not set again by later emissions once shown`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()
        viewModel.onDestinationSelected(HomeDestination.STAFF)
        cache.employees = listOf(cashier)
        advanceUntilIdle()
        viewModel.onLostAccessToastShown()

        cache.employees = listOf(cashier.copy(name = "Renamed"))
        advanceUntilIdle()
        cache.employees = listOf(owner)
        advanceUntilIdle()

        assertNull(viewModel.state.pendingLostAccessDestination)
    }

    @Test
    fun `an owner can edit the business information`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        advanceUntilIdle()

        assertTrue(viewModel.state.canEditBusinessInformation)
    }

    @Test
    fun `a non owner cannot edit the business information`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(manager))

        advanceUntilIdle()

        assertFalse(viewModel.state.canEditBusinessInformation)
    }

    @Test
    fun `a missing employee cannot edit the business information`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = emptyList())

        advanceUntilIdle()

        assertFalse(viewModel.state.canEditBusinessInformation)
    }

    @Test
    fun `losing ownership revokes business information editing`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()

        cache.employees = listOf(cashier)
        advanceUntilIdle()

        assertFalse(viewModel.state.canEditBusinessInformation)
    }

    @Test
    fun `losing the session keeps business information editing`() = runTest(dispatcher) {
        val authService = authService()
        val viewModel = viewModel(employees = listOf(owner), authService = authService)
        advanceUntilIdle()

        authService.authState.value = AuthState.SessionExpired
        advanceUntilIdle()

        assertTrue(viewModel.state.canEditBusinessInformation)
    }

    private fun TestScope.viewModel(
        employees: List<SyncEmployee> = emptyList(),
        cache: FakeMipymeCache = FakeMipymeCache(employees = employees),
        authService: FakeAuthService = authService(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): HomeViewModel {
        return HomeViewModel(
            savedStateHandle,
            authService,
            locationRepository(authService, cache),
            CurrentEmployeeRepository(authService, cache, dataScope),
        )
    }

    private fun locationRepository(
        authService: FakeAuthService,
        cache: FakeMipymeCache,
    ): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = FakePreferencesDataStore(),
            authService = authService,
            cache = cache,
            scope = dataScope,
        )
    }

    private fun authService(email: String? = null): FakeAuthService {
        return FakeAuthService(
            AuthState.Authenticated(
                IdentityAccount(
                    id = "user_1",
                    name = "Ada Lovelace",
                    email = email,
                    pictureUrl = "https://img.example.com/ada.png",
                    publicMetadata = IdentityPublicMetadata(employeeId = "emp_1"),
                ),
            ),
        )
    }
}
