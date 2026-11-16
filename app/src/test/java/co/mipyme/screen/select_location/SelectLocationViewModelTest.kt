package co.mipyme.screen.select_location

import arrow.core.left
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.testing.FakeAuthService
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
import kotlinx.coroutines.flow.first
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
class SelectLocationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val locationScope = CoroutineScope(dispatcher + SupervisorJob())
    private val cache = FakeMipymeCache(
        locations = listOf(
            location("loc_1", name = "Downtown", address = "Main St 1"),
            location("loc_2", name = "Airport", address = null),
        ),
        employees = listOf(syncEmployee("emp_1", emptyList(), isOwner = true)),
    )

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
    fun `exposes the accessible locations with their addresses`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(ownerState()))
        advanceUntilIdle()

        assertEquals(
            listOf(
                SelectLocationItem("loc_1", "Downtown", "Main St 1"),
                SelectLocationItem("loc_2", "Airport", null),
            ),
            viewModel.state.locations,
        )
    }

    @Test
    fun `updates the list when the accessible locations change`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(ownerState()))
        advanceUntilIdle()

        cache.locations = listOf(location("loc_3", name = "Warehouse"))
        advanceUntilIdle()

        assertEquals(listOf("loc_3"), viewModel.state.locations.map { it.id })
    }

    @Test
    fun `clicking a location selects it`() = runTest(dispatcher) {
        val authService = FakeAuthService(ownerState())
        val repository = repository(authService)
        val viewModel = SelectLocationViewModel(repository, authService)
        advanceUntilIdle()

        viewModel.onLocationClick("loc_2")
        advanceUntilIdle()

        assertEquals("loc_2", repository.selectedLocationFlow.first()?.id)
        assertTrue(viewModel.state.isSelectionComplete)
        assertFalse(viewModel.state.isSelecting)
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `choosing the only business again completes selection`() = runTest(dispatcher) {
        cache.locations = listOf(location("loc_1"))
        val authService = FakeAuthService(ownerState())
        val repository = repository(authService)
        advanceUntilIdle()
        repository.select("loc_1")
        advanceUntilIdle()
        val viewModel = SelectLocationViewModel(repository, authService)
        advanceUntilIdle()
        assertFalse(viewModel.state.isSelectionComplete)

        viewModel.onLocationClick("loc_1")
        advanceUntilIdle()

        assertTrue(viewModel.state.isSelectionComplete)
        assertEquals("loc_1", repository.selectedLocationFlow.first()?.id)
    }

    @Test
    fun `clicking an inaccessible location shows an error toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(ownerState())
        val repository = repository(authService)
        val viewModel = SelectLocationViewModel(repository, authService)
        advanceUntilIdle()

        viewModel.onLocationClick("missing")
        advanceUntilIdle()

        assertEquals(R.string.select_location_error, viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSelectionComplete)
        assertNull(repository.selectedLocationFlow.first())
        assertFalse(viewModel.state.isSelecting)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(ownerState()))
        advanceUntilIdle()
        viewModel.onLocationClick("missing")
        advanceUntilIdle()

        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `sign out asks for confirmation before signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(ownerState())
        val viewModel = viewModel(authService)

        viewModel.onSignOutClick()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `dismissing the confirmation hides it without signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(ownerState())
        val viewModel = viewModel(authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutDismissed()

        assertFalse(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `confirming signs out once and tracks progress`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(ownerState(), signOutGate = gate)
        val viewModel = viewModel(authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertFalse(viewModel.state.isSigningOut)
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `a failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(ownerState(), signOutResult = AuthError.Network.left())
        val viewModel = viewModel(authService)

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    private fun TestScope.viewModel(authService: FakeAuthService): SelectLocationViewModel {
        return SelectLocationViewModel(repository(authService), authService)
    }

    private fun TestScope.repository(authService: FakeAuthService): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = FakePreferencesDataStore(),
            authService = authService,
            cache = cache,
            scope = locationScope,
        )
    }

    private fun ownerState(): AuthState {
        return AuthState.Authenticated(
            IdentityAccount(
                id = "user_1",
                name = "Monkey D. Luffy",
                email = "luffy@example.com",
                pictureUrl = "",
                publicMetadata = IdentityPublicMetadata(
                    merchantId = "merch_1",
                    employeeId = "emp_1",
                ),
            ),
        )
    }
}
