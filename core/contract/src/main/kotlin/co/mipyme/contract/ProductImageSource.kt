package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductImageSource {
    @SerialName("managed")
    MANAGED,

    @SerialName("remote")
    REMOTE,
}
