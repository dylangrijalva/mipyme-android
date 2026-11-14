package co.mipyme.database.internal.relation

import androidx.room.Embedded
import androidx.room.Relation
import co.mipyme.database.internal.entity.OptionGroupEntity
import co.mipyme.database.internal.entity.ProductOptionEntity

internal data class OptionGroupWithOptions(
    @Embedded val group: OptionGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "option_group_id")
    val options: List<ProductOptionEntity>,
)
