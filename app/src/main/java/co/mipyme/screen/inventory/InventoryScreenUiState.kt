package co.mipyme.screen.inventory

import androidx.paging.PagingData
import co.mipyme.database.CatalogQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal enum class InventoryTab { PRODUCTS, CATEGORIES }
internal enum class InventoryBriefError { REFRESH, SCAN }
internal data class InventoryCategoryRow(val id: String, val name: String)

internal data class InventoryScreenUiState(
    val tab: InventoryTab = InventoryTab.PRODUCTS,
    val query: CatalogQuery = CatalogQuery(),
    val categoryQuery: String = "",
    val draft: CatalogQuery? = null,
    val products: Flow<PagingData<InventoryListItem>> = flowOf(PagingData.empty()),
    val totalProducts: Int? = null,
    val categories: List<InventoryCategoryRow>? = null,
    val visibleCategories: List<InventoryCategoryRow> = emptyList(),
    val canAdd: Boolean = false,
    val showAddSheet: Boolean = false,
    val refreshing: Boolean = false,
    val briefError: InventoryBriefError? = null,
    val localDataError: Boolean = false,
)
