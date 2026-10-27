package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateSupplierRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
)
