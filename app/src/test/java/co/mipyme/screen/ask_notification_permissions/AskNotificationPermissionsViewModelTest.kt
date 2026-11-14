package co.mipyme.screen.ask_notification_permissions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.mipyme.auth.AuthState
import co.mipyme.data.notification.NotificationPermissionRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakePreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskNotificationPermissionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository(scope: TestScope): NotificationPermissionRepository {
        return NotificationPermissionRepository(
            dataStore = FakePreferencesDataStore(),
            permissionChecker = { false },
            authService = FakeAuthService(AuthState.Initializing),
            scope = scope.backgroundScope,
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun `permission result marks the prompt answered`() = runTest(dispatcher) {
        val repository = repository(this)
        val viewModel = AskNotificationPermissionsViewModel(repository)

        viewModel.onPermissionResult()
        advanceUntilIdle()

        assertTrue(repository.answeredFlow.first())
        assertFalse(viewModel.state.isResponding)
    }

    @Test
    fun `request then permission result marks the prompt answered`() = runTest(dispatcher) {
        val repository = repository(this)
        val viewModel = AskNotificationPermissionsViewModel(repository)

        assertTrue(viewModel.onRequestPermission())
        viewModel.onPermissionResult()
        advanceUntilIdle()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `skip marks the prompt answered`() = runTest(dispatcher) {
        val repository = repository(this)
        val viewModel = AskNotificationPermissionsViewModel(repository)

        viewModel.onSkip()
        advanceUntilIdle()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `stays responding after the answer is saved`() = runTest(dispatcher) {
        val viewModel = AskNotificationPermissionsViewModel(repository(this))

        viewModel.onSkip()

        assertTrue(viewModel.state.isResponding)
        advanceUntilIdle()
        assertTrue(viewModel.state.isResponding)
    }

    @Test
    fun `second request is rejected while responding`() = runTest(dispatcher) {
        val viewModel = AskNotificationPermissionsViewModel(repository(this))

        assertTrue(viewModel.onRequestPermission())
        assertFalse(viewModel.onRequestPermission())
    }

    @Test
    fun `skip is ignored while a request is in progress`() = runTest(dispatcher) {
        val repository = repository(this)
        val viewModel = AskNotificationPermissionsViewModel(repository)

        viewModel.onRequestPermission()
        viewModel.onSkip()
        advanceUntilIdle()

        assertFalse(repository.answeredFlow.first())
    }

    @Test
    fun `second skip and permission result save once`() = runTest(dispatcher) {
        val dataStore = CountingDataStore()
        val repository = NotificationPermissionRepository(
            dataStore = dataStore,
            permissionChecker = { false },
            authService = FakeAuthService(AuthState.Initializing),
            scope = backgroundScope,
            ioDispatcher = dispatcher,
        )
        val viewModel = AskNotificationPermissionsViewModel(repository)

        viewModel.onSkip()
        viewModel.onSkip()
        viewModel.onPermissionResult()
        advanceUntilIdle()

        assertEquals(1, dataStore.writes)
    }

    private class CountingDataStore(
        private val delegate: FakePreferencesDataStore = FakePreferencesDataStore(),
    ) : DataStore<Preferences> {
        var writes = 0
            private set

        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            writes++
            return delegate.updateData(transform)
        }
    }
}
