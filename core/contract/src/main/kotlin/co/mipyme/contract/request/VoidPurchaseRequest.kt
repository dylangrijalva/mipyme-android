package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class VoidPurchaseRequest(
    val reason: String? = null,
)
