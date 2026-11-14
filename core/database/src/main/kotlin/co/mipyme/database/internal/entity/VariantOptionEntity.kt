package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "variant_options",
    primaryKeys = ["variant_id", "option_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["option_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("option_id")],
)
internal data class VariantOptionEntity(
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "option_id") val optionId: String,
    val ordinal: Int,
)
