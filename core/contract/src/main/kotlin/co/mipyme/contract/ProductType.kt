package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    @SerialName("item")
    ITEM,

    @SerialName("service")
    SERVICE,
}
