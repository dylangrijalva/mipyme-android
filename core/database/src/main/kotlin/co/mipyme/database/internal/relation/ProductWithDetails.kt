package co.mipyme.database.internal.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import co.mipyme.contract.Decimal
import co.mipyme.contract.Product
import co.mipyme.contract.ProductCollection
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductOptionGroup
import co.mipyme.contract.ProductVariant
import co.mipyme.database.internal.entity.OptionGroupEntity
import co.mipyme.database.internal.entity.ProductEntity
import co.mipyme.database.internal.entity.ProductOptionGroupEntity
import co.mipyme.database.internal.entity.ProductVariantEntity
import co.mipyme.database.internal.entity.toContract

internal data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        entity = OptionGroupEntity::class,
        associateBy = Junction(
            value = ProductOptionGroupEntity::class,
            parentColumn = "product_id",
            entityColumn = "option_group_id",
        ),
    )
    val optionGroups: List<OptionGroupWithOptions>,
    @Relation(parentColumn = "id", entityColumn = "product_id")
    val groupLinks: List<ProductOptionGroupEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "product_id",
        entity = ProductVariantEntity::class,
    )
    val variants: List<VariantWithDetails>,
)

internal fun ProductWithDetails.toContract(): Product {
    val groupOrder = groupLinks.associate { it.optionGroupId to it.ordinal }
    val sortedGroups = optionGroups.sortedBy { groupOrder[it.group.id] ?: Int.MAX_VALUE }

    return Product(
        id = product.id,
        name = product.name,
        slug = product.slug,
        unitCost = Decimal(product.unitCost),
        price = Decimal(product.price),
        productType = product.productType,
        status = product.status,
        imageUrl = product.imageUrl?.let(::ProductImage),
        collection = product.collectionId?.let { id ->
            ProductCollection(id = id, name = product.collectionName.orEmpty())
        },
        optionGroups = sortedGroups.map { group ->
            ProductOptionGroup(
                id = group.group.id,
                name = group.group.name,
                options = group.options
                    .sortedWith(compareBy({ it.position }, { it.id }))
                    .map { it.toContract() },
                createdAt = group.group.createdAt,
                updatedAt = group.group.updatedAt,
            )
        },
        variants = variants.sortedBy { it.variant.ordinal }.map { details ->
            ProductVariant(
                id = details.variant.id,
                name = details.variant.name,
                sku = details.variant.sku,
                barcode = details.variant.barcode,
                price = details.variant.price?.let(::Decimal),
                options = details.options
                    .sortedBy { option ->
                        details.optionLinks.firstOrNull { it.optionId == option.id }?.ordinal ?: Int.MAX_VALUE
                    }
                    .map { it.toContract() },
                inventoryLevels = details.inventoryLevels
                    .sortedBy { it.ordinal }
                    .map { it.toContract() },
                createdAt = details.variant.createdAt,
                updatedAt = details.variant.updatedAt,
            )
        },
        createdAt = product.createdAt,
        updatedAt = product.updatedAt,
        archivedAt = product.archivedAt,
    )
}
