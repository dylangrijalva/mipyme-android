package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WeightUnit {
    @SerialName("each")
    EACH,

    @SerialName("g")
    GRAM,

    @SerialName("kg")
    KILOGRAM,

    @SerialName("oz")
    OUNCE,

    @SerialName("lb")
    POUND,
}
