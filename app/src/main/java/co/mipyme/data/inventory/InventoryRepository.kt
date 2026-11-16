package co.mipyme.data.inventory

import androidx.paging.PagingData
import arrow.core.Either
import co.mipyme.contract.Collection
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.PosPermission
import co.mipyme.contract.Product
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.database.CatalogQuery
import co.mipyme.database.MipymeCache
import co.mipyme.sync.SyncEngine
import co.mipyme.sync.SyncError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

interface InventoryRepository {
    fun observeProducts(query: CatalogQuery): Flow<PagingData<Product>>
    fun observeProductCount(): Flow<Int>
    fun observeCategories(): Flow<List<Collection>>
    fun observeContext(): Flow<InventoryContext>
    suspend fun refresh(): Either<SyncError, Unit>
}

data class InventoryContext(
    val locationId: String? = null,
    val country: CountryIsoCode? = null,
    val canAdd: Boolean = false,
)

class DefaultInventoryRepository(
    private val cache: MipymeCache,
    private val locations: LocationSelectionRepository,
    private val employees: CurrentEmployeeRepository,
    private val syncEngine: SyncEngine,
) : InventoryRepository {
    override fun observeProducts(query: CatalogQuery): Flow<PagingData<Product>> {
        return cache.observeCatalog(query)
    }

    override fun observeProductCount(): Flow<Int> {
        return cache.observeCatalogCount()
    }

    override fun observeCategories(): Flow<List<Collection>> {
        return cache.observeCollections()
            .map { categories -> categories.sortedWith(compareBy({ it.name.lowercase() }, { it.id })) }
            .flowOn(Dispatchers.Default)
    }

    override fun observeContext(): Flow<InventoryContext> {
        return combine(
            locations.selectedLocationFlow,
            employees.currentEmployeeFlow,
            cache.observeMerchant(),
        ) { location, employee, merchant ->
            InventoryContext(
                locationId = location?.id,
                country = merchant?.country,
                canAdd = employee != null && (employee.isOwner ||
                    (employee.status == EmployeeStatus.ENABLED && PosPermission.MANAGE_CATALOG in employee.permissions)),
            )
        }.distinctUntilChanged()
    }

    override suspend fun refresh(): Either<SyncError, Unit> {
        return syncEngine.synchronize()
    }
}
