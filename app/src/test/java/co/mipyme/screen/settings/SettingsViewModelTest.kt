package co.mipyme.screen.settings

import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.SyncEmployee
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dataScope = CoroutineScope(dispatcher + SupervisorJob())

    private val owner = syncEmployee("emp_1", emptyList(), isOwner = true)
    private val cashier = syncEmployee("emp_1", emptyList())

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
    fun `sign out click shows the confirmation`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onSignOutClick()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `dismissing hides the confirmation without signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(signOutResult = Unit.right())
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `confirming signs out once and hides the confirmation`() = runTest(dispatcher) {
        val authService = FakeAuthService(signOutResult = Unit.right())
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = FakeAuthService(signOutResult = AuthError.Network.left()))
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `double tap while signing out calls sign out once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(signOutGate = gate)
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `signing out is true while the call is suspended and false afterwards`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(authService = FakeAuthService(signOutGate = gate))
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `dismissing while signing out keeps the confirmation visible`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(authService = FakeAuthService(signOutGate = gate))
        viewModel.onSignOutClick()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        viewModel.onSignOutDismissed()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `owner items remain after the session is no longer authenticated`() = runTest(dispatcher) {
        val authService = authService()
        val viewModel = viewModel(employees = listOf(owner), authService = authService)
        advanceUntilIdle()

        authService.authState.value = AuthState.SessionExpired
        advanceUntilIdle()

        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = FakeAuthService(signOutResult = AuthError.Network.left()))

        viewModel.onSignOutClick()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()
        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `visible items default to profile only`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)
    }

    @Test
    fun `an owner sees every item`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        advanceUntilIdle()

        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    @Test
    fun `a non owner only sees profile`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(cashier))

        advanceUntilIdle()

        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)
    }

    @Test
    fun `visible items follow employee changes`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()

        cache.employees = listOf(cashier)
        advanceUntilIdle()
        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)

        cache.employees = listOf(owner)
        advanceUntilIdle()
        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    private fun viewModel(
        employees: List<SyncEmployee> = emptyList(),
        cache: FakeMipymeCache = FakeMipymeCache(employees = employees),
        authService: FakeAuthService = authService(),
    ): SettingsViewModel {
        return SettingsViewModel(
            authService,
            CurrentEmployeeRepository(authService, cache, dataScope),
        )
    }

    private fun authService(): FakeAuthService {
        return FakeAuthService(
            AuthState.Authenticated(
                IdentityAccount(
                    id = "user_1",
                    name = "Ada Lovelace",
                    email = null,
                    pictureUrl = "https://img.example.com/ada.png",
                    publicMetadata = IdentityPublicMetadata(employeeId = "emp_1"),
                ),
            ),
        )
    }
}
