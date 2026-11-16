package co.mipyme.data.inventory

import co.mipyme.auth.AuthState
import co.mipyme.contract.*
import co.mipyme.contract.Collection
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.database.MipymeCache
import co.mipyme.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryRepositoryTest {
    @Test
    fun `context uses selected location existing employee rules and merchant currency country`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val base = FakeMipymeCache(locations = listOf(location("water7"), location("wano")), employees = listOf(syncEmployee("luffy", listOf("water7"), permissions = listOf(PosPermission.MANAGE_CATALOG))))
        val time = LocalDateTime.of(2026, 11, 16, 10, 0)
        val merchant = MutableStateFlow(Merchant("strawhatpirates", "Straw Hat Pirates", CountryIsoCode.NI, time, time))
        val categories = MutableStateFlow(listOf(
            Collection("wano", "Wano", "wano", time, time), Collection("alabasta", "Alabasta", "alabasta", time, time),
        ))
        val cache = object : MipymeCache by base {
            override fun observeMerchant(): Flow<Merchant?> { return merchant }
            override fun observeCollections(): Flow<List<Collection>> { return categories }
        }
        val auth = FakeAuthService(AuthState.Authenticated(IdentityAccount("luffy", "Monkey D. Luffy", "luffy@strawhatpirates.example.com", "https://strawhatpirates.example.com/luffy.png", IdentityPublicMetadata(employeeId = "luffy"))))
        val locations = LocationSelectionRepository(FakePreferencesDataStore(), auth, cache, scope)
        val sync = FakeSyncEngine()
        val repository = DefaultInventoryRepository(cache, locations, CurrentEmployeeRepository(auth, cache, scope), sync)
        var context: InventoryContext? = null
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeContext().collect { context = it } }
        advanceUntilIdle()
        assertEquals(InventoryContext("water7", CountryIsoCode.NI, true), context)
        base.employees = listOf(syncEmployee("luffy", listOf("water7"), permissions = listOf(PosPermission.MANAGE_INVENTORY)))
        advanceUntilIdle()
        assertFalse(context!!.canAdd)
        base.employees = listOf(syncEmployee("luffy", listOf("water7"), status = EmployeeStatus.DISABLED, permissions = listOf(PosPermission.MANAGE_CATALOG)))
        advanceUntilIdle()
        assertFalse(context!!.canAdd)
        base.employees = listOf(syncEmployee("luffy", emptyList(), status = EmployeeStatus.DISABLED, isOwner = true))
        advanceUntilIdle()
        assertTrue(context!!.canAdd)
        locations.select("wano")
        advanceUntilIdle()
        assertEquals("wano", context!!.locationId)
        assertEquals(listOf("Alabasta", "Wano"), repository.observeCategories().first().map { it.name })
        repository.refresh()
        assertEquals(1, sync.synchronizeCalls)
        collector.cancel()
        scope.cancel()
    }
}
