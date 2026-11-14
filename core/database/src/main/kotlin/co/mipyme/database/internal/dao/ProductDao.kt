package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import co.mipyme.database.internal.entity.InventoryLevelEntity
import co.mipyme.database.internal.entity.OptionGroupEntity
import co.mipyme.database.internal.entity.ProductEntity
import co.mipyme.database.internal.entity.ProductOptionEntity
import co.mipyme.database.internal.entity.ProductOptionGroupEntity
import co.mipyme.database.internal.entity.ProductVariantEntity
import co.mipyme.database.internal.entity.VariantOptionEntity
import co.mipyme.database.internal.relation.ProductWithDetails
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ProductDao {
    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Upsert
    suspend fun upsertOptionGroups(groups: List<OptionGroupEntity>)

    @Upsert
    suspend fun upsertGroupLinks(links: List<ProductOptionGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlinkedOptions(options: List<ProductOptionEntity>)

    @Upsert
    suspend fun upsertOptions(options: List<ProductOptionEntity>)

    @Query("UPDATE product_options SET name = :name, position = :position, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateOptionContent(id: String, name: String, position: Double, updatedAt: LocalDateTime)

    @Upsert
    suspend fun upsertVariants(variants: List<ProductVariantEntity>)

    @Upsert
    suspend fun upsertVariantOptions(variantOptions: List<VariantOptionEntity>)

    @Upsert
    suspend fun upsertInventoryLevels(levels: List<InventoryLevelEntity>)

    @Query("DELETE FROM product_option_groups WHERE product_id IN (:productIds)")
    suspend fun deleteGroupLinksOf(productIds: List<String>)

    @Query("DELETE FROM product_variants WHERE product_id IN (:productIds)")
    suspend fun deleteVariantsOf(productIds: List<String>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("DELETE FROM option_groups")
    suspend fun deleteAllOptionGroups()

    @Query("DELETE FROM product_options")
    suspend fun deleteAllOptions()

    @Transaction
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ProductWithDetails>>

    @Transaction
    @Query(
        "SELECT * FROM products " +
            "WHERE id IN (SELECT product_id FROM product_variants WHERE barcode = :barcode) " +
            "ORDER BY CASE status WHEN 'ARCHIVED' THEN 1 ELSE 0 END, id LIMIT 1",
    )
    suspend fun findByVariantBarcode(barcode: String): ProductWithDetails?
}
