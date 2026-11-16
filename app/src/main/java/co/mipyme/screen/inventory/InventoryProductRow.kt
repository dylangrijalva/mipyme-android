package co.mipyme.screen.inventory

import co.mipyme.contract.Product
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.database.CatalogGrouping
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal data class InventoryProductRow(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val price: String,
    val quantity: String?,
    val isService: Boolean,
    val badge: InventoryBadge?,
    val group: InventoryGroup?,
)

internal enum class InventoryBadge { DRAFT, ARCHIVED, OUT_OF_STOCK }

internal data class InventoryGroup(val key: String, val categoryName: String? = null, val status: ProductStatus? = null)

internal sealed interface InventoryListItem {
    data class ProductItem(val row: InventoryProductRow) : InventoryListItem
    data class Header(val group: InventoryGroup) : InventoryListItem
}

internal fun Product.toInventoryRow(
    context: InventoryContext,
    grouping: CatalogGrouping,
    locale: Locale = Locale.getDefault(),
): InventoryProductRow {
    val service = productType == ProductType.SERVICE
    val stock = if (service || context.locationId == null || variants.isEmpty()) null else {
        val values = variants.map { variant ->
            variant.inventoryLevels.firstOrNull { it.locationId == context.locationId }?.onHand
        }
        if (values.any { it == null || !it.isFinite() }) null else values.filterNotNull().fold(BigDecimal.ZERO) { total, quantity -> total + BigDecimal.valueOf(quantity) }
    }
    val priceFormatter = if (context.country == null || context.country.name == "ZZ") {
        NumberFormat.getNumberInstance(locale)
    } else {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(Locale.Builder().setRegion(context.country.name).build())
        }
    }
    val quantityFormatter = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 340 }
    val group = when (grouping) {
        CatalogGrouping.NONE -> null
        CatalogGrouping.CATEGORY -> InventoryGroup("category:${collection?.id.orEmpty()}", categoryName = collection?.name)
        CatalogGrouping.STATUS -> InventoryGroup("status:$status", status = status)
    }
    return InventoryProductRow(
        id = id,
        name = name,
        imageUrl = imageUrl?.value,
        price = priceFormatter.format(BigDecimal(price.value)),
        quantity = stock?.let { quantityFormatter.format(it) },
        isService = service,
        badge = when {
            status == ProductStatus.DRAFT -> InventoryBadge.DRAFT
            status == ProductStatus.ARCHIVED -> InventoryBadge.ARCHIVED
            !service && stock != null && stock.signum() <= 0 -> InventoryBadge.OUT_OF_STOCK
            else -> null
        },
        group = group,
    )
}
