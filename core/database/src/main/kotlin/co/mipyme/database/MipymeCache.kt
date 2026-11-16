package co.mipyme.database

import androidx.paging.PagingData
import co.mipyme.contract.Collection
import co.mipyme.contract.Customer
import co.mipyme.contract.Employee
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.Supplier
import kotlinx.coroutines.flow.Flow

interface MipymeCache {
    fun observeMerchant(): Flow<Merchant?>

    fun observeProducts(): Flow<List<Product>>

    fun observeCatalog(query: CatalogQuery): Flow<PagingData<Product>>

    fun observeCatalogCount(query: CatalogQuery = CatalogQuery()): Flow<Int>

    suspend fun findVariantByBarcode(barcode: String): ProductVariantMatch?

    fun observeLocations(): Flow<List<Location>>

    fun observeLocationAccess(employeeId: String): Flow<EmployeeLocationAccess?>

    fun observeEmployee(employeeId: String): Flow<Employee?>

    fun observeCustomers(): Flow<List<Customer>>

    fun observeSuppliers(): Flow<List<Supplier>>

    fun observeCollections(): Flow<List<Collection>>

    fun observeSyncState(): Flow<SyncState?>
}
