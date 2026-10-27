package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryState {
    @SerialName("none")
    NONE,

    @SerialName("in_stock")
    IN_STOCK,

    @SerialName("sold")
    SOLD,

    @SerialName("waste")
    WASTE,
}
