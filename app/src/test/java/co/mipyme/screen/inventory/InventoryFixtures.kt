package co.mipyme.screen.inventory

import co.mipyme.contract.*
import java.time.LocalDateTime

internal val inventoryTime: LocalDateTime = LocalDateTime.of(2026, 11, 16, 10, 0)

internal fun sunnyProduct(
    status: ProductStatus = ProductStatus.ACTIVE,
    type: ProductType = ProductType.ITEM,
    variants: List<ProductVariant> = listOf(sunnyVariant("luffy", 2.5), sunnyVariant("zoro", 1.25)),
): Product {
    return Product(
        id = "thousand_sunny", name = "Thousand Sunny", slug = "thousand-sunny", unitCost = Decimal("5"),
        price = Decimal("25.50"), productType = type, status = status, imageUrl = null,
        collection = ProductCollection("water7", "Water 7"), optionGroups = emptyList(), variants = variants,
        createdAt = inventoryTime, updatedAt = inventoryTime, archivedAt = null,
    )
}

internal fun sunnyVariant(id: String, quantity: Double?, location: String = "water7"): ProductVariant {
    return ProductVariant(
        id = id, name = "Straw Hat $id", sku = "strawhat_$id", barcode = "WANO-$id", price = Decimal("999"),
        options = emptyList(), inventoryLevels = if (quantity == null) emptyList() else listOf(
            InventoryLevel("level_$id", id, location, quantity, 1.0, 10.0),
        ), createdAt = inventoryTime, updatedAt = inventoryTime,
    )
}
