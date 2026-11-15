package co.mipyme.sync.testing

import co.mipyme.contract.Collection
import co.mipyme.contract.Customer
import co.mipyme.contract.Employee
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.Supplier
import co.mipyme.database.EmployeeLocationAccess
import co.mipyme.database.MipymeCache
import co.mipyme.database.ProductVariantMatch
import co.mipyme.database.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class FakeMipymeCache : MipymeCache {
    val syncState = MutableStateFlow<SyncState?>(null)

    override fun observeSyncState(): Flow<SyncState?> = syncState

    override fun observeMerchant(): Flow<Merchant?> = emptyFlow()

    override fun observeProducts(): Flow<List<Product>> = emptyFlow()

    override suspend fun findVariantByBarcode(barcode: String): ProductVariantMatch? = null

    override fun observeLocations(): Flow<List<Location>> = emptyFlow()

    override fun observeLocationAccess(employeeId: String): Flow<EmployeeLocationAccess?> = emptyFlow()

    override fun observeEmployee(employeeId: String): Flow<Employee?> = emptyFlow()

    override fun observeCustomers(): Flow<List<Customer>> = emptyFlow()

    override fun observeSuppliers(): Flow<List<Supplier>> = emptyFlow()

    override fun observeCollections(): Flow<List<Collection>> = emptyFlow()
}
