package co.mipyme.screen.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import co.mipyme.R
import co.mipyme.data.inventory.DefaultInventoryRepository
import co.mipyme.data.inventory.InventoryRepository
import co.mipyme.database.CatalogQuery
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.home.LocalHomeDrawer
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.inventoryScreenModule(): Module {
    factory<InventoryRepository> { DefaultInventoryRepository(get(), get(), get(), get()) }
    viewModelOf(::InventoryViewModel)
    return this
}

@Composable
internal fun InventoryScreen(viewModel: InventoryViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val session = remember(context) { InventoryScanSession(GoogleInventoryScanner(context)) }
    val currentResultHandler by rememberUpdatedState(viewModel::onScanResult)
    var scanning by remember { mutableStateOf(false) }
    DisposableEffect(session) { onDispose { session.dispose() } }
    val briefErrorText = state.briefError?.let { error ->
        stringResource(when (error) {
            InventoryBriefError.REFRESH -> R.string.inventory_refresh_error
            InventoryBriefError.SCAN -> R.string.inventory_scan_error
        })
    }
    LaunchedEffect(briefErrorText) {
        briefErrorText?.let { text ->
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.onBriefErrorShown()
        }
    }
    InventoryContent(
        state = state,
        actions = InventoryActions(
            selectTab = viewModel::onTabSelected,
            productQuery = viewModel::onProductQueryChanged,
            categoryQuery = viewModel::onCategoryQueryChanged,
            openFilters = viewModel::onOpenFilters,
            changeDraft = viewModel::onDraftChanged,
            applyFilters = viewModel::onApplyFilters,
            dismissFilters = viewModel::onDismissFilters,
            clearFilters = viewModel::onClearFilters,
            showAdd = viewModel::onShowAdd,
            dismissAdd = viewModel::onDismissAdd,
            refresh = viewModel::onRefresh,
            retryLocal = viewModel::onRetryLocal,
            scan = {
                scanning = true
                session.launch { result ->
                    scanning = false
                    currentResultHandler(result)
                }
            },
        ),
        scanning = scanning,
    )
}

internal data class InventoryActions(
    val selectTab: (InventoryTab) -> Unit = {},
    val productQuery: (String) -> Unit = {},
    val categoryQuery: (String) -> Unit = {},
    val openFilters: () -> Unit = {},
    val changeDraft: (CatalogQuery) -> Unit = {},
    val applyFilters: () -> Unit = {},
    val dismissFilters: () -> Unit = {},
    val clearFilters: () -> Unit = {},
    val showAdd: () -> Unit = {},
    val dismissAdd: () -> Unit = {},
    val refresh: () -> Unit = {},
    val retryLocal: () -> Unit = {},
    val scan: () -> Unit = {},
)

