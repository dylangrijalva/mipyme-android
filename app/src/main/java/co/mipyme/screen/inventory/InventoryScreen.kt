package co.mipyme.screen.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadStates
import androidx.paging.PagingData
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.database.CatalogGrouping
import kotlinx.coroutines.flow.MutableStateFlow
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
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import kotlin.math.abs

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
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = state.tab.ordinal, pageCount = { InventoryTab.entries.size })
    LaunchedEffect(state.tab) {
        if (pagerState.currentPage != state.tab.ordinal || abs(pagerState.currentPageOffsetFraction) > 0.001f) {
            pagerState.animateScrollToPage(state.tab.ordinal)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }.collect { (page, scrolling) ->
            if (!scrolling) actions.selectTab(InventoryTab.entries[page])
        }
    }
    Column(Modifier.fillMaxSize().background(Moshi.colors.background.surface)) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_inventory),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
            actions = if (state.canAdd) listOf(MoshiToolbarAction(
                icon = R.drawable.ic_fluent_add_24_regular,
                label = stringResource(R.string.inventory_add),
                onClick = actions.showAdd,
            )) else emptyList(),
        )
        MoshiTabs(
            labels = listOf(stringResource(R.string.inventory_products), stringResource(R.string.inventory_categories)),
            selectedIndex = state.tab.ordinal,
            onTabSelected = { index ->
                actions.selectTab(InventoryTab.entries[index])
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.padding(horizontal = spacing.pageGutterNarrow),
            pagePosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
        )
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), key = { it }) { page ->
            when (InventoryTab.entries[page]) {
                InventoryTab.PRODUCTS -> InventoryProductsPage(state, productScroll, actions, scanning)
                InventoryTab.CATEGORIES -> InventoryCategoriesPage(state, categoryScroll, actions)
            }
        }
    }
    state.draft?.let { draft ->
        InventoryFilterSheet(draft, state.categories.orEmpty(), actions.changeDraft, actions.applyFilters, actions.dismissFilters)
    }
    if (state.showAddSheet && state.canAdd) InventoryAddSheet(actions.dismissAdd)
}

@Composable
private fun InventoryProductsPage(
    state: InventoryScreenUiState,
    scroll: LazyListState,
    actions: InventoryActions,
    scanning: Boolean,
) {
    val spacing = Moshi.spacing
    val filterCount = state.query.filterCount
    InventoryPage(
        metadataReady = state.totalProducts != null,
        trulyEmpty = state.totalProducts == 0,
        state = state,
        actions = actions,
        search = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.pageGutterNarrow),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoshiSearchField(
                    value = state.query.text,
                    onValueChange = actions.productQuery,
                    label = stringResource(R.string.inventory_search_products),
                    modifier = Modifier.weight(1f),
                    clearLabel = stringResource(R.string.inventory_clear_search),
                )
                Spacer(Modifier.width(spacing.space1))
                Box {
                    MoshiIconButton(
                        icon = R.drawable.ic_fluent_filter_24_regular,
                        label = if (filterCount > 0) {
                            pluralStringResource(R.plurals.inventory_filter_count, filterCount, filterCount)
                        } else {
                            stringResource(R.string.inventory_filters)
                        },
                        onClick = actions.openFilters,
                        modifier = Modifier.background(Moshi.colors.background.subtle, RoundedCornerShape(spacing.space3)),
                    )
                    MoshiBadge(
                        count = filterCount,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                Spacer(Modifier.width(spacing.space1))
                MoshiIconButton(
                    icon = R.drawable.ic_fluent_barcode_scanner_24_regular,
                    label = stringResource(R.string.inventory_scan),
                    onClick = actions.scan,
                    modifier = Modifier.background(Moshi.colors.background.subtle, RoundedCornerShape(spacing.space3)),
                    enabled = !scanning,
                )
            }
        },
        empty = { InventoryEmptySection(products = true, canAdd = state.canAdd, onAdd = actions.showAdd) },
    ) {
        key(state.products) {
            val products = state.products.collectAsLazyPagingItems()
            InventoryProducts(state, products, scroll, actions)
        }
    }
}

@Composable
private fun InventoryCategoriesPage(
    state: InventoryScreenUiState,
    scroll: LazyListState,
    actions: InventoryActions,
) {
    InventoryPage(
        metadataReady = state.categories != null,
        trulyEmpty = state.categories?.isEmpty() == true,
        state = state,
        actions = actions,
        search = {
            MoshiSearchField(
                value = state.categoryQuery,
                onValueChange = actions.categoryQuery,
                label = stringResource(R.string.inventory_search_categories),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Moshi.spacing.pageGutterNarrow),
                clearLabel = stringResource(R.string.inventory_clear_search),
            )
        },
        empty = { InventoryEmptySection(products = false, canAdd = state.canAdd, onAdd = actions.showAdd) },
    ) {
        InventoryCategories(state, scroll, actions)
    }
}

