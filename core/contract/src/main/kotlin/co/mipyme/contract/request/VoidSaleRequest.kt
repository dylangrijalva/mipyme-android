package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class VoidSaleRequest(
    val reason: String? = null,
)
