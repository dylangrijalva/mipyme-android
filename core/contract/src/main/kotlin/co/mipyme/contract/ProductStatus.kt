package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("draft")
    DRAFT,

    @SerialName("archived")
    ARCHIVED,
}
