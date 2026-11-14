package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import co.mipyme.contract.InventoryLevel

@Entity(
    tableName = "inventory_levels",
    foreignKeys = [
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("variant_id"), Index("location_id")],
)
internal data class InventoryLevelEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "on_hand") val onHand: Double,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Double,
    @ColumnInfo(name = "optimal_stock_level") val optimalStockLevel: Double,
    val ordinal: Int,
)

internal fun InventoryLevel.toEntity(variantId: String, ordinal: Int): InventoryLevelEntity {
    return InventoryLevelEntity(
        id = id,
        variantId = variantId,
        locationId = locationId,
        onHand = onHand,
        lowStockThreshold = lowStockThreshold,
        optimalStockLevel = optimalStockLevel,
        ordinal = ordinal,
    )
}

internal fun InventoryLevelEntity.toContract(): InventoryLevel {
    return InventoryLevel(
        id = id,
        variantId = variantId,
        locationId = locationId,
        onHand = onHand,
        lowStockThreshold = lowStockThreshold,
        optimalStockLevel = optimalStockLevel,
    )
}

