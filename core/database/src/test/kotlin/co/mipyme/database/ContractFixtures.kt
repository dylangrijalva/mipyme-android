package co.mipyme.database

import co.mipyme.contract.BusinessType
import co.mipyme.contract.Collection
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.Customer
import co.mipyme.contract.Decimal
import co.mipyme.contract.InventoryLevel
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.ProductCollection
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductOption
import co.mipyme.contract.ProductOptionGroup
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.contract.ProductVariant
import co.mipyme.contract.Supplier
import co.mipyme.contract.SyncResponse
import java.time.LocalDateTime

val FIXTURE_TIME: LocalDateTime = LocalDateTime.of(2026, 11, 14, 10, 30, 15, 123_000_000)

fun productOption(id: String, name: String = id, position: Double = 0.0): ProductOption {
    return ProductOption(
        id = id,
        name = name,
        position = position,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun inventoryLevel(id: String, variantId: String, locationId: String = "loc_1"): InventoryLevel {
    return InventoryLevel(
        id = id,
        variantId = variantId,
        locationId = locationId,
        onHand = 4.5,
        lowStockThreshold = 2.0,
        optimalStockLevel = 10.0,
    )
}

fun productVariant(
    id: String,
    barcode: String? = null,
    options: List<ProductOption> = emptyList(),
    inventoryLevels: List<InventoryLevel> = listOf(inventoryLevel("${id}_level", id)),
): ProductVariant {
    return ProductVariant(
        id = id,
        name = "Variant $id",
        sku = "sku_$id",
        barcode = barcode,
        price = Decimal("12.50"),
        options = options,
        inventoryLevels = inventoryLevels,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun product(
    id: String,
    name: String = "Product $id",
    status: ProductStatus = ProductStatus.ACTIVE,
    optionGroups: List<ProductOptionGroup> = emptyList(),
    variants: List<ProductVariant> = listOf(productVariant("${id}_variant")),
): Product {
    return Product(
        id = id,
        name = name,
        slug = "slug-$id",
        unitCost = Decimal("5.00"),
        price = Decimal("12.50"),
        productType = ProductType.ITEM,
        status = status,
        imageUrl = ProductImage("https://img.example.com/$id.png"),
        collection = ProductCollection(id = "col_1", name = "Drinks"),
        optionGroups = optionGroups,
        variants = variants,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
        archivedAt = null,
    )
}

fun optionGroup(id: String, options: List<ProductOption>): ProductOptionGroup {
    return ProductOptionGroup(
        id = id,
        name = "Group $id",
        options = options,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun merchant(id: String = "merch_1"): Merchant {
    return Merchant(
        id = id,
        name = "Straw Hat Shop",
        country = CountryIsoCode.CO,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun collection(id: String): Collection {
    return Collection(id = id, name = "Collection $id", slug = "c-$id", createdAt = FIXTURE_TIME, updatedAt = FIXTURE_TIME)
}

fun customer(id: String): Customer {
    return Customer(
        id = id,
        name = "Customer $id",
        email = "$id@example.com",
        phone = null,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun supplier(id: String): Supplier {
    return Supplier(
        id = id,
        name = "Supplier $id",
        email = null,
        phone = "555-0100",
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
    )
}

fun location(id: String): Location {
    return Location(
        id = id,
        name = "Location $id",
        businessType = BusinessType.BAKERY,
        address = "Main St 1",
        phone = null,
        taxId = "123",
        taxRate = Decimal("19.00"),
        nextSaleNumber = 7,
        nextPurchaseOrderNumber = 3,
        allowBackorders = true,
        receiptHeaderText = "Hi",
        receiptFooterText = null,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
        exchangeRate = Decimal("1.00"),
    )
}

fun syncResponse(
    fullSync: Boolean = true,
    syncToken: String = "token_1",
    merchant: Merchant? = merchant(),
    products: List<Product> = emptyList(),
    collections: List<Collection> = emptyList(),
    customers: List<Customer> = emptyList(),
    suppliers: List<Supplier> = emptyList(),
    locations: List<Location> = emptyList(),
): SyncResponse {
    return SyncResponse(
        merchant = merchant,
        fullSync = fullSync,
        syncToken = syncToken,
        products = products,
        collections = collections,
        customers = customers,
        suppliers = suppliers,
        locations = locations,
    )
}
