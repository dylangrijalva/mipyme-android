package co.mipyme.screen.initial_sync

import arrow.core.left
import arrow.core.right
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
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
class InitialSyncViewModelTest {

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
    fun `starts syncing as soon as it is created`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine(setOf("merch_1"), gate = CompletableDeferred())
        val viewModel = viewModel(syncEngine)
        advanceUntilIdle()

        assertEquals(1, syncEngine.synchronizeCalls)
        assertTrue(viewModel.state.isSyncing)
        assertNull(viewModel.state.errorRes)
    }

    @Test
    fun `a successful sync stops loading without an error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1")))
        advanceUntilIdle()

        assertFalse(viewModel.state.isSyncing)
        assertNull(viewModel.state.errorRes)
    }

    @Test
    fun `a successful sync that left the initial sync incomplete shows an error to retry`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine())
        advanceUntilIdle()

        assertEquals(R.string.initial_sync_error_unknown, viewModel.state.errorRes)
        assertFalse(viewModel.state.isSyncing)
    }

    @Test
    fun `an offline launch sync that continued with the cache shows no error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1"), result = Unit.right()))
        advanceUntilIdle()

        assertNull(viewModel.state.errorRes)
        assertFalse(viewModel.state.isSyncing)
    }

    @Test
    fun `a network failure without a cache shows the network error with retry`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(result = SyncError.Network.left()))
        advanceUntilIdle()

        assertEquals(R.string.initial_sync_error_network, viewModel.state.errorRes)
    }

    @Test
    fun `a server failure with a cache shows the error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1"), result = SyncError.Server.left()))
        advanceUntilIdle()

        assertEquals(R.string.initial_sync_error_server, viewModel.state.errorRes)
    }

    @Test
    fun `each sync error maps to its message`() = runTest(dispatcher) {
        val expected = mapOf(
            SyncError.Network to R.string.initial_sync_error_network,
            SyncError.Server to R.string.initial_sync_error_server,
            SyncError.Unauthorized to R.string.initial_sync_error_unauthorized,
            SyncError.Forbidden to R.string.initial_sync_error_forbidden,
            SyncError.Unknown to R.string.initial_sync_error_unknown,
        )

        expected.forEach { (error, message) ->
            val viewModel = viewModel(FakeSyncEngine(result = error.left()))
            advanceUntilIdle()

            assertEquals("error=$error", message, viewModel.state.errorRes)
            assertFalse("error=$error", viewModel.state.isSyncing)
        }
    }

    @Test
    fun `retry synchronizes again and clears the error on success`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine(setOf("merch_1"), result = SyncError.Network.left())
        val viewModel = viewModel(syncEngine)
        advanceUntilIdle()

        syncEngine.result = Unit.right()
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, syncEngine.synchronizeCalls)
        assertNull(viewModel.state.errorRes)
        assertFalse(viewModel.state.isSyncing)
    }

    @Test
    fun `retry shows loading while the new attempt runs`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine(setOf("merch_1"), result = SyncError.Server.left())
        val viewModel = viewModel(syncEngine)
        advanceUntilIdle()

        syncEngine.gate = CompletableDeferred()
        viewModel.onRetry()
        advanceUntilIdle()

        assertTrue(viewModel.state.isSyncing)
        assertNull(viewModel.state.errorRes)
    }

    @Test
    fun `tapping sign out shows the confirmation and dismissing hides it`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1")))
        advanceUntilIdle()

        viewModel.onSignOutClick()
        assertTrue(viewModel.state.isSignOutConfirmationVisible)

        viewModel.onSignOutDismissed()
        assertFalse(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `the sign out confirmation is available while the sync is loading`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1"), gate = CompletableDeferred()))
        advanceUntilIdle()

        viewModel.onSignOutClick()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `confirming signs out and toggles signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            AuthState.Authenticated(account()),
            signOutGate = CompletableDeferred(),
        )
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1")), authService)
        advanceUntilIdle()
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertTrue(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)

        authService.signOutGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `retry is ignored while signing out`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine(setOf("merch_1"), result = SyncError.Network.left())
        val authService = FakeAuthService(
            AuthState.Authenticated(account()),
            signOutGate = CompletableDeferred(),
        )
        val viewModel = viewModel(syncEngine, authService)
        advanceUntilIdle()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(1, syncEngine.synchronizeCalls)
    }

    @Test
    fun `a failed sign out shows a toast message`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            AuthState.Authenticated(account()),
            signOutResult = AuthError.Unknown().left(),
        )
        val viewModel = viewModel(FakeSyncEngine(setOf("merch_1")), authService)
        advanceUntilIdle()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertTrue(viewModel.state.pendingToastRes != null)
        viewModel.onToastShown()
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `retry is ignored while a sync is running`() = runTest(dispatcher) {
        val syncEngine = FakeSyncEngine(setOf("merch_1"), gate = CompletableDeferred())
        val viewModel = viewModel(syncEngine)
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(1, syncEngine.synchronizeCalls)
    }

    private fun viewModel(
        syncEngine: FakeSyncEngine,
        authService: FakeAuthService = FakeAuthService(AuthState.Authenticated(account())),
    ): InitialSyncViewModel {
        return InitialSyncViewModel(syncEngine, authService)
    }

    private fun account(): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Monkey D. Luffy",
            email = null,
            pictureUrl = "",
            publicMetadata = IdentityPublicMetadata(merchantId = "merch_1"),
        )
    }
}
