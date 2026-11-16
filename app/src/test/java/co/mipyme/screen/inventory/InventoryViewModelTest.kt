package co.mipyme.screen.inventory

import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import arrow.core.Either
import co.mipyme.contract.Collection
import co.mipyme.contract.Product
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.data.inventory.InventoryRepository
import co.mipyme.database.CatalogGrouping
import co.mipyme.database.CatalogQuery
import co.mipyme.sync.SyncError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeInventoryRepository()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test
    fun `tab switching retains independent queries applied options and cached product handle`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onProductQueryChanged("Sunny")
        vm.onOpenFilters()
        vm.onDraftChanged(vm.state.draft!!.copy(type = ProductType.SERVICE, grouping = CatalogGrouping.CATEGORY))
        vm.onApplyFilters()
        val products = vm.state.products
        vm.onTabSelected(InventoryTab.CATEGORIES)
        vm.onCategoryQueryChanged("wano")
        vm.onTabSelected(InventoryTab.PRODUCTS)
        advanceUntilIdle()
        assertEquals("Sunny", vm.state.query.text)
        assertEquals("wano", vm.state.categoryQuery)
        assertEquals(ProductType.SERVICE, vm.state.query.type)
        assertEquals(CatalogGrouping.CATEGORY, vm.state.query.grouping)
        assertSame(products, vm.state.products)
    }

    @Test
    fun `sheet draft dismisses without applying and apply commits all choices together`() = runTest(dispatcher) {
        val vm = viewModel()
        val original = vm.state.products
        vm.onOpenFilters()
        vm.onDraftChanged(vm.state.draft!!.copy(categoryIds = setOf("wano", "water7"), statuses = setOf(ProductStatus.ACTIVE)))
        assertSame(original, vm.state.products)
        assertEquals(0, vm.state.query.filterCount)
        vm.onDismissFilters()
        assertEquals(0, vm.state.query.filterCount)
        vm.onOpenFilters()
        vm.onDraftChanged(vm.state.draft!!.copy(categoryIds = setOf("wano", "water7"), statuses = setOf(ProductStatus.ACTIVE, ProductStatus.DRAFT), grouping = CatalogGrouping.STATUS))
        vm.onApplyFilters()
        assertNull(vm.state.draft)
        assertEquals(2, vm.state.query.filterCount)
        assertEquals(CatalogGrouping.STATUS, vm.state.query.grouping)
        vm.onProductQueryChanged("Luffy")
        vm.onOpenFilters()
        vm.onDraftChanged(vm.state.draft!!.clearFilters())
        assertEquals(2, vm.state.query.filterCount)
        vm.onDismissFilters()
        vm.onClearFilters()
        assertEquals(0, vm.state.query.filterCount)
        assertEquals("Luffy", vm.state.query.text)
        assertEquals(CatalogGrouping.STATUS, vm.state.query.grouping)
    }

    @Test
    fun `nonempty scan replaces query and filters atomically while preserving grouping`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onOpenFilters()
        vm.onDraftChanged(CatalogQuery(text = "old", type = ProductType.ITEM, grouping = CatalogGrouping.CATEGORY))
        vm.onApplyFilters()
        val oldQueryCount = repository.queries.size
        vm.onScanResult(InventoryScanResult.Success("WANO-123"))
        assertEquals(oldQueryCount + 1, repository.queries.size)
        assertEquals("WANO-123", vm.state.query.text)
        assertEquals(0, vm.state.query.filterCount)
        assertEquals(CatalogGrouping.CATEGORY, vm.state.query.grouping)
        vm.onOpenFilters()
        vm.onDraftChanged(vm.state.draft!!.copy(type = ProductType.SERVICE))
        vm.onApplyFilters()
        val previous = vm.state.query
        for (result in listOf(InventoryScanResult.Cancelled, InventoryScanResult.Failure, InventoryScanResult.Success(""), InventoryScanResult.Success("  "))) {
            vm.onScanResult(result)
            assertEquals(previous, vm.state.query)
        }
        assertEquals(InventoryBriefError.SCAN, vm.state.briefError)
        vm.onBriefErrorShown()
        assertNull(vm.state.briefError)
    }

    @Test
    fun `permission revocation removes add entry points and closes the chooser`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onShowAdd()
        assertFalse(vm.state.showAddSheet)
        repository.context.value = InventoryContext(canAdd = true)
        advanceUntilIdle()
        vm.onShowAdd()
        assertTrue(vm.state.showAddSheet)
        repository.context.value = InventoryContext(canAdd = false)
        advanceUntilIdle()
        assertFalse(vm.state.canAdd)
        assertFalse(vm.state.showAddSheet)
    }

    @Test
    fun `repeated refresh joins one request and failure keeps query filters rows and local updates`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onProductQueryChanged("Sunny")
        val products = vm.state.products
        repository.refreshGate = CompletableDeferred()
        repository.refreshResult = Either.Left(SyncError.Network)
        vm.onRefresh()
        vm.onRefresh()
        runCurrent()
        assertTrue(vm.state.refreshing)
        assertEquals(1, repository.refreshes)
        assertSame(products, vm.state.products)
        repository.categories.value = listOf(Collection("wano", "Wano", "wano", inventoryTime, inventoryTime))
        repository.refreshGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.refreshing)
        assertEquals(InventoryBriefError.REFRESH, vm.state.briefError)
        assertEquals("Sunny", vm.state.query.text)
        assertEquals(listOf("Wano"), vm.state.categories!!.map { it.name })
        assertSame(products, vm.state.products)
    }

    @Test
    fun `local read failure remains retryable and counts start unknown instead of empty`() = runTest(dispatcher) {
        repository.failLocal = true
        val vm = viewModel()
        assertNull(vm.state.totalProducts)
        assertNull(vm.state.categories)
        advanceUntilIdle()
        assertTrue(vm.state.localDataError)
        repository.failLocal = false
        vm.onRetryLocal()
        advanceUntilIdle()
        assertFalse(vm.state.localDataError)
        assertEquals(4, vm.state.totalProducts)
        assertEquals(emptyList<InventoryCategoryRow>(), vm.state.categories)
    }

    private fun viewModel(): InventoryViewModel {
        val vm = InventoryViewModel(repository)
        store.put("inventory", vm)
        return vm
    }

    private class FakeInventoryRepository : InventoryRepository {
        val context = MutableStateFlow(InventoryContext())
        val categories = MutableStateFlow(emptyList<Collection>())
        val queries = mutableListOf<CatalogQuery>()
        var refreshes = 0
        var refreshGate: CompletableDeferred<Unit>? = null
        var refreshResult: Either<SyncError, Unit> = Either.Right(Unit)
        var failLocal = false

        override fun observeContext(): Flow<InventoryContext> { return context }
        override fun observeCategories(): Flow<List<Collection>> {
            return if (failLocal) flow { throw IllegalStateException("Local cache unavailable") } else categories
        }
        override fun observeProductCount(): Flow<Int> {
            return if (failLocal) flow { throw IllegalStateException("Local cache unavailable") } else flowOf(4)
        }
        override fun observeProducts(query: CatalogQuery): Flow<PagingData<Product>> {
            queries += query
            return flowOf(PagingData.from(listOf(sunnyProduct())))
        }
        override suspend fun refresh(): Either<SyncError, Unit> {
            refreshes++
            refreshGate?.await()
            return refreshResult
        }
    }
}
