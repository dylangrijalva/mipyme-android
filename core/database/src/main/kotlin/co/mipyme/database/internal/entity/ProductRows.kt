package co.mipyme.database.internal.entity

import co.mipyme.contract.Product

internal data class ProductRows(
    val product: ProductEntity,
    val optionGroups: List<OptionGroupEntity>,
    val groupLinks: List<ProductOptionGroupEntity>,
    val options: List<ProductOptionEntity>,
    val unlinkedOptions: List<ProductOptionEntity>,
    val variants: List<ProductVariantEntity>,
    val variantOptions: List<VariantOptionEntity>,
    val inventoryLevels: List<InventoryLevelEntity>,
)

internal fun Product.toRows(): ProductRows {
    val linkedOptionIds = optionGroups.flatMap { group -> group.options.map { it.id } }.toSet()

    return ProductRows(
        product = ProductEntity(
            id = id,
            name = name,
            slug = slug,
            unitCost = unitCost.value,
            price = price.value,
            productType = productType,
            status = status,
            imageUrl = imageUrl?.value,
            collectionId = collection?.id,
            collectionName = collection?.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            archivedAt = archivedAt,
        ),
        optionGroups = optionGroups.map { group ->
            OptionGroupEntity(
                id = group.id,
                name = group.name,
                createdAt = group.createdAt,
                updatedAt = group.updatedAt,
            )
        },
        groupLinks = optionGroups.mapIndexed { index, group ->
            ProductOptionGroupEntity(productId = id, optionGroupId = group.id, ordinal = index)
        },
        options = optionGroups.flatMap { group ->
            group.options.map { option -> option.toEntity(group.id) }
        },
        unlinkedOptions = variants
            .flatMap { it.options }
            .filter { it.id !in linkedOptionIds }
            .distinctBy { it.id }
            .map { option -> option.toEntity(null) },
        variants = variants.mapIndexed { index, variant ->
            ProductVariantEntity(
                id = variant.id,
                productId = id,
                name = variant.name,
                sku = variant.sku,
                barcode = variant.barcode,
                price = variant.price?.value,
                ordinal = index,
                createdAt = variant.createdAt,
                updatedAt = variant.updatedAt,
            )
        },
        variantOptions = variants.flatMap { variant ->
            variant.options.mapIndexed { index, option ->
                VariantOptionEntity(variantId = variant.id, optionId = option.id, ordinal = index)
            }
        },
        inventoryLevels = variants.flatMap { variant ->
            variant.inventoryLevels.mapIndexed { index, level -> level.toEntity(variant.id, index) }
        },
    )
}
