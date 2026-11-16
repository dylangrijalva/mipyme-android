package co.mipyme.screen.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.data.inventory.InventoryRepository
import co.mipyme.database.CatalogQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _stateFlow = MutableStateFlow(InventoryScreenUiState())
    val stateFlow: StateFlow<InventoryScreenUiState> = _stateFlow.asStateFlow()
    val state: InventoryScreenUiState
        get() = _stateFlow.value

    private val retryLocal = MutableStateFlow(0)
    private val context = retryLocal.flatMapLatest {
        repository.observeContext().catch {
            _stateFlow.update { it.copy(localDataError = true) }
            emit(InventoryContext())
        }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, InventoryContext())
    private var productScope: CoroutineScope? = null
    private var refreshJob: Job? = null

    init {
        replaceProducts(state.query)
        viewModelScope.launch {
            retryLocal.flatMapLatest {
                repository.observeProductCount().catch { _stateFlow.update { it.copy(localDataError = true) } }
            }
                .collect { count -> _stateFlow.update { it.copy(totalProducts = count) } }
        }
        viewModelScope.launch {
            retryLocal.flatMapLatest {
                repository.observeCategories().catch { _stateFlow.update { it.copy(localDataError = true) } }
            }
                .collect { categories ->
                    _stateFlow.update { current ->
                        val rows = categories.map { InventoryCategoryRow(it.id, it.name) }
                        current.copy(categories = rows, visibleCategories = rows.search(current.categoryQuery))
                    }
                }
        }
        viewModelScope.launch {
            context.collect { context ->
                _stateFlow.update { it.copy(canAdd = context.canAdd, showAddSheet = it.showAddSheet && context.canAdd) }
            }
        }
    }

    fun onTabSelected(tab: InventoryTab) {
        _stateFlow.update { it.copy(tab = tab) }
    }

    fun onProductQueryChanged(text: String) {
        applyQuery(state.query.copy(text = text))
    }

    fun onCategoryQueryChanged(text: String) {
        _stateFlow.update { it.copy(categoryQuery = text, visibleCategories = it.categories.orEmpty().search(text)) }
    }

    fun onOpenFilters() {
        _stateFlow.update { it.copy(draft = it.query) }
    }

    fun onDraftChanged(draft: CatalogQuery) {
        _stateFlow.update { if (it.draft == null) it else it.copy(draft = draft) }
    }

    fun onDismissFilters() {
        _stateFlow.update { it.copy(draft = null) }
    }

    fun onApplyFilters() {
        val draft = state.draft ?: return
        applyQuery(draft.copy(text = state.query.text))
        onDismissFilters()
    }

    fun onClearFilters() {
        applyQuery(state.query.clearFilters())
    }

    fun onShowAdd() {
        _stateFlow.update { it.copy(showAddSheet = it.canAdd) }
    }

    fun onDismissAdd() {
        _stateFlow.update { it.copy(showAddSheet = false) }
    }

    fun onScanResult(result: InventoryScanResult) {
        when (result) {
            is InventoryScanResult.Success -> {
                if (result.text.isNotBlank()) applyQuery(state.query.clearFilters().copy(text = result.text))
                else _stateFlow.update { it.copy(briefError = InventoryBriefError.SCAN) }
            }
            InventoryScanResult.Cancelled -> Unit
            InventoryScanResult.Failure -> _stateFlow.update { it.copy(briefError = InventoryBriefError.SCAN) }
        }
    }

    fun onBriefErrorShown() {
        _stateFlow.update { it.copy(briefError = null) }
    }

    fun onRefresh() {
        if (refreshJob?.isActive == true) return
        _stateFlow.update { it.copy(refreshing = true) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh().onLeft { _stateFlow.update { it.copy(briefError = InventoryBriefError.REFRESH) } }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _stateFlow.update { it.copy(briefError = InventoryBriefError.REFRESH) }
            } finally {
                _stateFlow.update { it.copy(refreshing = false) }
            }
        }
    }

    fun onRetryLocal() {
        _stateFlow.update { it.copy(localDataError = false) }
        retryLocal.update { it + 1 }
        replaceProducts(state.query)
    }

    private fun applyQuery(query: CatalogQuery) {
        if (state.query == query) return
        replaceProducts(query)
    }

    private fun replaceProducts(query: CatalogQuery) {
        productScope?.cancel()
        val scope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
        productScope = scope
        val source = repository.observeProducts(query).cachedIn(scope)
        val rows = combine(
            source,
            context.map { it.copy(canAdd = false) }.distinctUntilChanged(),
        ) { page, context ->
            page.map { product -> InventoryListItem.ProductItem(product.toInventoryRow(context, query.grouping)) }
                .insertSeparators<InventoryListItem.ProductItem, InventoryListItem> { before, after ->
                    val group = after?.row?.group
                    if (group != null && before?.row?.group?.key != group.key) InventoryListItem.Header(group) else null
                }
        }.flowOn(Dispatchers.Default).cachedIn(scope)
        _stateFlow.update { it.copy(query = query, products = rows) }
    }

    private fun List<InventoryCategoryRow>.search(text: String): List<InventoryCategoryRow> {
        return filter { it.name.contains(text, ignoreCase = true) }
    }
}
