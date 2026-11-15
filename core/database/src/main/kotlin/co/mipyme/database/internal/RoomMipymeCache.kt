package co.mipyme.database.internal

import androidx.room.withTransaction
import co.mipyme.contract.Collection
import co.mipyme.contract.Customer
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.Supplier
import co.mipyme.contract.SyncEmployee
import co.mipyme.contract.SyncResponse
import co.mipyme.database.EmployeeLocationAccess
import co.mipyme.database.MipymeCache
import co.mipyme.database.ProductVariantMatch
import co.mipyme.database.SyncCacheWriter
import co.mipyme.database.SyncState
import co.mipyme.database.internal.entity.EmployeeLocationEntity
import co.mipyme.database.internal.entity.SyncStateEntity
import co.mipyme.database.internal.entity.toContract
import co.mipyme.database.internal.entity.toEntity
import co.mipyme.database.internal.entity.toRows
import co.mipyme.database.internal.relation.toContract
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class RoomMipymeCache(
    private val database: MipymeCacheDatabase,
) : MipymeCache, SyncCacheWriter {
    private val merchantDao = database.merchantDao()
    private val collectionDao = database.collectionDao()
    private val customerDao = database.customerDao()
    private val supplierDao = database.supplierDao()
    private val locationDao = database.locationDao()
    private val employeeDao = database.employeeDao()
    private val productDao = database.productDao()
    private val syncStateDao = database.syncStateDao()

    override fun observeMerchant(): Flow<Merchant?> {
        return merchantDao.observe().map { it?.toContract() }
    }

    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeAll()
            .map { products -> products.map { it.toContract() } }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun findVariantByBarcode(barcode: String): ProductVariantMatch? {
        val product = productDao.findByVariantBarcode(barcode)?.toContract() ?: return null
        val variant = product.variants.firstOrNull { it.barcode == barcode } ?: return null
        return ProductVariantMatch(product = product, variant = variant)
    }

    override fun observeLocations(): Flow<List<Location>> {
        return locationDao.observeAll().map { items -> items.map { it.toContract() } }
    }

    override fun observeLocationAccess(employeeId: String): Flow<EmployeeLocationAccess?> {
        return employeeDao.observeLocationRows(employeeId, EmployeeStatus.ENABLED).map { rows ->
            val first = rows.firstOrNull()
            val merchantId = first?.merchantId
            if (first == null || merchantId == null) {
                null
            } else {
                EmployeeLocationAccess(
                    merchantId = merchantId,
                    isOwner = first.isOwner ?: false,
                    locations = rows.mapNotNull { it.location?.toContract() },
                )
            }
        }
    }

    override fun observeEmployee(employeeId: String): Flow<Employee?> {
        return employeeDao.observe(employeeId).map { it?.toContract() }
    }

    override fun observeCustomers(): Flow<List<Customer>> {
        return customerDao.observeAll().map { items -> items.map { it.toContract() } }
    }

    override fun observeSuppliers(): Flow<List<Supplier>> {
        return supplierDao.observeAll().map { items -> items.map { it.toContract() } }
    }

    override fun observeCollections(): Flow<List<Collection>> {
        return collectionDao.observeAll().map { items -> items.map { it.toContract() } }
    }

    override fun observeSyncState(): Flow<SyncState?> {
        return syncStateDao.observe().map { it?.toContract() }
    }

    override suspend fun readSyncState(): SyncState? {
        return syncStateDao.find()?.toContract()
    }

    override suspend fun apply(
        response: SyncResponse,
        merchantId: String,
        syncedAt: LocalDateTime,
        isCurrent: suspend () -> Boolean,
    ): Boolean {
        return database.withTransaction {
            if (!isCurrent()) {
                return@withTransaction false
            }
            if (response.fullSync) {
                deleteAllRows()
            }
            response.merchant?.let { merchantDao.upsert(it.toEntity()) }
            response.collections.chunked(BATCH_SIZE).forEach { batch ->
                collectionDao.upsertAll(batch.map { it.toEntity() })
            }
            response.customers.chunked(BATCH_SIZE).forEach { batch ->
                customerDao.upsertAll(batch.map { it.toEntity() })
            }
            response.suppliers.chunked(BATCH_SIZE).forEach { batch ->
                supplierDao.upsertAll(batch.map { it.toEntity() })
            }
            response.locations.chunked(BATCH_SIZE).forEach { batch ->
                locationDao.upsertAll(batch.map { it.toEntity() })
            }
            response.employees.chunked(BATCH_SIZE).forEach { batch ->
                replaceEmployees(batch)
            }
            response.products.chunked(BATCH_SIZE).forEach { batch ->
                replaceProducts(batch)
            }
            syncStateDao.upsert(
                SyncStateEntity(
                    syncToken = response.syncToken,
                    merchantId = merchantId,
                    lastSuccessfulSyncAt = syncedAt,
                ),
            )
            true
        }
    }

    override suspend fun clearCache() {
        database.withTransaction { deleteAllRows() }
    }

    override suspend fun clearCacheUnlessMerchant(merchantId: String) {
        database.withTransaction {
            val cachedMerchantId = syncStateDao.find()?.merchantId
            if (cachedMerchantId != null && cachedMerchantId != merchantId) {
                deleteAllRows()
            }
        }
    }

    private suspend fun replaceProducts(products: List<Product>) {
        val ids = products.map { it.id }
        productDao.deleteGroupLinksOf(ids)
        productDao.deleteVariantsOf(ids)
        val rows = products.map { it.toRows() }
        productDao.upsertProducts(rows.map { it.product })
        productDao.upsertOptionGroups(rows.flatMap { it.optionGroups })
        productDao.upsertGroupLinks(rows.flatMap { it.groupLinks })
        productDao.upsertOptions(rows.flatMap { it.options })
        val unlinkedOptions = rows.flatMap { it.unlinkedOptions }
        productDao.insertUnlinkedOptions(unlinkedOptions)
        unlinkedOptions.forEach { option ->
            productDao.updateOptionContent(option.id, option.name, option.position, option.updatedAt)
        }
        productDao.upsertVariants(rows.flatMap { it.variants })
        productDao.upsertVariantOptions(rows.flatMap { it.variantOptions })
        productDao.upsertInventoryLevels(rows.flatMap { it.inventoryLevels })
    }

    private suspend fun replaceEmployees(employees: List<SyncEmployee>) {
        employeeDao.upsertAll(employees.map { it.toEntity() })
        employeeDao.deleteLocationLinksOf(employees.map { it.id })
        employeeDao.insertLocationLinks(
            employees.flatMap { employee ->
                employee.locationIds.distinct().map { EmployeeLocationEntity(employee.id, it) }
            },
        )
    }

    private suspend fun deleteAllRows() {
        employeeDao.deleteAllLocationLinks()
        employeeDao.deleteAll()
        productDao.deleteAll()
        productDao.deleteAllOptions()
        productDao.deleteAllOptionGroups()
        collectionDao.deleteAll()
        customerDao.deleteAll()
        supplierDao.deleteAll()
        locationDao.deleteAll()
        merchantDao.deleteAll()
        syncStateDao.deleteAll()
    }

    private companion object {
        private const val BATCH_SIZE = 500
    }
}
