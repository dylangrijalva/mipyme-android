package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class ScopedPaginationRequest(
    val limit: Int = 50,
    val cursor: String? = null,
    val merchantId: String,
)