@Composable
internal fun InventoryContent(
    state: InventoryScreenUiState,
    actions: InventoryActions = InventoryActions(),
    scanning: Boolean = false,
) {
    val productScroll = rememberLazyListState()
    val categoryScroll = rememberLazyListState()
    val spacing = Moshi.spacing
    Column(Modifier.fillMaxSize().background(Moshi.colors.background100)) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_inventory),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
            actions = if (state.canAdd) listOf(MoshiToolbarAction(
                icon = R.drawable.ic_fluent_add_24_regular,
                label = stringResource(R.string.inventory_add),
                onClick = actions.showAdd,
            )) else emptyList(),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = spacing.pageGutterNarrow).selectableGroup()) {
            MoshiPill(stringResource(R.string.inventory_products), state.tab == InventoryTab.PRODUCTS,
                { actions.selectTab(InventoryTab.PRODUCTS) }, Modifier.weight(1f), role = Role.Tab)
            Spacer(Modifier.width(spacing.space2))
            MoshiPill(stringResource(R.string.inventory_categories), state.tab == InventoryTab.CATEGORIES,
                { actions.selectTab(InventoryTab.CATEGORIES) }, Modifier.weight(1f), role = Role.Tab)
        }
        Spacer(Modifier.height(spacing.space4))
        val productsTab = state.tab == InventoryTab.PRODUCTS
        val trulyEmpty = if (productsTab) state.totalProducts == 0 else state.categories?.isEmpty() == true
        val metadataReady = if (productsTab) state.totalProducts != null else state.categories != null
        if (!trulyEmpty && metadataReady) {
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.pageGutterNarrow), verticalAlignment = Alignment.CenterVertically) {
                MoshiSearchField(
                    value = if (productsTab) state.query.text else state.categoryQuery,
                    onValueChange = if (productsTab) actions.productQuery else actions.categoryQuery,
                    label = stringResource(if (productsTab) R.string.inventory_search_products else R.string.inventory_search_categories),
                    modifier = Modifier.weight(1f),
                    clearLabel = stringResource(R.string.inventory_clear_search),
                )
                if (productsTab) {
                    Spacer(Modifier.width(spacing.space1))
                    MoshiIconButton(R.drawable.ic_fluent_filter_24_regular, stringResource(R.string.inventory_filters), actions.openFilters)
                    MoshiIconButton(R.drawable.ic_fluent_barcode_scanner_24_regular, stringResource(R.string.inventory_scan), actions.scan, enabled = !scanning)
                }
            }
            Spacer(Modifier.height(spacing.space2))
            if (productsTab && state.query.filterCount > 0) {
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = spacing.pageGutterNarrow),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    MoshiPill(pluralStringResource(R.plurals.inventory_filter_count, state.query.filterCount, state.query.filterCount), true, actions.openFilters)
                    MoshiPill(stringResource(R.string.inventory_clear_filters), false, actions.clearFilters)
                }
                Spacer(Modifier.height(spacing.space2))
            }
        }
        MoshiPullToRefreshBox(state.refreshing, actions.refresh, Modifier.weight(1f).fillMaxWidth()) {
            key(state.products) {
                val products = state.products.collectAsLazyPagingItems()
                when {
                    state.localDataError -> InventoryLocalError(actions.retryLocal)
                    !metadataReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MoshiLoadingIndicator() }
                    trulyEmpty -> InventoryEmptySection(productsTab, state.canAdd, actions.showAdd)
                    productsTab -> InventoryProducts(state, products, productScroll, actions)
                    else -> InventoryCategories(state, categoryScroll, actions)
                }
            }
        }
    }
    state.draft?.let { draft ->
        InventoryFilterSheet(draft, state.categories.orEmpty(), actions.changeDraft, actions.applyFilters, actions.dismissFilters)
    }
    if (state.showAddSheet && state.canAdd) InventoryAddSheet(actions.dismissAdd)
}

@Composable
private fun InventoryProducts(
    state: InventoryScreenUiState,
    products: LazyPagingItems<InventoryListItem>,
    scroll: LazyListState,
    actions: InventoryActions,
) {
    val refresh = products.loadState.refresh
    when {
        refresh is LoadState.Loading && products.itemCount == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MoshiLoadingIndicator() }
        refresh is LoadState.Error && products.itemCount == 0 -> InventoryLocalError(products::retry)
        refresh is LoadState.NotLoading && products.itemCount == 0 -> InventoryNoMatches(
            query = state.query.text,
            hasFilters = state.query.filterCount > 0,
            clearSearch = { actions.productQuery("") },
            clearFilters = actions.clearFilters,
        )
        else -> LazyColumn(state = scroll, modifier = Modifier.fillMaxSize()) {
            for (index in 0 until products.itemCount) {
                when (val item = products.peek(index)) {
                    is InventoryListItem.Header -> stickyHeader(key = "header:${item.group.key}") {
                        // Accessing the indexed presentation item also advances Paging's load hints.
                        products[index]
                        InventoryGroupHeading(item.group)
                    }
                    is InventoryListItem.ProductItem -> item(key = "product:${item.row.id}", contentType = "product") {
                        (products[index] as? InventoryListItem.ProductItem)?.let { InventoryProductListRow(it.row) }
                    }
                    null -> item(key = "placeholder:$index") { products[index]; MoshiLoadingIndicator() }
                }
            }
            if (refresh is LoadState.Error || products.loadState.append is LoadState.Error) {
                item(key = "retry") {
                    MoshiButton(onClick = products::retry, modifier = Modifier.fillMaxWidth(), variant = MoshiButtonVariant.Text) {
                        MoshiText(stringResource(R.string.inventory_retry))
                    }
                }
            }
            if (products.loadState.append is LoadState.Loading) {
                item(key = "append") { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MoshiLoadingIndicator() } }
            }
        }
    }
}

