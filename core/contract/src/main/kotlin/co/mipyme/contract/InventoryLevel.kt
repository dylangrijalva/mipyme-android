package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class InventoryLevel(
    val id: String,
    val variantId: String,
    val locationId: String,
    val onHand: Double,
    val lowStockThreshold: Double,
    val optimalStockLevel: Double,
)
