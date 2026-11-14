package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_option_groups",
    primaryKeys = ["product_id", "option_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = OptionGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["option_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("option_group_id")],
)
internal data class ProductOptionGroupEntity(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "option_group_id") val optionGroupId: String,
    val ordinal: Int,
)
