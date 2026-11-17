package co.mipyme.screen.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.lifecycle.ViewModelStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.core.Either
import co.mipyme.contract.Collection
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.Decimal
import co.mipyme.contract.Product
import co.mipyme.contract.ProductCollection
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.data.inventory.InventoryRepository
import co.mipyme.database.CatalogGrouping
import co.mipyme.database.CatalogQuery
import co.mipyme.sync.SyncError
import co.mipyme.ui.MoshiTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class InventoryScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private lateinit var viewModel: InventoryViewModel
    private val repository = ScreenInventoryRepository()

    @After
    fun tearDown() { compose.runOnIdle { store.clear() } }

    @Test
    fun emptyTabsDoNotRefreshOnPull() {
        var refreshes = 0
        val state = androidx.compose.runtime.mutableStateOf(
            InventoryScreenUiState(totalProducts = 0, categories = emptyList()),
        )
        compose.setContent {
            MoshiTheme {
                InventoryContent(state.value, InventoryActions(refresh = { refreshes++ }))
            }
        }
        compose.onRoot().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.9f) }
        compose.runOnIdle {
            assertEquals(0, refreshes)
            state.value = state.value.copy(tab = InventoryTab.CATEGORIES)
        }
        compose.onNodeWithText("Organize your catalog").assertIsDisplayed()
        compose.onRoot().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.9f) }
        compose.runOnIdle {
            assertEquals(0, refreshes)
            state.value = state.value.copy(
                categories = listOf(InventoryCategoryRow("water7", "Water 7")),
                categoryQuery = "Wano",
                visibleCategories = emptyList(),
            )
        }
        compose.onNodeWithText("No matches").assertIsDisplayed()
        compose.onRoot().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.9f) }
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun tabsRetainIndependentSearch() {
        start()
        compose.onNodeWithContentDescription("Search products").performTextInput("Sunny")
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithContentDescription("Search categories").performTextInput("Water")
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithContentDescription("Search products").assertTextContains("Sunny")
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithContentDescription("Search categories").assertTextContains("Water")
        compose.onNodeWithText("Water 7").assertIsDisplayed()
    }

    @Test
    fun switchingTabsDoesNotTransferSearchFocus() {
        start()
        compose.onNodeWithContentDescription("Search products").performClick().assertIsFocused()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithContentDescription("Search categories").assertIsNotFocused()
    }

    @Test
    fun horizontalSwipeSelectsCategoriesAndKeepsSearchControlsInSync() {
        start()
        compose.onNodeWithContentDescription("Search products").performTextInput("Sunny")
        compose.onNodeWithText("Sunny 000").performTouchInput { swipeLeft() }
        compose.waitUntil(5000) { viewModel.state.tab == InventoryTab.CATEGORIES }
        compose.onNodeWithContentDescription("Search categories").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search products").assertDoesNotExist()
        compose.onNodeWithText("Categories").assertIsSelected()
        compose.onNodeWithContentDescription("Search categories").performTextInput("Water")
        compose.onNodeWithText("Water 7").performTouchInput { swipeRight() }
        compose.waitUntil(5000) { viewModel.state.tab == InventoryTab.PRODUCTS }
        compose.onNodeWithContentDescription("Search products").assertTextContains("Sunny")
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithContentDescription("Search categories").assertTextContains("Water")
    }

    @Test
    fun tabsRetainIndependentScrollPositions() {
        repository.categories.value = (0..39).map { index ->
            Collection("water7_$index", "Water 7 ${index.toString().padStart(3, '0')}", "water7-$index", time, time)
        }
        start()
        compose.waitUntil(5000) { repository.loads > 0 }
        compose.onNode(verticalListWithScrollToIndex()).performScrollToIndex(7)
        compose.onNodeWithText("Sunny 007").assertIsDisplayed()
        compose.onNodeWithText("Categories").performClick()
        compose.onNode(verticalListWithScrollToIndex()).performScrollToIndex(15)
        compose.onNodeWithText("Water 7 015").assertIsDisplayed()
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithText("Sunny 007").assertIsDisplayed()
        compose.onNodeWithText("Sunny 000").assertDoesNotExist()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithText("Water 7 015").assertIsDisplayed()
        compose.onNodeWithText("Water 7 000").assertDoesNotExist()
    }

    @Test
    fun trulyEmptySectionsHideSearchAndPermissionControlsAdd() {
        repository.products = emptyList()
        repository.categories.value = emptyList()
        start()
        compose.onNodeWithText("Your catalog starts here").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search products").assertDoesNotExist()
        compose.onNodeWithText("Add product").assertDoesNotExist()
        compose.runOnIdle { repository.context.value = repository.context.value.copy(canAdd = true) }
        compose.onNodeWithText("Add product").performClick()
        compose.onNodeWithText("Add service").assertIsDisplayed().performClick()
        compose.onNodeWithText("Add service").assertDoesNotExist()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithText("Organize your catalog").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search categories").assertDoesNotExist()
        compose.onNodeWithText("Add category").assertIsDisplayed()
    }

    @Test
    fun filtersApplyTogetherAndDismissDiscardsDraft() {
        start()
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onNodeWithContentDescription("Alabasta").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("1 filter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onNodeWithContentDescription("Alabasta").performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("1 filter").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()
        compose.onNodeWithText("1 filter").assertDoesNotExist()
    }

    @Test
    fun noMatchesKeepsSearchAndClearActionsAndRefreshFailureKeepsState() {
        start()
        compose.onNodeWithContentDescription("Search products").performTextInput("Zoro")
        compose.onNodeWithText("No matches").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search products").assertIsDisplayed()
        compose.onNodeWithText("Clear search").assertIsDisplayed()
        compose.runOnIdle {
            viewModel.onOpenFilters()
            viewModel.onDraftChanged(viewModel.state.draft!!.copy(statuses = setOf(ProductStatus.DRAFT)))
            viewModel.onApplyFilters()
            viewModel.onRefresh()
        }
        compose.waitUntil { viewModel.state.briefError == InventoryBriefError.REFRESH }
        compose.onNodeWithText("No matches").assertIsDisplayed()
        compose.onAllNodesWithText("Clear filters").assertCountEquals(2)
        compose.runOnIdle {
            assertEquals("Zoro", viewModel.state.query.text)
            assertEquals(1, viewModel.state.query.filterCount)
            assertFalse(viewModel.state.refreshing)
        }
    }

    @Test
    fun permissionLossHidesToolbarAddAndClosesChooser() {
        repository.context.value = repository.context.value.copy(canAdd = true)
        start()
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Add service").assertIsDisplayed()
        compose.runOnIdle { repository.context.value = repository.context.value.copy(canAdd = false) }
        compose.onNodeWithText("Add service").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add").assertDoesNotExist()
    }

    @Test
    fun pagingKeepsOneStickyHeadingAcrossGroupBoundaries() {
        start()
        compose.runOnIdle {
            viewModel.onOpenFilters()
            viewModel.onDraftChanged(viewModel.state.draft!!.copy(grouping = CatalogGrouping.CATEGORY))
            viewModel.onApplyFilters()
        }
        compose.onNodeWithText("Alabasta").assertIsDisplayed()
        repeat(5) {
            compose.onNode(verticalListWithScrollToIndex()).performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        compose.waitUntil { repository.loads > 2 }
        compose.onAllNodesWithText("Water 7").assertCountEquals(1)
        compose.onNodeWithText("Water 7").assertIsDisplayed()
    }

    @Test
    fun serviceRowHidesStockAndMissingImageKeepsName() {
        repository.products = listOf(screenProduct(0).copy(productType = ProductType.SERVICE))
        start()
        compose.onNodeWithText("Service").assertIsDisplayed()
        compose.onNodeWithText("Stock unavailable").assertDoesNotExist()
        compose.onNodeWithText("Sunny 000").assertIsDisplayed()
    }

    @Test
    fun captureLightScreen() { captureScreen(false, 1f, "inventory-screen-light.png") }

    @Test
    fun captureDarkScreen() { captureScreen(true, 1f, "inventory-screen-dark.png") }

    @Test
    fun captureLargeFontScreen() { captureScreen(false, 2f, "inventory-screen-large-font.png") }

    private fun captureScreen(dark: Boolean, fontScale: Float, filename: String) {
        repository.products = listOf(screenProduct(0).copy(name = "Thousand Sunny Straw Hat Pirates supplies and navigation services", price = Decimal("999999999.99")), screenProduct(1).copy(productType = ProductType.SERVICE, status = ProductStatus.DRAFT))
        start(dark, fontScale)
        compose.waitUntil(5000) { compose.onAllNodesWithText(repository.products.first().name).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(repository.products.first().name).assertIsDisplayed()
        compose.waitForIdle()
        lateinit var bitmap: Bitmap
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "inventory-screen").apply { mkdirs() }
        File(directory, filename).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun start(dark: Boolean = false, fontScale: Float = 1f) {
        compose.runOnUiThread {
            viewModel = InventoryViewModel(repository)
            store.put("inventory", viewModel)
        }
        compose.setContent {
            val state by viewModel.stateFlow.collectAsState()
            MoshiTheme(darkTheme = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.width(320.dp).testTag("inventoryCapture")) {
                InventoryContent(state, InventoryActions(
                    selectTab = viewModel::onTabSelected, productQuery = viewModel::onProductQueryChanged,
                    categoryQuery = viewModel::onCategoryQueryChanged, openFilters = viewModel::onOpenFilters,
                    changeDraft = viewModel::onDraftChanged, applyFilters = viewModel::onApplyFilters,
                    dismissFilters = viewModel::onDismissFilters, clearFilters = viewModel::onClearFilters,
                    showAdd = viewModel::onShowAdd, dismissAdd = viewModel::onDismissAdd,
                    refresh = viewModel::onRefresh, retryLocal = viewModel::onRetryLocal,
                ))
                }
                }
            }
        }
        compose.waitUntil { viewModel.state.totalProducts != null && viewModel.state.categories != null }
        compose.waitForIdle()
    }

    private fun verticalListWithScrollToIndex(): SemanticsMatcher {
        return hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }

    private class ScreenInventoryRepository : InventoryRepository {
        var products = (0..39).map { screenProduct(it) }
        var loads = 0
        val context = MutableStateFlow(InventoryContext("water7", CountryIsoCode.NI))
        val categories = MutableStateFlow(listOf(
            Collection("alabasta", "Alabasta", "alabasta", time, time),
            Collection("water7", "Water 7", "water7", time, time),
        ))
        override fun observeProductCount(): Flow<Int> { return flowOf(products.size) }
        override fun observeCategories(): Flow<List<Collection>> { return categories }
        override fun observeContext(): Flow<InventoryContext> { return context }
        override suspend fun refresh(): Either<SyncError, Unit> { return Either.Left(SyncError.Network) }
        override fun observeProducts(query: CatalogQuery): Flow<PagingData<Product>> {
            val matches = products.filter { product ->
                product.name.contains(query.text, true) && (query.categoryIds.isEmpty() || product.collection?.id in query.categoryIds) &&
                    (query.statuses.isEmpty() || product.status in query.statuses)
            }.sortedWith(compareBy<Product> { if (query.grouping == CatalogGrouping.CATEGORY) it.collection?.name else "" }.thenBy { it.name })
            return Pager(PagingConfig(10, initialLoadSize = 10, prefetchDistance = 3, enablePlaceholders = false)) {
                object : PagingSource<Int, Product>() {
                    override fun getRefreshKey(state: PagingState<Int, Product>): Int? { return null }
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Product> {
                        loads++
                        val offset = params.key ?: 0
                        val end = (offset + params.loadSize).coerceAtMost(matches.size)
                        return LoadResult.Page(matches.subList(offset, end), if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0), if (end < matches.size) end else null)
                    }
                }
            }.flow
        }
    }

    companion object {
        private val time = LocalDateTime.of(2026, 11, 16, 10, 0)
        private fun screenProduct(index: Int): Product {
            return Product("sunny_$index", "Sunny ${index.toString().padStart(3, '0')}", "sunny-$index", Decimal("5"), Decimal("25.50"),
                ProductType.ITEM, ProductStatus.ACTIVE, null,
                if (index < 15) ProductCollection("alabasta", "Alabasta") else ProductCollection("water7", "Water 7"),
                emptyList(), emptyList(), time, time, null)
        }
    }
}
