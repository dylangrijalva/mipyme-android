package co.mipyme.database.internal.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import co.mipyme.database.internal.entity.InventoryLevelEntity
import co.mipyme.database.internal.entity.ProductOptionEntity
import co.mipyme.database.internal.entity.ProductVariantEntity
import co.mipyme.database.internal.entity.VariantOptionEntity

internal data class VariantWithDetails(
    @Embedded val variant: ProductVariantEntity,
    @Relation(parentColumn = "id", entityColumn = "variant_id")
    val inventoryLevels: List<InventoryLevelEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = VariantOptionEntity::class,
            parentColumn = "variant_id",
            entityColumn = "option_id",
        ),
    )
    val options: List<ProductOptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "variant_id")
    val optionLinks: List<VariantOptionEntity>,
)
