package co.mipyme.database

import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType

data class CatalogQuery(
    val text: String = "",
    val categoryIds: Set<String> = emptySet(),
    val includeUncategorized: Boolean = false,
    val type: ProductType? = null,
    val statuses: Set<ProductStatus> = emptySet(),
    val grouping: CatalogGrouping = CatalogGrouping.NONE,
) {
    val filterCount: Int
        get() = (if (categoryIds.isNotEmpty() || includeUncategorized) 1 else 0) +
            (if (type != null) 1 else 0) + (if (statuses.isNotEmpty()) 1 else 0)

    fun clearFilters(): CatalogQuery {
        return copy(categoryIds = emptySet(), includeUncategorized = false, type = null, statuses = emptySet())
    }
}

enum class CatalogGrouping { NONE, CATEGORY, STATUS }
