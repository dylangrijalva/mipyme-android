package co.mipyme.database

import co.mipyme.contract.Collection
import co.mipyme.contract.Product

interface CatalogEditorCache {
    suspend fun findProduct(id: String, merchantId: String): Product?

    suspend fun storeProduct(product: Product, merchantId: String)

    suspend fun storeCollection(collection: Collection, merchantId: String)
}
