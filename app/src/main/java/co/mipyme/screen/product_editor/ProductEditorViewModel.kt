package co.mipyme.screen.product_editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.OptionalField
import co.mipyme.contract.Product
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.data.product_editor.ProductEditorContext
import co.mipyme.data.product_editor.ProductEditorRepository
import co.mipyme.network.NetworkError
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProductEditorViewModel(
    private val repository: ProductEditorRepository,
    productType: ProductType,
    private val productId: String? = null,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val restored = savedState.get<ProductEditorCheckpoint>(CHECKPOINT_KEY)
    private val initialState = ProductEditorState(productType = productType, isEditing = productId != null)
    private val _stateFlow = MutableStateFlow(restored?.restore(initialState) ?: initialState)
    val stateFlow: StateFlow<ProductEditorState> = _stateFlow.asStateFlow()
    val state: ProductEditorState get() = _stateFlow.value

    private var contextReady = false
    private var contextJob: Job? = null
    private var productJob: Job? = null
    private var originalProduct: Product? = null
    private var originalFields = restored?.originalFields ?: initialState.savedFields()
    private var persistedProduct = restored?.savedProduct
    private var pendingProduct = restored?.pendingProduct
    private var pendingUpdate = restored?.pendingUpdate
    private var restoredInventory = emptyMap<String, StockDraft>()
    private var restoreEditFields = productId != null && persistedProduct?.id == productId
    private var photoMayHaveBeenSaved = restored?.photoMayHaveBeenSaved ?: false
    private var clearPhoto = restored?.clearPhoto ?: false
    private var photoUpload = restored?.photoUpload
    private val pendingCollections = mutableMapOf<String, CollectionCreation>()
    private var failedCollectionName = restored?.failedCollectionName
    private var creatingCollectionName: String? = null

    init {
        restored?.pendingCollection?.let { pendingCollections[it.name.lowercase()] = it }
        photoUpload?.let(repository::restorePhotoUploadCheckpoint)
        viewModelScope.launch {
            repository.observePhotoUploadCheckpoint().collect {
                photoUpload = it
                checkpoint()
            }
        }
        if (state.savedProductId == null) load()
    }

    fun setName(value: String) {
        edit { it.copy(name = value) }
    }

    fun setBarcode(value: String) {
        if (state.showsBarcode) edit { it.copy(barcode = value) }
    }

    fun setPrice(value: String) {
        edit { it.copy(price = value) }
    }

    fun setPhoto(uri: Uri): Boolean {
        val current = state
        if ((contextReady && !current.canManageCatalog) || current.isSaving ||
            current.isCreatingCollection || current.savedProductId != null
        ) return false
        repository.discardPhotoUpload()
        photoUpload = null
        clearPhoto = false
        updateForm { it.copy(photoUri = uri) }
        return true
    }

    fun removePhoto() {
        if (!state.canEdit) return
        repository.discardPhotoUpload()
        photoUpload = null
        clearPhoto = photoMayHaveBeenSaved
        updateForm { it.copy(photoUri = null, photoUrl = null) }
    }

    fun reportPhotoError() {
        reportBriefError(ProductEditorError.PHOTO)
    }

    fun reportScanError() {
        reportBriefError(ProductEditorError.SCAN)
    }

    fun dismissError() {
        updateState {
            if (it.error == ProductEditorError.PHOTO || it.error == ProductEditorError.SCAN) {
                it.copy(error = null)
            } else {
                it
            }
        }
    }

    fun setQuantity(locationId: String, value: String) {
        editLocation(locationId) { if (it.quantityReadOnly) it else it.copy(quantity = value) }
    }

    fun setLowStock(locationId: String, value: String) {
        editLocation(locationId) { it.copy(lowStock = value) }
    }

    fun setOptimalStock(locationId: String, value: String) {
        editLocation(locationId) { it.copy(optimalStock = value) }
    }

    fun openPricing(): Boolean {
        if (!state.canEdit || state.isService) return false
        updateState { it.copy(pricingDraft = it.pricing) }
        return true
    }

    fun setDraftPrice(value: String) {
        editPricing { it.copy(price = value) }
    }

    fun setDraftUnitCost(value: String) {
        editPricing { if (it.unitCostReadOnly) it else it.copy(unitCost = value) }
    }

    fun applyPricing(): Boolean {
        val draft = state.pricingDraft ?: return false
        if (!state.canEdit || !draft.canApply) return false
        updateForm { it.copy(price = draft.price, unitCost = draft.unitCost, pricingDraft = null) }
        return true
    }

    fun discardPricing() {
        updateState { it.copy(pricingDraft = null) }
    }

    fun openCollections(): Boolean {
        if (!state.canEdit) return false
        failedCollectionName = null
        updateState { it.copy(collectionQuery = "", collectionError = false, collectionSelectionApplied = false) }
        return true
    }

    fun setCollectionQuery(value: String) {
        if (state.canEdit) updateState { it.copy(collectionQuery = value) }
    }

    fun selectCollection(collection: ProductEditorCollection?) {
        if (state.canEdit) applyCollection(collection)
    }

    fun clearCollectionSelectionApplied() {
        updateState { it.copy(collectionSelectionApplied = false) }
    }

    fun createCollection() {
        if (!state.canCreateCollection) return
        createCollection(state.collectionQuery.trim())
    }

    fun retryCollection() {
        if (!state.canEdit) return
        val name = failedCollectionName ?: return
        val existing = findCollection(name)
        if (existing != null) applyCollection(existing) else createCollection(name)
    }

    fun save() {
        val draft = state
        if (!draft.canSave) return
        updateState { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val productId = persist(draft).fold(
                    { failure ->
                        showSaveError(failure)
                        return@launch
                    },
                    { it },
                )
                val uri = draft.photoUri
                if (uri != null && !uploadPhoto(productId, uri)) {
                    showSaveError(ProductEditorError.PHOTO_SAVE)
                    return@launch
                }
                updateState {
                    it.copy(isSaving = false, isDirty = false, hasSaveChanges = false, savedProductId = productId)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showSaveError(writeBlockError() ?: ProductEditorError.SAVE)
            }
        }
    }

    fun retry() {
        when (state.error) {
            ProductEditorError.LOAD -> if (!state.isLoading) load()
            null, ProductEditorError.PHOTO, ProductEditorError.SCAN -> Unit
            else -> save()
        }
    }

    override fun onCleared() {
        repository.discardPhotoUpload()
        super.onCleared()
    }

    private fun load() {
        updateState { it.copy(isLoading = true, error = it.error.takeUnless { error -> error == ProductEditorError.LOAD }) }
        if (!contextReady) {
            contextJob?.cancel()
            contextJob = viewModelScope.launch {
                try {
                    repository.observeContext().collect { context ->
                        contextReady = true
                        applyContext(context)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    contextReady = false
                    showLoadError()
                }
            }
        }
        if (productId == null || originalProduct != null || productJob?.isActive == true) return
        productJob = viewModelScope.launch {
            try {
                repository.findProduct(productId).fold({ showLoadError() }, { initializeEdit(it) })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showLoadError()
            }
        }
    }

    private fun showLoadError() {
        updateState {
            it.copy(error = ProductEditorError.LOAD, isLoading = false)
        }
    }

    private fun applyContext(context: ProductEditorContext) {
        updateForm { current ->
            val collections = context.collections.map { ProductEditorCollection(it.id, it.name) }
                .sortedBy { it.name.lowercase() }
            val selected = current.selectedCollection
            val currentLocations = current.locations.associateBy { it.id }
            current.copy(
                isLoading = current.error != ProductEditorError.LOAD && productId != null && originalProduct == null,
                canManageCatalog = context.canManageCatalog,
                isOffline = !context.isOnline,
                country = context.country,
                collections = if (selected != null && collections.none { it.id == selected.id }) {
                    (collections + selected).sortedBy { it.name.lowercase() }
                } else {
                    collections
                },
                selectedCollection = collections.firstOrNull { it.id == selected?.id } ?: selected,
                locations = if (current.isService) emptyList() else context.locations.map { location ->
                    currentLocations[location.id]?.copy(name = location.name) ?: restoredLocation(location.id, location.name)
                },
            )
        }
    }

    private fun initializeEdit(product: Product) {
        originalProduct = product
        val preserveDraft = restoreEditFields
        restoreEditFields = false
        val fields = product.savedFields()
        val hasDefaultVariant = product.variants.size == 1
        val isService = product.productType == ProductType.SERVICE
        val unitCostReadOnly = product.status != ProductStatus.DRAFT
        if (preserveDraft) {
            fun protectedFields(draft: ProductEditorFields): ProductEditorFields {
                return draft.copy(
                    barcode = if (hasDefaultVariant && !isService) draft.barcode else fields.barcode,
                    unitCost = if (unitCostReadOnly || isService) fields.unitCost else draft.unitCost,
                )
            }
            originalFields = protectedFields(originalFields)
            persistedProduct = persistedProduct?.let { it.copy(fields = protectedFields(it.fields)) }
            pendingUpdate = pendingUpdate?.let { it.copy(fields = protectedFields(it.fields)) }
        } else {
            originalFields = fields
            persistedProduct = SavedProduct(product.id, fields)
        }
        updateForm { current ->
            val collection = if (preserveDraft) {
                current.selectedCollection
            } else {
                product.collection?.let { ProductEditorCollection(it.id, it.name) }
            }
            val cost = if (preserveDraft && !unitCostReadOnly) current.unitCost else fields.unitCost
            current.copy(
                productType = product.productType,
                isLoading = current.error != ProductEditorError.LOAD && !contextReady,
                name = if (preserveDraft) current.name else fields.name,
                barcode = if (preserveDraft && hasDefaultVariant) current.barcode else fields.barcode.orEmpty(),
                hasDefaultVariant = hasDefaultVariant,
                price = if (preserveDraft) current.price else fields.price,
                unitCost = cost,
                requiresUnitCost = !isService,
                unitCostReadOnly = unitCostReadOnly,
                pricingDraft = current.pricingDraft?.let {
                    it.copy(
                        unitCost = if (unitCostReadOnly) cost else it.unitCost,
                        unitCostReadOnly = unitCostReadOnly,
                    )
                },
                selectedCollection = collection,
                collections = if (collection != null && current.collections.none { it.id == collection.id }) {
                    (current.collections + collection).sortedBy { it.name.lowercase() }
                } else {
                    current.collections
                },
                photoUrl = if (preserveDraft) current.photoUrl else product.imageUrl?.value,
                locations = if (isService) emptyList() else current.locations.map { restoredLocation(it.id, it.name) },
            )
        }
    }

    private fun restoredLocation(id: String, name: String): ProductEditorLocation {
        val location = locationFields(id, name)
        return restoredInventory[id]?.merge(location) ?: location
    }

    private fun locationFields(id: String, name: String): ProductEditorLocation {
        val product = originalProduct ?: return ProductEditorLocation(id, name)
        val levels = product.variants.map { variant ->
            variant.inventoryLevels.firstOrNull { it.locationId == id }
        }
        val quantities = levels.map { it?.onHand }
        val onHand = if (quantities.isEmpty() || quantities.any { it == null || !it.isFinite() }) {
            ""
        } else {
            quantities.filterNotNull().fold(BigDecimal.ZERO) { sum, number -> sum + BigDecimal.valueOf(number) }
                .stripTrailingZeros().toPlainString()
        }
        val threshold = levels.singleOrNull()
        return ProductEditorLocation(
            id = id,
            name = name,
            quantity = onHand,
            lowStock = threshold?.lowStockThreshold.inputText(),
            optimalStock = threshold?.optimalStockLevel.inputText(),
            quantityReadOnly = true,
        )
    }

    private fun edit(transform: (ProductEditorState) -> ProductEditorState) {
        if (state.canEdit) updateForm(transform)
    }

    private fun editLocation(id: String, transform: (ProductEditorLocation) -> ProductEditorLocation) {
        edit { current -> current.copy(locations = current.locations.map { if (it.id == id) transform(it) else it }) }
    }

    private fun editPricing(transform: (ProductPricingDraft) -> ProductPricingDraft) {
        if (!state.canEdit) return
        updateState { it.copy(pricingDraft = it.pricingDraft?.let(transform)) }
    }

    private fun updateForm(transform: (ProductEditorState) -> ProductEditorState) {
        updateState { current ->
            val next = transform(current)
            if (next.savedProductId != null) return@updateState next
            val fields = next.savedFields()
            val hasSaveChanges = fields != originalFields || next.photoUri != null || clearPhoto ||
                pendingUpdate != null ||
                persistedProduct?.fields?.let { fields != it } == true
            val stockChanged = next.locations.any { location ->
                val original = locationFields(location.id, location.name)
                location.quantity.normalizedDecimal() != original.quantity.normalizedDecimal() ||
                    location.lowStock.normalizedDecimal() != original.lowStock.normalizedDecimal() ||
                    location.optimalStock.normalizedDecimal() != original.optimalStock.normalizedDecimal()
            }
            next.copy(hasSaveChanges = hasSaveChanges, isDirty = hasSaveChanges || stockChanged)
        }
    }

    private fun updateState(transform: (ProductEditorState) -> ProductEditorState) {
        _stateFlow.update(transform)
        checkpoint()
    }

    private fun checkpoint() {
        val current = state
        val hasInventoryBaseline = contextReady && (productId == null || originalProduct != null)
        val inventory = if (hasInventoryBaseline) {
            current.locations.mapNotNull { location ->
                val original = locationFields(location.id, location.name)
                val quantity = location.quantity.takeUnless { location.quantityReadOnly || it == original.quantity }
                val lowStock = location.lowStock.takeUnless { it == original.lowStock }
                val optimalStock = location.optimalStock.takeUnless { it == original.optimalStock }
                if (quantity == null && lowStock == null && optimalStock == null) return@mapNotNull null
                location.id to StockDraft(
                    quantity?.checkpointDecimal(),
                    lowStock?.checkpointDecimal(),
                    optimalStock?.checkpointDecimal(),
                )
            }.take(MAX_CHECKPOINT_LOCATIONS).toMap()
        } else {
            restoredInventory
        }
        if (hasInventoryBaseline) restoredInventory = emptyMap()
        val retryCollection = creatingCollectionName ?: failedCollectionName
        val photoUri = current.photoUri?.toString()?.takeIf { it.length <= MAX_CHECKPOINT_URI_LENGTH }
        savedState[CHECKPOINT_KEY] = ProductEditorCheckpoint(
            name = current.name.checkpointText(),
            barcode = current.barcode.checkpointText(),
            price = current.price.checkpointDecimal(),
            unitCost = current.unitCost.checkpointDecimal(),
            collection = current.selectedCollection,
            photoUri = photoUri,
            photoUrl = current.photoUrl,
            pricingDraft = current.pricingDraft?.let {
                it.copy(price = it.price.checkpointDecimal(), unitCost = it.unitCost.checkpointDecimal())
            },
            inventory = inventory,
            originalFields = originalFields,
            savedProduct = persistedProduct,
            pendingProduct = pendingProduct,
            pendingUpdate = pendingUpdate,
            collectionQuery = current.collectionQuery.checkpointText(),
            failedCollectionName = retryCollection,
            pendingCollection = retryCollection?.let { pendingCollections[it.lowercase()] },
            collectionSelectionApplied = current.collectionSelectionApplied,
            error = when {
                current.isSaving && photoMayHaveBeenSaved && current.photoUri != null -> ProductEditorError.PHOTO_SAVE
                current.isSaving -> ProductEditorError.SAVE
                else -> current.error
            },
            completedProductId = current.savedProductId,
            photoMayHaveBeenSaved = photoMayHaveBeenSaved,
            clearPhoto = clearPhoto,
            photoUpload = photoUpload?.takeIf { it.productId == persistedProduct?.id && it.uri == photoUri },
        )
    }

    private fun reportBriefError(error: ProductEditorError) {
        updateState {
            when (it.error) {
                null, ProductEditorError.PHOTO, ProductEditorError.SCAN -> it.copy(error = error)
                else -> it
            }
        }
    }

    private fun findCollection(name: String): ProductEditorCollection? {
        return state.collections.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    private fun createCollection(name: String) {
        val key = name.lowercase()
        val creation = pendingCollections.getOrPut(key) { CollectionCreation(name, UUID.randomUUID().toString()) }
        creatingCollectionName = name
        updateState { it.copy(isCreatingCollection = true, collectionError = false) }
        viewModelScope.launch {
            try {
                repository.createCollection(CreateCollectionRequest(creation.name), creation.key).fold(
                    { failure ->
                        if (failure.isDefinitiveClientError()) pendingCollections.remove(key)
                        val existing = findCollection(name)
                        if (existing != null) applyCollection(existing) else showCollectionError(name)
                    },
                    { collection ->
                        pendingCollections.remove(key)
                        applyCollection(ProductEditorCollection(collection.id, collection.name))
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showCollectionError(name)
            }
        }
    }

    private fun applyCollection(collection: ProductEditorCollection?) {
        creatingCollectionName = null
        failedCollectionName = null
        if (collection != null) pendingCollections.remove(collection.name.lowercase())
        updateForm { current ->
            val collections = current.collections
            current.copy(
                selectedCollection = collection,
                collections = if (collection != null && collections.none { it.id == collection.id }) {
                    (collections + collection).sortedBy { it.name.lowercase() }
                } else {
                    collections
                },
                isCreatingCollection = false,
                collectionSelectionApplied = true,
                collectionError = false,
            )
        }
    }

    private fun showCollectionError(name: String) {
        creatingCollectionName = null
        failedCollectionName = name
        updateState { it.copy(isCreatingCollection = false, collectionError = true) }
    }

    private fun writeBlockError(): ProductEditorError? {
        return when {
            !contextReady -> ProductEditorError.LOAD
            !state.canManageCatalog -> ProductEditorError.SAVE_ACCESS
            state.isOffline -> ProductEditorError.SAVE_OFFLINE
            else -> null
        }
    }

    private suspend fun uploadPhoto(id: String, uri: Uri): Boolean {
        if (writeBlockError() != null) return false
        photoMayHaveBeenSaved = true
        checkpoint()
        return try {
            repository.uploadPhoto(id, uri).isRight()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun persist(draft: ProductEditorState): Either<ProductEditorError, String> {
        val fields = draft.savedFields()
        var saved = persistedProduct
        writeBlockError()?.let { return it.left() }
        if (saved == null) {
            val creation = pendingProduct ?: ProductCreation(
                fields = fields,
                key = UUID.randomUUID().toString(),
            ).also {
                pendingProduct = it
                checkpoint()
            }
            val request = creation.fields.createRequest(draft.productType)
            val created = repository.createProduct(request, creation.key).fold(
                { failure ->
                    if (failure.isDefinitiveClientError()) pendingProduct = null
                    return failure.toSaveError().left()
                },
                { it },
            )
            saved = SavedProduct(created.id, creation.fields)
            persistedProduct = saved
            pendingProduct = null
            checkpoint()
        }
        pendingUpdate?.let { update ->
            saved = applyUpdate(requireNotNull(saved), update).fold(
                { return it.left() },
                { it },
            )
        }
        val confirmed = requireNotNull(saved)
        val applied = if (draft.photoUri == null) fields else fields.copy(photoUrl = confirmed.fields.photoUrl)
        return applyUpdate(confirmed, ProductUpdate(applied, clearPhoto && draft.photoUri == null)).map { it.id }
    }

    private suspend fun applyUpdate(saved: SavedProduct, update: ProductUpdate): Either<ProductEditorError, SavedProduct> {
        val request = update.fields.updateRequest(saved.fields, update.clearPhoto)
        if (request == null) {
            if (pendingUpdate == update) {
                pendingUpdate = null
                checkpoint()
            }
            return saved.right()
        }
        writeBlockError()?.let { return it.left() }
        pendingUpdate = update
        checkpoint()
        return repository.updateProduct(saved.id, request).fold(
            { failure ->
                if (failure.isDefinitiveClientError()) pendingUpdate = null
                failure.toSaveError().left()
            },
            { product ->
                val confirmed = SavedProduct(product.id, update.fields)
                persistedProduct = confirmed
                pendingUpdate = null
                if (request.imageUrl is OptionalField.Present) {
                    photoMayHaveBeenSaved = false
                    clearPhoto = false
                }
                checkpoint()
                confirmed.right()
            },
        )
    }

    private fun showSaveError(error: ProductEditorError) {
        updateForm {
            it.copy(isSaving = false, error = if (contextReady) error else ProductEditorError.LOAD)
        }
    }

}

private const val CHECKPOINT_KEY = "product_editor_checkpoint"
private const val MAX_CHECKPOINT_LOCATIONS = 60
private const val MAX_CHECKPOINT_URI_LENGTH = 8192

private fun Double?.inputText(): String {
    if (this == null || !isFinite()) return ""
    return BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
}

private fun NetworkError.isDefinitiveClientError(): Boolean {
    if (this !is NetworkError.Response) return false
    return when (error.code) {
        MipymeErrorCode.VALIDATION_ERROR,
        MipymeErrorCode.BARCODE_ALREADY_EXISTS,
        MipymeErrorCode.SKU_ALREADY_EXISTS,
        MipymeErrorCode.PRICE_BELOW_UNIT_COST,
        MipymeErrorCode.COLLECTION_NAME_ALREADY_EXISTS,
        MipymeErrorCode.RESOURCE_NOT_FOUND -> true
        else -> false
    }
}

private fun NetworkError.toSaveError(): ProductEditorError {
    return when (this) {
        is NetworkError.NetworkUnavailable -> ProductEditorError.SAVE_OFFLINE
        is NetworkError.Timeout -> ProductEditorError.SAVE_TIMEOUT
        is NetworkError.Response -> when (error.code) {
            MipymeErrorCode.BARCODE_ALREADY_EXISTS -> ProductEditorError.BARCODE_ALREADY_EXISTS
            MipymeErrorCode.FORBIDDEN_ERROR, MipymeErrorCode.INVALID_OAUTH_TOKEN -> ProductEditorError.SAVE_ACCESS
            MipymeErrorCode.VALIDATION_ERROR, MipymeErrorCode.PRICE_BELOW_UNIT_COST ->
                ProductEditorError.PRODUCT_VALIDATION
            else -> ProductEditorError.SAVE
        }
        else -> ProductEditorError.SAVE
    }
}
