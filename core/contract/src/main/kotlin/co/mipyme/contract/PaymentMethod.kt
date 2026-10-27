package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod {
    @SerialName("cash")
    CASH,

    @SerialName("cash_dollar")
    CASH_DOLLAR,

    @SerialName("non_integrated_card")
    NON_INTEGRATED_CARD,

    @SerialName("wire_transfer")
    WIRE_TRANSFER,
}