@Composable
private fun InventoryPage(
    metadataReady: Boolean,
    trulyEmpty: Boolean,
    state: InventoryScreenUiState,
    actions: InventoryActions,
    search: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = Moshi.spacing
    val showSearchAndRefresh = metadataReady && !trulyEmpty
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(spacing.space4))
        if (showSearchAndRefresh) {
            search()
            Spacer(Modifier.height(spacing.space2))
        }
        val body: @Composable BoxScope.() -> Unit = {
            when {
                state.localDataError -> InventoryLocalError(actions.retryLocal)
                !metadataReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MoshiLoadingIndicator() }
                trulyEmpty -> empty()
                else -> content()
            }
        }
        val bodyModifier = Modifier.weight(1f).fillMaxWidth()
        if (showSearchAndRefresh) {
            MoshiPullToRefreshBox(state.refreshing, actions.refresh, bodyModifier, content = body)
        } else {
            Box(bodyModifier, content = body)
        }
    }
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
internal fun InventoryProductListRow(row: InventoryProductRow, modifier: Modifier = Modifier) {
    MoshiProductRow(
        name = row.name,
        price = row.price,
        supportingText = if (row.isService) stringResource(R.string.inventory_service) else {
            row.quantity?.let { stringResource(R.string.inventory_stock_quantity, it) }
                ?: stringResource(R.string.inventory_stock_unavailable)
        },
        tag = row.badge?.let { badge ->
            stringResource(when (badge) {
                InventoryBadge.DRAFT -> R.string.inventory_status_draft
                InventoryBadge.ARCHIVED -> R.string.inventory_status_archived
                InventoryBadge.OUT_OF_STOCK -> R.string.inventory_out_of_stock
            })
        },
        tagTone = if (row.badge == InventoryBadge.OUT_OF_STOCK) MoshiTagTone.Error else MoshiTagTone.Neutral,
        onClick = {},
        modifier = modifier,
        image = {
            SubcomposeAsyncImage(
                model = row.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = Moshi.spacing.space16, height = Moshi.spacing.space12)
                    .clip(RoundedCornerShape(Moshi.spacing.space2)),
                contentScale = ContentScale.Crop,
                loading = { MoshiMonogram(row.name, Modifier.fillMaxSize()) },
                error = { MoshiMonogram(row.name, Modifier.fillMaxSize()) },
            )
        },
    )
}

@Composable
private fun InventoryGroupHeading(group: InventoryGroup) {
    MoshiText(
        text = group.status?.let { stringResource(it.inventoryLabel()) }
            ?: group.categoryName ?: stringResource(R.string.inventory_uncategorized),
        modifier = Modifier
            .fillMaxWidth()
            .background(Moshi.colors.background.surface)
            .padding(horizontal = Moshi.spacing.pageGutterNarrow, vertical = Moshi.spacing.space3).semantics { heading() },
        role = MoshiTextRole.Heading14,
        color = Moshi.colors.foreground.secondary
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
internal fun InventoryCategoryListRow(row: InventoryCategoryRow) {
    val spacing = Moshi.spacing
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(interactionSource = null, indication = ripple(), onClick = {})
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoshiMonogram(row.name)
        Spacer(Modifier.width(spacing.space3))
        MoshiText(row.name, modifier = Modifier.weight(1f))
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

// StateFlow exposes PagingData's cached snapshot immediately to static previews.
private fun inventoryPreviewState(
    rows: List<InventoryListItem> = inventoryPreviewProducts(),
    append: LoadState = LoadState.NotLoading(true),
): InventoryScreenUiState {
    val categories = listOf(InventoryCategoryRow("water7", "Water 7"), InventoryCategoryRow("alabasta", "Alabasta"))
    return InventoryScreenUiState(
        products = MutableStateFlow(PagingData.from(rows, LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), append))),
        totalProducts = 6,
        categories = categories,
        visibleCategories = categories,
        canAdd = true,
    )
}

private fun inventoryPreviewProducts(): List<InventoryListItem> {
    return listOf(
        InventoryProductRow(
            "sunny", "Thousand Sunny Straw Hat Pirates supplies and navigation services", null,
            "C$ 999,999,999.99", "123,456.789", false, InventoryBadge.DRAFT, null,
        ),
        InventoryProductRow("logpose", "Log Pose", null, "C$ 350.00", "0", false, InventoryBadge.OUT_OF_STOCK, null),
        InventoryProductRow("merry", "Going Merry", null, "C$ 1,200.00", "12", false, null, null),
        InventoryProductRow("den-den", "Den Den Mushi", null, "C$ 750.00", "3", false, InventoryBadge.ARCHIVED, null),
        InventoryProductRow("navigation", "Nami navigation", null, "C$ 150.00", null, true, null, null),
        InventoryProductRow("vivre", "Vivre Card", null, "C$ 25.00", null, false, null, null),
    ).map { InventoryListItem.ProductItem(it) }
}

private fun inventoryPreviewNoMatches(query: CatalogQuery): InventoryScreenUiState {
    return inventoryPreviewState(rows = emptyList()).copy(query = query)
}

@ScreenPreview
@Preview(name = "Spanish large text light", locale = "es", fontScale = 2f, widthDp = 320, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Spanish large text dark", locale = "es", fontScale = 2f, widthDp = 320, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InventoryProductsPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState()) }
}

@ScreenPreview
@Composable
private fun InventoryEmptyProductsReadOnlyPreview() {
    MoshiTheme { InventoryContent(InventoryScreenUiState(totalProducts = 0, canAdd = false)) }
}

@ScreenPreview
@Composable
private fun InventoryCategoriesPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState().copy(tab = InventoryTab.CATEGORIES)) }
}

