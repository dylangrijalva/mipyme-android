package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryChangeType {
    @SerialName("movement")
    MOVEMENT,

    @SerialName("physical_count")
    PHYSICAL_COUNT,
}
