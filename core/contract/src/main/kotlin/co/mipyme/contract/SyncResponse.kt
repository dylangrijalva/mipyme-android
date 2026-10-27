package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val merchant: Merchant?,
    val fullSync: Boolean,
    val syncToken: String,
    val products: List<Product>,
    val collections: List<Collection>,
    val customers: List<Customer>,
    val suppliers: List<Supplier>,
    val locations: List<Location>,
)
