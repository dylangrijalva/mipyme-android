package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class ReadResourcesResponse(
    val syncToken: String,
    val fullSync: Boolean,
    val merchant: Merchant?,
    val products: List<Product>,
    val collections: List<Collection>,
    val customers: List<Customer>,
    val employees: List<Employee>,
    val locations: List<Location>,
    val suppliers: List<Supplier>,
)
