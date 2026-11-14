package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import co.mipyme.contract.ProductOption
import java.time.LocalDateTime

@Entity(
    tableName = "product_options",
    foreignKeys = [
        ForeignKey(
            entity = OptionGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["option_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("option_group_id")],
)
internal data class ProductOptionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "option_group_id") val optionGroupId: String?,
    val name: String,
    val position: Double,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

internal fun ProductOption.toEntity(optionGroupId: String?): ProductOptionEntity {
    return ProductOptionEntity(
        id = id,
        optionGroupId = optionGroupId,
        name = name,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun ProductOptionEntity.toContract(): ProductOption {
    return ProductOption(
        id = id,
        name = name,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

