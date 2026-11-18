package co.mipyme.screen.product_editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockStatusTest {
    @Test
    fun `missing or invalid quantity is not set`() {
        for (quantity in listOf("", " ", "invalid")) {
            assertEquals(ProductStockStatus.NOT_SET, location(quantity).stockStatus)
        }
    }

    @Test
    fun `zero and negative quantities are out of stock regardless of threshold`() {
        for (quantity in listOf("0", "0.00", "-2.5")) {
            assertEquals(ProductStockStatus.OUT_OF_STOCK, location(quantity, "10").stockStatus)
        }
    }

    @Test
    fun `positive quantity at or below threshold is low stock`() {
        for (quantity in listOf("1", "2.50", "2,5")) {
            assertEquals(ProductStockStatus.LOW_STOCK, location(quantity, "2.5").stockStatus)
        }
    }

    @Test
    fun `quantity above threshold is in stock`() {
        assertEquals(ProductStockStatus.IN_STOCK, location("2.51", "2.5").stockStatus)
    }

    @Test
    fun `positive quantity without valid threshold is in stock`() {
        for (threshold in listOf("", " ", "invalid", "-1", "0")) {
            assertEquals(ProductStockStatus.IN_STOCK, location("1", threshold).stockStatus)
        }
    }

    private fun location(quantity: String, lowStock: String = ""): ProductEditorLocation {
        return ProductEditorLocation(id = "water-seven", name = "Water Seven", quantity = quantity, lowStock = lowStock)
    }
}
