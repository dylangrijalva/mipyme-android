package co.mipyme.data.notification

import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakePreferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPermissionRepositoryTest {

    private val account = IdentityAccount(
        id = "user_1",
        name = "Monkey D. Luffy",
        email = "luffy@example.com",
        pictureUrl = "",
        publicMetadata = IdentityPublicMetadata(merchantId = "merch_1"),
    )

    private fun createRepository(
        authService: FakeAuthService,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        granted: Boolean = false,
        dataStore: FakePreferencesDataStore = FakePreferencesDataStore(),
    ): NotificationPermissionRepository {
        return NotificationPermissionRepository(
            dataStore = dataStore,
            permissionChecker = { granted },
            authService = authService,
            scope = scope,
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun `is not answered by default`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(
            FakeAuthService(AuthState.Authenticated(account)),
            backgroundScope,
            dispatcher,
        )
        runCurrent()

        assertFalse(repository.answeredFlow.first())
    }

    @Test
    fun `mark answered persists`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(
            FakeAuthService(AuthState.Authenticated(account)),
            backgroundScope,
            dispatcher,
        )
        runCurrent()

        repository.markAnswered()
        runCurrent()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `staying authenticated keeps the answer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val repository = createRepository(authService, backgroundScope, dispatcher)
        runCurrent()
        repository.markAnswered()

        authService.authState.value = AuthState.Authenticated(account.copy(name = "Roronoa Zoro"))
        runCurrent()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `initializing keeps the answer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val repository = createRepository(authService, backgroundScope, dispatcher)
        runCurrent()
        repository.markAnswered()

        authService.authState.value = AuthState.Initializing
        runCurrent()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `leaving authenticated clears the answer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val signedOutStates = listOf(AuthState.Unauthenticated, AuthState.SessionExpired, AuthState.Blocked)

        signedOutStates.forEach { signedOut ->
            val authService = FakeAuthService(AuthState.Authenticated(account))
            val repository = createRepository(authService, backgroundScope, dispatcher)
            runCurrent()
            repository.markAnswered()

            authService.authState.value = signedOut
            runCurrent()

            assertFalse("state=$signedOut", repository.answeredFlow.first())
        }
    }

    @Test
    fun `permission status comes from the checker`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val granted = createRepository(FakeAuthService(), backgroundScope, dispatcher, granted = true)
        val denied = createRepository(FakeAuthService(), backgroundScope, dispatcher, granted = false)

        assertTrue(granted.isPermissionGranted())
        assertFalse(denied.isPermissionGranted())
    }

    @Test
    fun `mark answered still reports answered when the data store fails to read and write`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dataStore = FakePreferencesDataStore(failReads = true, failWrites = true)
        val repository = createRepository(
            FakeAuthService(AuthState.Authenticated(account)),
            backgroundScope,
            dispatcher,
            dataStore = dataStore,
        )
        runCurrent()
        assertFalse(repository.answeredFlow.first())

        repository.markAnswered()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `mark answered reports answered when only the write fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(
            FakeAuthService(AuthState.Authenticated(account)),
            backgroundScope,
            dispatcher,
            dataStore = FakePreferencesDataStore(failWrites = true),
        )

        repository.markAnswered()

        assertTrue(repository.answeredFlow.first())
    }

    @Test
    fun `leaving authenticated does not crash when the data store fails to write`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val repository = createRepository(
            authService,
            backgroundScope,
            dispatcher,
            dataStore = FakePreferencesDataStore(failWrites = true),
        )
        runCurrent()
        repository.markAnswered()

        authService.authState.value = AuthState.Unauthenticated
        runCurrent()

        assertFalse(repository.answeredFlow.first())
    }
}
