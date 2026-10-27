package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductImageStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("available")
    AVAILABLE,
}
