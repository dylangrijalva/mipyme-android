package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryMovementReason {
    @SerialName("sale")
    SALE,

    @SerialName("sale_void")
    SALE_VOID,

    @SerialName("purchase")
    PURCHASE,

    @SerialName("purchase_void")
    PURCHASE_VOID,

    @SerialName("lost")
    LOST,

    @SerialName("damaged")
    DAMAGED,

    @SerialName("theft")
    THEFT,

    @SerialName("correction")
    CORRECTION,
}
