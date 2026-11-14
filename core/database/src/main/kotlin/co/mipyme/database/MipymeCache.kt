package co.mipyme.database

import co.mipyme.contract.Collection
import co.mipyme.contract.Customer
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.Supplier
import kotlinx.coroutines.flow.Flow

interface MipymeCache {
    fun observeMerchant(): Flow<Merchant?>

    fun observeProducts(): Flow<List<Product>>

    suspend fun findVariantByBarcode(barcode: String): ProductVariantMatch?

    fun observeLocations(): Flow<List<Location>>

    fun observeCustomers(): Flow<List<Customer>>

    fun observeSuppliers(): Flow<List<Supplier>>

    fun observeCollections(): Flow<List<Collection>>

    fun observeSyncState(): Flow<SyncState?>
}
