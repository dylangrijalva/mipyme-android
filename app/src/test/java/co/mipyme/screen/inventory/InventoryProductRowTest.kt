package co.mipyme.screen.inventory

import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.database.CatalogGrouping
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class InventoryProductRowTest {

    @Test
    fun `price uses product default and fractional quantities across every variant at selected location`() {
        val product = sunnyProduct()
        val row = product.toInventoryRow(InventoryContext("water7", CountryIsoCode.NI), CatalogGrouping.NONE, Locale.US)
        assertTrue(row.price, row.price.contains("25.50"))
        assertFalse(row.price.contains("999"))
        assertEquals("3.75", row.quantity)
        assertNull(row.badge)
        assertNull(product.toInventoryRow(InventoryContext("wano"), CatalogGrouping.NONE).quantity)
        assertNull(product.toInventoryRow(InventoryContext(), CatalogGrouping.NONE).quantity)
    }

    @Test
    fun `missing one or all variants never becomes a partial sum or zero`() {
        listOf(
            listOf(sunnyVariant("luffy", 2.5), sunnyVariant("zoro", null)),
            listOf(sunnyVariant("luffy", null), sunnyVariant("zoro", null)),
            emptyList(),
        ).forEach { variants ->
            val row = sunnyProduct(variants = variants).toInventoryRow(InventoryContext("water7"), CatalogGrouping.NONE)
            assertNull(row.quantity)
            assertNull(row.badge)
        }
    }

    @Test
    fun `stock totals preserve fractions without floating point artifacts and allow negative values`() {
        val fraction = sunnyProduct(variants = listOf(sunnyVariant("luffy", 0.1), sunnyVariant("zoro", 0.2)))
        assertEquals("0.3", fraction.toInventoryRow(InventoryContext("water7"), CatalogGrouping.NONE, Locale.US).quantity)
        val negative = sunnyProduct(variants = listOf(sunnyVariant("luffy", -1.25)))
        val row = negative.toInventoryRow(InventoryContext("water7"), CatalogGrouping.NONE, Locale.US)
        assertEquals("-1.25", row.quantity)
        assertEquals(InventoryBadge.OUT_OF_STOCK, row.badge)
    }

    @Test
    fun `lifecycle badges override out of stock and services never expose stock`() {
        for (type in ProductType.entries) for (status in ProductStatus.entries) {
            val row = sunnyProduct(status, type, listOf(sunnyVariant("luffy", 0.0)))
                .toInventoryRow(InventoryContext("water7"), CatalogGrouping.STATUS)
            assertEquals(type == ProductType.SERVICE, row.isService)
            if (type == ProductType.SERVICE) assertNull(row.quantity)
            assertEquals(when (status) {
                ProductStatus.DRAFT -> InventoryBadge.DRAFT
                ProductStatus.ARCHIVED -> InventoryBadge.ARCHIVED
                ProductStatus.ACTIVE -> if (type == ProductType.ITEM) InventoryBadge.OUT_OF_STOCK else null
            }, row.badge)
            assertEquals(status, row.group?.status)
        }
        val unavailable = sunnyProduct(ProductStatus.DRAFT, variants = emptyList()).toInventoryRow(InventoryContext(), CatalogGrouping.NONE)
        assertNull(unavailable.quantity)
        assertEquals(InventoryBadge.DRAFT, unavailable.badge)
    }
}
