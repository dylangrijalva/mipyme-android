package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import java.time.LocalDateTime

@Entity(tableName = "products")
internal data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    @ColumnInfo(name = "unit_cost") val unitCost: String,
    val price: String,
    @ColumnInfo(name = "product_type") val productType: ProductType,
    val status: ProductStatus,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "collection_id") val collectionId: String?,
    @ColumnInfo(name = "collection_name") val collectionName: String?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
    @ColumnInfo(name = "archived_at") val archivedAt: LocalDateTime?,
)

