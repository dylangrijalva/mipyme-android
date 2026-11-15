package co.mipyme.data.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import co.mipyme.auth.AuthState
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.location
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionRepositoryTest {

    private val downtown = location("loc_1", name = "Downtown")
    private val airport = location("loc_2", name = "Airport")
    private val warehouse = location("loc_3", name = "Warehouse")

    private val owner = syncEmployee("emp_1", emptyList(), isOwner = true)

    @Test
    fun `an owner can access every cached location`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown, airport, warehouse), listOf(owner)))

        assertEquals(listOf(downtown, airport, warehouse), repository.accessibleLocationsFlow.first())
    }

    @Test
    fun `an owner with one location is not auto selected`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown), listOf(owner)))

        assertEquals(listOf(downtown), repository.accessibleLocationsFlow.first())
        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `a non owner can access only the locations assigned to their employee`() = runTest {
        val repository = repository(
            authenticated(),
            FakeMipymeCache(listOf(downtown, airport, warehouse), listOf(syncEmployee("emp_1", listOf("loc_1", "loc_3")))),
        )

        assertEquals(listOf(downtown, warehouse), repository.accessibleLocationsFlow.first())
        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `a missing own employee record gives no access to an owner or staff`() = runTest {
        val cache = FakeMipymeCache(listOf(downtown), listOf(syncEmployee("other", listOf("loc_1"), isOwner = true)))
        val repository = repository(authenticated(), cache)

        assertEquals(emptyList<Any>(), repository.accessibleLocationsFlow.first())
    }

    @Test
    fun `a non owner that is not enabled has no access`() = runTest {
        listOf(EmployeeStatus.DISABLED, EmployeeStatus.INVITED).forEach { status ->
            val repository = repository(
                authenticated(),
                FakeMipymeCache(listOf(downtown), listOf(syncEmployee("emp_1", listOf("loc_1"), status))),
            )

            assertEquals("status=$status", emptyList<Any>(), repository.accessibleLocationsFlow.first())
        }
    }

    @Test
    fun `an owner has every location whatever their status`() = runTest {
        listOf(EmployeeStatus.DISABLED, EmployeeStatus.INVITED).forEach { status ->
            val repository = repository(
                authenticated(),
                FakeMipymeCache(listOf(downtown, airport), listOf(syncEmployee("emp_1", emptyList(), status, isOwner = true))),
            )

            assertEquals("status=$status", listOf(downtown, airport), repository.accessibleLocationsFlow.first())
            assertNull("status=$status", repository.selectedLocationFlow.first())
        }
    }

    @Test
    fun `an account without an employee id has no access and the account id is not used`() = runTest {
        val repository = repository(
            authenticated(employeeId = null),
            FakeMipymeCache(listOf(downtown), listOf(syncEmployee("user_1", listOf("loc_1"), isOwner = true))),
        )

        assertEquals(LocationSelectionState(), repository.locationSelectionFlow.first())
        assertEquals(Either.Left(LocationSelectionError.NotAccessible), repository.select("loc_1"))
    }

    @Test
    fun `a non owner with exactly one location resolves and persists it`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val cache = FakeMipymeCache(listOf(downtown, airport), listOf(syncEmployee("emp_1", listOf("loc_2"))))
        val repository = repository(authenticated(), cache, dataStore)

        assertEquals(airport, repository.selectedLocationFlow.first())
        runCurrent()
        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2")))
        runCurrent()
        assertEquals(airport, repository.selectedLocationFlow.first())

        val restored = repository(authenticated(), cache, dataStore)
        assertEquals(airport, restored.selectedLocationFlow.first())
    }

    @Test
    fun `a failed persist keeps the resolved location and does not retry on unrelated emissions`() = runTest {
        val dataStore = CountingDataStore(FakePreferencesDataStore(failWrites = true))
        val cache = FakeMipymeCache(listOf(downtown), listOf(syncEmployee("emp_1", listOf("loc_1"))))
        val repository = repository(authenticated(), cache, dataStore)
        runCurrent()
        val attempts = dataStore.updates

        cache.locations = listOf(downtown.copy(address = "New address"))
        runCurrent()
        cache.locations = listOf(downtown.copy(address = "Newer address"))
        runCurrent()

        assertEquals("Newer address", repository.selectedLocationFlow.first()?.address)
        assertTrue(attempts >= 1)
        assertEquals(attempts, dataStore.updates)
    }

    @Test
    fun `selecting an accessible location selects it`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown, airport), listOf(owner)))

        val result = repository.select("loc_2")
        runCurrent()

        assertTrue(result.isRight())
        assertEquals(airport, repository.selectedLocationFlow.first())
    }

    @Test
    fun `selecting an inaccessible location is rejected and keeps the current selection`() = runTest {
        val repository = repository(
            authenticated(),
            FakeMipymeCache(listOf(downtown, airport, warehouse), listOf(syncEmployee("emp_1", listOf("loc_1", "loc_3")))),
        )
        repository.select("loc_1")
        runCurrent()

        val unassigned = repository.select("loc_2")
        val unknown = repository.select("missing")
        runCurrent()

        assertEquals(Either.Left(LocationSelectionError.NotAccessible), unassigned)
        assertEquals(Either.Left(LocationSelectionError.NotAccessible), unknown)
        assertEquals(downtown, repository.selectedLocationFlow.first())
    }

    @Test
    fun `selecting while signed out is rejected`() = runTest {
        val repository = repository(AuthState.Unauthenticated, FakeMipymeCache(listOf(downtown), listOf(owner)))

        assertEquals(Either.Left(LocationSelectionError.NotAccessible), repository.select("loc_1"))
    }

    @Test
    fun `selecting reports a failure when the selection cannot be persisted`() = runTest {
        val repository = repository(
            authenticated(),
            FakeMipymeCache(listOf(downtown, airport), listOf(owner)),
            FakePreferencesDataStore(failWrites = true),
        )

        assertEquals(Either.Left(LocationSelectionError.PersistFailed), repository.select("loc_1"))
    }

    @Test
    fun `clearing the selection leaves no location selected`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown, airport), listOf(owner)))
        assertTrue(repository.select("loc_2").isRight())
        runCurrent()
        assertEquals(airport, repository.selectedLocationFlow.first())

        val result = repository.clearSelection()
        runCurrent()

        assertTrue(result.isRight())
        assertNull(repository.selectedLocationFlow.first())
        assertEquals(listOf(downtown, airport), repository.accessibleLocationsFlow.first())
    }

    @Test
    fun `clearing an owner selection with one location does not auto select it again`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown), listOf(owner)))
        assertTrue(repository.select("loc_1").isRight())
        runCurrent()
        assertEquals(downtown, repository.selectedLocationFlow.first())

        repository.clearSelection()
        runCurrent()

        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `clearing reports a failure when the selection cannot be persisted`() = runTest {
        val repository = repository(
            authenticated(),
            FakeMipymeCache(listOf(downtown, airport), listOf(owner)),
            FakePreferencesDataStore(failWrites = true),
        )

        assertEquals(Either.Left(LocationSelectionError.PersistFailed), repository.clearSelection())
    }

    @Test
    fun `a selection is not shared with another employee`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val cache = FakeMipymeCache(
            listOf(downtown, airport),
            listOf(owner, syncEmployee("emp_2", emptyList(), isOwner = true)),
        )
        val authService = FakeAuthService(authenticated(employeeId = "emp_1"))
        val repository = repository(authService, cache, dataStore)
        repository.select("loc_1")
        runCurrent()

        authService.authState.value = authenticated(employeeId = "emp_2")
        runCurrent()
        assertNull(repository.selectedLocationFlow.first())

        authService.authState.value = authenticated(employeeId = "emp_1")
        runCurrent()
        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `a stored selection survives a new repository for the same employee`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val cache = FakeMipymeCache(listOf(downtown, airport), listOf(owner))
        repository(authenticated(), cache, dataStore).select("loc_2")
        runCurrent()

        val restored = repository(authenticated(), cache, dataStore)

        assertEquals(airport, restored.selectedLocationFlow.first())
    }

    @Test
    fun `leaving authenticated clears the selection`() = runTest {
        val signedOutStates = listOf(AuthState.Unauthenticated, AuthState.SessionExpired, AuthState.Blocked)

        signedOutStates.forEach { signedOut ->
            val authService = FakeAuthService(authenticated())
            val repository = repository(authService, FakeMipymeCache(listOf(downtown, airport), listOf(owner)))
            repository.select("loc_1")
            runCurrent()

            authService.authState.value = signedOut
            runCurrent()
            authService.authState.value = authenticated()
            runCurrent()

            assertNull("state=$signedOut", repository.selectedLocationFlow.first())
        }
    }

    @Test
    fun `initializing keeps the selection`() = runTest {
        val authService = FakeAuthService(authenticated())
        val repository = repository(authService, FakeMipymeCache(listOf(downtown, airport), listOf(owner)))
        repository.select("loc_1")
        runCurrent()

        authService.authState.value = AuthState.Initializing
        runCurrent()
        authService.authState.value = authenticated()
        runCurrent()

        assertEquals(downtown, repository.selectedLocationFlow.first())
    }

    @Test
    fun `losing access to the selected location clears the selection`() = runTest {
        val cache = FakeMipymeCache(
            listOf(downtown, airport, warehouse),
            listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2"))),
        )
        val repository = repository(authenticated(), cache)
        repository.select("loc_1")
        runCurrent()

        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_2", "loc_3")))
        runCurrent()
        assertNull(repository.selectedLocationFlow.first())

        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2")))
        runCurrent()
        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `a disabled non owner loses the selection`() = runTest {
        val cache = FakeMipymeCache(listOf(downtown, airport), listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2"))))
        val repository = repository(authenticated(), cache)
        repository.select("loc_1")
        runCurrent()

        cache.employees = listOf(syncEmployee("emp_1", listOf("loc_1", "loc_2"), EmployeeStatus.DISABLED))
        runCurrent()

        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `a disabled owner keeps the selection`() = runTest {
        val cache = FakeMipymeCache(listOf(downtown, airport), listOf(owner))
        val repository = repository(authenticated(), cache)
        repository.select("loc_1")
        runCurrent()

        cache.employees = listOf(syncEmployee("emp_1", emptyList(), EmployeeStatus.DISABLED, isOwner = true))
        runCurrent()

        assertEquals(downtown, repository.selectedLocationFlow.first())
    }

    @Test
    fun `clearing a stale selection does not wipe a newer one`() = runTest {
        val delegate = FakePreferencesDataStore()
        val employeeKey = stringPreferencesKey("selected_employee_id")
        val locationKey = stringPreferencesKey("selected_location_id")
        delegate.updateData { it.toMutablePreferences().apply { set(employeeKey, "emp_1"); set(locationKey, "gone") } }
        val dataStore = InterceptingDataStore(delegate)
        dataStore.beforeFirstUpdate = {
            delegate.updateData { it.toMutablePreferences().apply { set(employeeKey, "emp_1"); set(locationKey, "loc_2") } }
        }
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown, airport), listOf(owner)), dataStore)
        runCurrent()

        assertEquals(airport, repository.selectedLocationFlow.first())
    }

    @Test
    fun `a selection still in flight after sign out is not persisted`() = runTest {
        val delegate = FakePreferencesDataStore()
        val dataStore = InterceptingDataStore(delegate)
        val authService = FakeAuthService(authenticated())
        val repository = repository(authService, FakeMipymeCache(listOf(downtown, airport), listOf(owner)), dataStore)
        dataStore.beforeFirstUpdate = { authService.authState.value = AuthState.Unauthenticated }

        val result = repository.select("loc_1")
        runCurrent()
        authService.authState.value = authenticated()
        runCurrent()

        assertEquals(Either.Left(LocationSelectionError.NotAccessible), result)
        assertNull(repository.selectedLocationFlow.first())
    }

    @Test
    fun `location data that was not synced yet is not exposed`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(listOf(downtown), listOf(owner), merchantId = null))

        assertEquals(LocationSelectionState(), repository.locationSelectionFlow.first())
    }

    @Test
    fun `the selection state carries the synced merchant id`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(employees = listOf(owner)))

        assertEquals("merch_1", repository.locationSelectionFlow.first().merchantId)
    }

    @Test
    fun `the selection state carries the employee id for the authenticated account`() = runTest {
        val repository = repository(authenticated(), FakeMipymeCache(employees = listOf(owner)))

        assertEquals("emp_1", repository.locationSelectionFlow.first().userId)
    }

    @Test
    fun `the selection state has no user id while signed out`() = runTest {
        val repository = repository(AuthState.Unauthenticated, FakeMipymeCache(listOf(downtown), listOf(owner)))

        assertEquals(LocationSelectionState(), repository.locationSelectionFlow.first())
    }

    private fun TestScope.repository(
        authState: AuthState,
        cache: FakeMipymeCache,
        dataStore: DataStore<Preferences> = FakePreferencesDataStore(),
    ): LocationSelectionRepository {
        return repository(FakeAuthService(authState), cache, dataStore)
    }

    private fun TestScope.repository(
        authService: FakeAuthService,
        cache: FakeMipymeCache,
        dataStore: DataStore<Preferences> = FakePreferencesDataStore(),
    ): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = dataStore,
            authService = authService,
            cache = cache,
            scope = backgroundScope,
        ).also { runCurrent() }
    }

    private fun authenticated(employeeId: String? = "emp_1"): AuthState.Authenticated {
        return AuthState.Authenticated(
            IdentityAccount(
                id = "user_1",
                name = "Monkey D. Luffy",
                email = "luffy@example.com",
                pictureUrl = "",
                publicMetadata = IdentityPublicMetadata(merchantId = "merch_1", employeeId = employeeId),
            ),
        )
    }

    private class CountingDataStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        var updates = 0
            private set

        override val data = delegate.data

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            updates++
            return delegate.updateData(transform)
        }
    }

    private class InterceptingDataStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        var beforeFirstUpdate: (suspend () -> Unit)? = null

        override val data = delegate.data

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            beforeFirstUpdate?.let { hook ->
                beforeFirstUpdate = null
                hook()
            }
            return delegate.updateData(transform)
        }
    }
}
