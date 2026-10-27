package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class PaginationResponse<T>(
    val results: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