@Composable
private fun InventoryGroupHeading(group: InventoryGroup) {
    MoshiText(
        text = group.status?.let { stringResource(it.inventoryLabel()) }
            ?: group.categoryName ?: stringResource(R.string.inventory_uncategorized),
        modifier = Modifier.fillMaxWidth().background(Moshi.colors.background100)
            .padding(horizontal = Moshi.spacing.pageGutterNarrow, vertical = Moshi.spacing.space3).semantics { heading() },
        role = MoshiTextRole.Heading20,
    )
}

@Composable
private fun InventoryCategories(state: InventoryScreenUiState, scroll: LazyListState, actions: InventoryActions) {
    if (state.visibleCategories.isEmpty()) {
        InventoryNoMatches(state.categoryQuery, false, { actions.categoryQuery("") }, actions.clearFilters, categories = true)
    } else {
        LazyColumn(state = scroll, modifier = Modifier.fillMaxSize()) {
            items(state.visibleCategories, key = { it.id }) { InventoryCategoryListRow(it) }
        }
    }
}

@Composable
private fun InventoryEmptySection(products: Boolean, canAdd: Boolean, onAdd: () -> Unit) {
    InventoryEmptyContainer {
        MoshiEmptyState(
            title = stringResource(if (products) R.string.inventory_empty_products_title else R.string.inventory_empty_categories_title),
            description = stringResource(if (products) R.string.inventory_empty_products_description else R.string.inventory_empty_categories_description),
            icon = { InventoryEmptyIcon(products) },
            primaryAction = if (canAdd) MoshiEmptyStateAction(stringResource(if (products) R.string.inventory_add_product else R.string.inventory_add_category), onAdd) else null,
        )
    }
}

@Composable
private fun InventoryNoMatches(query: String, hasFilters: Boolean, clearSearch: () -> Unit, clearFilters: () -> Unit, categories: Boolean = false) {
    val clearSearchAction = if (query.isNotEmpty()) MoshiEmptyStateAction(stringResource(R.string.inventory_clear_search), clearSearch) else null
    val clearFiltersAction = if (hasFilters) MoshiEmptyStateAction(stringResource(R.string.inventory_clear_filters), clearFilters) else null
    InventoryEmptyContainer {
        MoshiEmptyState(
            title = stringResource(R.string.inventory_no_matches_title),
            description = stringResource(if (categories) R.string.inventory_no_categories_description else R.string.inventory_no_matches_description),
            icon = { InventoryEmptyIcon(!categories) },
            primaryAction = clearSearchAction ?: clearFiltersAction,
            secondaryAction = if (clearSearchAction != null) clearFiltersAction else null,
        )
    }
}

@Composable
private fun InventoryLocalError(onRetry: () -> Unit) {
    InventoryEmptyContainer {
        MoshiEmptyState(
            title = stringResource(R.string.inventory_local_error_title),
            description = stringResource(R.string.inventory_local_error_description),
            icon = { InventoryEmptyIcon(true) },
            primaryAction = MoshiEmptyStateAction(stringResource(R.string.inventory_retry), onRetry),
        )
    }
}

@Composable
private fun InventoryEmptyContainer(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { content() }
}

@Composable
private fun InventoryEmptyIcon(products: Boolean) {
    Icon(painterResource(if (products) R.drawable.ic_fluent_box_24_filled else R.drawable.ic_fluent_tag_24_filled), null,
        modifier = Modifier.size(Moshi.spacing.space6))
}

@ScreenPreview
@Composable
fun InventoryScreenPreview() {
    MoshiTheme { InventoryContent(InventoryScreenUiState(totalProducts = 0, categories = emptyList(), canAdd = true)) }
}
