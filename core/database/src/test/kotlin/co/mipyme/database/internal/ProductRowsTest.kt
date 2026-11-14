package co.mipyme.database.internal

import co.mipyme.database.inventoryLevel
import co.mipyme.database.internal.entity.toRows
import co.mipyme.database.internal.relation.OptionGroupWithOptions
import co.mipyme.database.internal.relation.ProductWithDetails
import co.mipyme.database.internal.relation.VariantWithDetails
import co.mipyme.database.internal.relation.toContract
import co.mipyme.database.optionGroup
import co.mipyme.database.product
import co.mipyme.database.productOption
import co.mipyme.database.productVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductRowsTest {
    private val small = productOption("opt_s", position = 1.0)
    private val large = productOption("opt_l", position = 2.0)
    private val red = productOption("opt_red", position = 1.0)
    private val blue = productOption("opt_blue", position = 2.0)

    private val sample = product(
        id = "prod_1",
        optionGroups = listOf(
            optionGroup("grp_size", listOf(small, large)),
            optionGroup("grp_color", listOf(red, blue)),
        ),
        variants = listOf(
            productVariant(
                id = "var_1",
                barcode = "111",
                options = listOf(small, red),
                inventoryLevels = listOf(
                    inventoryLevel("lvl_b", "var_1", "loc_2"),
                    inventoryLevel("lvl_a", "var_1", "loc_1"),
                ),
            ),
            productVariant(id = "var_2", barcode = null, options = listOf(large, blue)),
        ),
    )

    @Test
    fun `contract product survives the trip through its rows`() {
        assertEquals(sample, sample.toRows().asDetails().toContract())
    }

    @Test
    fun `rows keep the child order the server sent`() {
        val shuffled = sample.toRows().asDetails().let { details ->
            details.copy(
                optionGroups = details.optionGroups.reversed(),
                variants = details.variants.reversed().map { it.copy(inventoryLevels = it.inventoryLevels.reversed()) },
            )
        }

        assertEquals(sample, shuffled.toContract())
    }

    @Test
    fun `variant options from a group the product does not link survive the trip`() {
        val orphan = productOption("opt_orphan", position = 3.0)
        val product = product(
            id = "prod_2",
            optionGroups = listOf(optionGroup("grp_size", listOf(small))),
            variants = listOf(productVariant(id = "var_9", options = listOf(orphan, small))),
        )

        val rows = product.toRows()

        assertEquals(listOf("opt_orphan"), rows.unlinkedOptions.map { it.id })
        assertEquals(product, rows.asDetails().toContract())
    }

    private fun co.mipyme.database.internal.entity.ProductRows.asDetails(): ProductWithDetails {
        return ProductWithDetails(
            product = product,
            optionGroups = optionGroups.map { group ->
                OptionGroupWithOptions(group, options.filter { it.optionGroupId == group.id })
            },
            groupLinks = groupLinks,
            variants = variants.map { variant ->
                val optionIds = variantOptions.filter { it.variantId == variant.id }.map { it.optionId }.toSet()
                VariantWithDetails(
                    variant = variant,
                    inventoryLevels = inventoryLevels.filter { it.variantId == variant.id },
                    options = (options + unlinkedOptions).filter { it.id in optionIds },
                    optionLinks = variantOptions.filter { it.variantId == variant.id },
                )
            },
        )
    }
}