@ScreenPreview
@Composable
private fun InventoryEmptyCategoriesPreview() {
    MoshiTheme { InventoryContent(InventoryScreenUiState(tab = InventoryTab.CATEGORIES, categories = emptyList(), canAdd = true)) }
}

@ScreenPreview
@Composable
private fun InventoryLoadingProductsPreview() {
    MoshiTheme { InventoryContent(InventoryScreenUiState(canAdd = true)) }
}

@ScreenPreview
@Composable
private fun InventoryLocalErrorPreview() {
    MoshiTheme { InventoryContent(InventoryScreenUiState(localDataError = true, canAdd = true)) }
}

@ScreenPreview
@Composable
private fun InventoryAppendLoadingPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState(rows = inventoryPreviewProducts().take(2), append = LoadState.Loading)) }
}

@ScreenPreview
@Composable
private fun InventoryAppendErrorPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState(rows = inventoryPreviewProducts().take(2), append = LoadState.Error(IllegalStateException("Preview")))) }
}

@ScreenPreview
@Composable
private fun InventoryRefreshingPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState().copy(refreshing = true)) }
}

@ScreenPreview
@Composable
private fun InventoryFilteredProductsPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState(rows = inventoryPreviewProducts().take(1)).copy(query = CatalogQuery(categoryIds = setOf("water7"), type = ProductType.ITEM))) }
}

@ScreenPreview
@Composable
private fun InventoryNoSearchAndFilterMatchesPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewNoMatches(CatalogQuery(text = "Zoro", type = ProductType.SERVICE))) }
}

@ScreenPreview
@Composable
private fun InventoryNoCategoryMatchesPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState().copy(tab = InventoryTab.CATEGORIES, categoryQuery = "Wano", visibleCategories = emptyList())) }
}

@ScreenPreview
@Composable
private fun InventorySelectedFilterSheetPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState().copy(draft = CatalogQuery(categoryIds = setOf("water7"), includeUncategorized = true, type = ProductType.SERVICE, statuses = setOf(ProductStatus.DRAFT), grouping = CatalogGrouping.STATUS))) }
}

@ScreenPreview
@Composable
private fun InventoryAddSheetPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewState().copy(showAddSheet = true)) }
}

@ScreenPreview
@Composable
private fun InventoryCategoryGroupingPreview() {
    val named = InventoryGroup("water7", categoryName = "Water 7")
    val uncategorized = InventoryGroup("uncategorized")
    val products = inventoryPreviewProducts()
    val rows = listOf(InventoryListItem.Header(named), products[0], InventoryListItem.Header(uncategorized), products[1])
    MoshiTheme { InventoryContent(inventoryPreviewState(rows).copy(query = CatalogQuery(grouping = CatalogGrouping.CATEGORY))) }
}

@ScreenPreview
@Preview(name = "Spanish large text light", locale = "es", fontScale = 2f, widthDp = 320, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Spanish large text dark", locale = "es", fontScale = 2f, widthDp = 320, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InventoryNoMatchesLargeTextPreview() {
    MoshiTheme { InventoryContent(inventoryPreviewNoMatches(CatalogQuery(text = "Zoro", type = ProductType.SERVICE))) }
}
