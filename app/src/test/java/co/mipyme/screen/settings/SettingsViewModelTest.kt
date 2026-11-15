package co.mipyme.screen.settings

import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.toStringResource
import co.mipyme.testing.FakeAuthService
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
class SettingsViewModelTest {

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
        val authService = FakeAuthService(signOutResult = Unit.right())
        val viewModel = SettingsViewModel(authService)

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeAuthService(signOutResult = AuthError.Network.left()))

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `double tap while signing out calls sign out once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(signOutGate = gate)
        val viewModel = SettingsViewModel(authService)

        viewModel.onSignOut()
        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `signing out is true while the call is suspended and false afterwards`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = SettingsViewModel(FakeAuthService(signOutGate = gate))

        viewModel.onSignOut()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeAuthService(signOutResult = AuthError.Network.left()))

        viewModel.onSignOut()
        advanceUntilIdle()
        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }
}
