package co.mipyme.screen.no_location_access

import arrow.core.left
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.sync.SyncError
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeSyncEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NoLocationAccessViewModelTest {

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
    fun `does not sync until asked to refresh`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine()
        NoLocationAccessViewModel(syncEngine, FakeAuthService(AuthState.Unauthenticated))
        advanceUntilIdle()

        assertEquals(0, syncEngine.synchronizeCalls)
    }

    @Test
    fun `refreshing synchronizes and tracks progress`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val syncEngine = FakeSyncEngine(gate = gate)
        val viewModel = NoLocationAccessViewModel(syncEngine, FakeAuthService(AuthState.Unauthenticated))

        viewModel.onRefresh()
        advanceUntilIdle()
        assertTrue(viewModel.state.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, syncEngine.synchronizeCalls)
        assertFalse(viewModel.state.isRefreshing)
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `refreshing again while in progress synchronizes once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val syncEngine = FakeSyncEngine(gate = gate)
        val viewModel = NoLocationAccessViewModel(syncEngine, FakeAuthService(AuthState.Unauthenticated))

        viewModel.onRefresh()
        viewModel.onRefresh()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, syncEngine.synchronizeCalls)
    }

    @Test
    fun `a failed refresh shows the mapped error toast`() = runTest(dispatcher) {
        val errors = mapOf(
            SyncError.Network to R.string.no_location_access_error_network,
            SyncError.Server to R.string.no_location_access_error_server,
            SyncError.Unauthorized to R.string.no_location_access_error_unauthorized,
            SyncError.Forbidden to R.string.no_location_access_error_forbidden,
            SyncError.Unknown to R.string.no_location_access_error_unknown,
        )

        errors.forEach { (error, expected) ->
            val viewModel = NoLocationAccessViewModel(
                FakeSyncEngine(result = error.left()),
                FakeAuthService(AuthState.Unauthenticated),
            )

            viewModel.onRefresh()
            advanceUntilIdle()

            assertEquals("error=$error", expected, viewModel.state.pendingToastRes)
            assertFalse(viewModel.state.isRefreshing)
        }
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = NoLocationAccessViewModel(
            FakeSyncEngine(result = SyncError.Network.left()),
            FakeAuthService(AuthState.Unauthenticated),
        )
        viewModel.onRefresh()
        advanceUntilIdle()

        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `sign out asks for confirmation before signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Unauthenticated)
        val viewModel = NoLocationAccessViewModel(FakeSyncEngine(), authService)

        viewModel.onSignOutClick()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `dismissing the confirmation hides it without signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Unauthenticated)
        val viewModel = NoLocationAccessViewModel(FakeSyncEngine(), authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutDismissed()

        assertFalse(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `confirming signs out once and tracks progress`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(AuthState.Unauthenticated, signOutGate = gate)
        val viewModel = NoLocationAccessViewModel(FakeSyncEngine(), authService)
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
    }

    @Test
    fun `a failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Unauthenticated, signOutResult = AuthError.Network.left())
        val viewModel = NoLocationAccessViewModel(FakeSyncEngine(), authService)

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `refreshing is ignored while signing out`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val syncEngine = FakeSyncEngine()
        val viewModel = NoLocationAccessViewModel(
            syncEngine,
            FakeAuthService(AuthState.Unauthenticated, signOutGate = gate),
        )
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        viewModel.onRefresh()
        advanceUntilIdle()

        assertEquals(0, syncEngine.synchronizeCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }
}
