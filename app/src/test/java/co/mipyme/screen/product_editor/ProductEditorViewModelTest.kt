package co.mipyme.screen.product_editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.contract.Collection
import co.mipyme.contract.Decimal
import co.mipyme.contract.InventoryLevel
import co.mipyme.contract.MipymeError
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.OptionalField
import co.mipyme.contract.Product
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.contract.ProductVariant
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.UpdateProductRequest
import co.mipyme.data.product_editor.ProductEditorContext
import co.mipyme.data.product_editor.ProductEditorRepository
import co.mipyme.network.NetworkError
import co.mipyme.testing.location
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeProductEditorRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `pricing calculates losses before apply and discard preserves the editor`() = runTest(dispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.setName("Clima-Tact")
        vm.setQuantity(WATER_7, "2,75")
        assertTrue(vm.openPricing())
        vm.setDraftPrice("8,00")
        vm.setDraftUnitCost("10")

        val draft = requireNotNull(vm.state.pricingDraft)
        assertEquals(BigDecimal("-2.00"), draft.grossProfit)
        assertEquals(BigDecimal("-25.00"), draft.marginPercent)
        assertEquals(ProductEditorFieldError.BELOW_COST, draft.priceError)
        assertTrue(vm.state.isPricingDirty)
        assertFalse(vm.applyPricing())
        vm.discardPricing()
        assertEquals("", vm.state.price)
        assertEquals("2,75", vm.state.locations.single().quantity)
        assertFalse(vm.state.canSave)

        assertTrue(vm.openPricing())
        vm.setDraftPrice("12.50")
        vm.setDraftUnitCost("0")
        assertFalse(vm.applyPricing())
        vm.setDraftUnitCost("10")
        assertTrue(vm.applyPricing())
        assertEquals("12.50", vm.state.price)
        assertEquals("2,75", vm.state.locations.single().quantity)
        assertTrue(vm.state.canSave)
    }

    @Test
    fun `hidden inventory drafts do not block product saves`() = runTest(dispatcher) {
        val vm = editor(productId = PRODUCT_ID)
        advanceUntilIdle()
        assertFalse(vm.state.isDirty)
        assertFalse(vm.state.canSave)
        val location = vm.state.locations.single()
        assertEquals("-1.25", location.quantity)
        assertEquals("2.5", location.lowStock)
        assertEquals("5", location.optimalStock)

        vm.setQuantity(WATER_7, "5.5")
        assertEquals("-1.25", vm.state.locations.single().quantity)
        vm.setLowStock(WATER_7, "6")
        assertEquals(ProductEditorFieldError.BELOW_LOW_STOCK, vm.state.locations.single().optimalStockError)
        vm.setName("Upgraded Clima-Tact")
        assertTrue(vm.state.canSave)
        vm.setOptimalStock(WATER_7, "6.5")
        vm.setName("Clima-Tact")
        assertTrue(vm.state.isDirty)
        assertFalse(vm.state.canSave)

        vm.setName("Upgraded Clima-Tact")
        assertTrue(vm.state.canSave)
        vm.save()
        advanceUntilIdle()
        assertEquals(PRODUCT_ID, vm.state.savedProductId)
        assertEquals(
            listOf(UpdateProductRequest(name = OptionalField.Present("Upgraded Clima-Tact"))),
            repository.updates,
        )
    }

    @Test
    fun `an existing item cost stays required and a photo can be removed without resending other fields`() =
        runTest(dispatcher) {
            val vm = editor(productId = PRODUCT_ID)
            advanceUntilIdle()
            assertTrue(vm.openPricing())
            vm.setDraftUnitCost("")
            assertEquals(ProductEditorFieldError.REQUIRED, vm.state.pricingDraft?.unitCostError)
            assertFalse(vm.applyPricing())
            vm.discardPricing()
            vm.removePhoto()
            vm.save()
            advanceUntilIdle()

            assertEquals(PRODUCT_ID, vm.state.savedProductId)
            assertEquals(listOf(UpdateProductRequest(imageUrl = OptionalField.Present(null))), repository.updates)
        }

    @Test
    fun `a lost create response replays the same request then retries a failed patch on the confirmed id`() =
        runTest(dispatcher) {
            val vm = editor(ProductType.SERVICE)
            advanceUntilIdle()
            vm.setName("Navigation lesson")
            vm.setPrice("12.50")
            vm.setBarcode("8001234")
            assertFalse(vm.openPricing())
            assertTrue(vm.state.locations.isEmpty())
            repository.loseCreateResponse = true
            vm.save()
            advanceUntilIdle()
            assertEquals(ProductEditorError.SAVE_TIMEOUT, vm.state.error)

            vm.setName("Advanced navigation lesson")
            assertEquals(ProductEditorError.SAVE_TIMEOUT, vm.state.error)
            repository.nextCreateFailure = response(MipymeErrorCode.RATE_LIMIT_EXCEEDED, 429)
            vm.retry()
            advanceUntilIdle()
            assertEquals(ProductEditorError.SAVE, vm.state.error)

            repository.failUpdate = true
            vm.retry()
            advanceUntilIdle()
            assertEquals(ProductEditorError.SAVE_TIMEOUT, vm.state.error)
            assertNull(vm.state.savedProductId)
            assertEquals(3, repository.creates.size)
            assertEquals(1, repository.creates.distinct().size)
            val request = repository.creates.first().first
            assertEquals("12.5", request.price.value)
            assertNull(request.unitCost)
            assertNull(request.barcode)

            vm.retry()
            advanceUntilIdle()
            assertEquals(PRODUCT_ID, vm.state.savedProductId)
            assertEquals(3, repository.creates.size)
            assertEquals(
                List(2) { UpdateProductRequest(name = OptionalField.Present("Advanced navigation lesson")) },
                repository.updates,
            )
        }

    @Test
    fun `a known barcode rejection stays visible and a corrected barcode can be saved`() = runTest(dispatcher) {
        val vm = editor()
        advanceUntilIdle()
        vm.setName("Clima-Tact")
        vm.setBarcode("8001234")
        vm.openPricing()
        vm.setDraftPrice("10")
        vm.setDraftUnitCost("9")
        vm.applyPricing()
        repository.nextCreateFailure = response(MipymeErrorCode.BARCODE_ALREADY_EXISTS, 409)
        vm.save()
        advanceUntilIdle()
        assertEquals(ProductEditorError.BARCODE_ALREADY_EXISTS, vm.state.error)

        vm.setBarcode("8004321")
        vm.reportScanError()
        assertEquals(ProductEditorError.BARCODE_ALREADY_EXISTS, vm.state.error)
        vm.retry()
        advanceUntilIdle()
        assertEquals(PRODUCT_ID, vm.state.savedProductId)
        assertEquals(listOf("8001234", "8004321"), repository.creates.map { it.first.barcode })
        assertEquals(2, repository.creates.map { it.second }.distinct().size)
    }

    @Test
    fun `collection retry keeps the failed name and key after the search changes`() = runTest(dispatcher) {
        val vm = editor(ProductType.SERVICE)
        advanceUntilIdle()
        assertTrue(vm.openCollections())
        vm.setCollectionQuery("  wAnO ")
        assertFalse(vm.state.canCreateCollection)
        assertEquals(listOf("Wano"), vm.state.visibleCollections.map { it.name })
        vm.setCollectionQuery("  Baratie ")
        assertTrue(vm.state.canCreateCollection)
        repository.loseCollectionResponse = true
        vm.createCollection()
        advanceUntilIdle()
        assertTrue(vm.state.collectionError)

        vm.setCollectionQuery("Whole Cake Island")
        vm.retryCollection()
        advanceUntilIdle()
        assertEquals(ProductEditorCollection("baratie", "Baratie"), vm.state.selectedCollection)
        assertTrue(vm.state.collectionSelectionApplied)
        assertFalse(vm.state.collectionError)
        assertEquals(2, repository.collectionCreates.size)
        assertEquals(repository.collectionCreates[0], repository.collectionCreates[1])
        assertTrue(repository.creates.isEmpty())
    }

    @Test
    fun `offline and permission changes block writes while preserving the merchant draft`() = runTest(dispatcher) {
        val vm = editor(ProductType.SERVICE)
        advanceUntilIdle()
        vm.setName("Swordsmanship lesson")
        vm.setPrice("15")
        repository.context.value = repository.context.value.copy(isOnline = false)
        advanceUntilIdle()
        vm.setName("Navigation lesson")
        vm.save()
        assertTrue(vm.state.isOffline)
        assertFalse(vm.state.canSave)
        assertEquals("Swordsmanship lesson", vm.state.name)

        repository.context.value = repository.context.value.copy(isOnline = true, canManageCatalog = false)
        advanceUntilIdle()
        vm.save()
        assertFalse(vm.state.canSave)
        assertTrue(repository.creates.isEmpty())
        repository.context.value = repository.context.value.copy(canManageCatalog = true)
        advanceUntilIdle()
        assertTrue(vm.state.canSave)
    }

    @Test
    fun `a recreated editor restores pricing but discards hidden inventory drafts`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val before = editor(handle = handle)
        advanceUntilIdle()
        before.setName("Clima-Tact")
        before.setBarcode("8001234")
        before.setQuantity(WATER_7, "2.75")
        before.setLowStock(WATER_7, "2")
        before.openPricing()
        before.setDraftPrice("15")
        before.setDraftUnitCost("10")

        val after = editor(handle = recreate(handle))
        advanceUntilIdle()
        assertEquals("Clima-Tact", after.state.name)
        assertEquals("8001234", after.state.barcode)
        assertEquals("", after.state.locations.single().quantity)
        assertEquals("", after.state.locations.single().lowStock)
        assertEquals("", after.state.price)
        assertEquals("15", after.state.pricingDraft?.price)
        assertEquals("10", after.state.pricingDraft?.unitCost)
        assertTrue(after.state.isPricingDirty)
        assertTrue(after.applyPricing())
        assertTrue(after.state.canSave)
    }

    @Test
    fun `process recreation before the create response repeats the same key and original request`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val before = editor(ProductType.SERVICE, handle = handle)
        advanceUntilIdle()
        before.setName("Navigation lesson")
        before.setPrice("12.50")
        repository.createResponse = CompletableDeferred()
        before.save()
        advanceUntilIdle()
        assertTrue(before.state.isSaving)
        assertEquals(1, repository.creates.size)

        repository.createResponse = null
        val after = editor(ProductType.SERVICE, handle = recreate(handle))
        advanceUntilIdle()
        assertEquals(ProductEditorError.SAVE, after.state.error)
        assertEquals("Navigation lesson", after.state.name)
        after.setName("Advanced navigation lesson")
        after.retry()
        advanceUntilIdle()

        assertEquals(PRODUCT_ID, after.state.savedProductId)
        assertEquals(2, repository.creates.size)
        assertEquals(repository.creates[0], repository.creates[1])
        assertEquals("Advanced navigation lesson", repository.savedName)
    }

    @Test
    fun `an uncertain patch replays before applying the reverted name and new price after recreation`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val before = editor(productId = PRODUCT_ID, handle = handle)
        advanceUntilIdle()
        before.setName("Upgraded Clima-Tact")
        repository.loseUpdateResponse = true
        before.save()
        advanceUntilIdle()
        assertEquals("Upgraded Clima-Tact", repository.savedName)
        assertEquals(ProductEditorError.SAVE_TIMEOUT, before.state.error)

        val after = editor(productId = PRODUCT_ID, handle = recreate(handle))
        advanceUntilIdle()
        assertEquals("Upgraded Clima-Tact", after.state.name)
        after.setName("Clima-Tact")
        assertTrue(after.state.canSave)
        after.openPricing()
        after.setDraftPrice("11")
        after.applyPricing()
        after.retry()
        advanceUntilIdle()

        assertEquals("Clima-Tact", repository.savedName)
        assertEquals("11", repository.savedPrice)
        assertEquals(PRODUCT_ID, after.state.savedProductId)
        assertEquals(repository.updates[0], repository.updates[1])
        assertEquals(3, repository.updates.size)
    }

    @Test
    fun `recreating a draft preserves variants and unit cost that have since become read only`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val before = editor(productId = PRODUCT_ID, handle = handle)
        advanceUntilIdle()
        before.setName("Upgraded Clima-Tact")
        before.setBarcode("8001234")
        before.openPricing()
        before.setDraftUnitCost("8")
        before.applyPricing()
        before.openPricing()
        before.setDraftUnitCost("7")
        repository.activateProductWithSecondVariant()

        val after = editor(productId = PRODUCT_ID, handle = recreate(handle))
        advanceUntilIdle()
        assertFalse(after.state.showsBarcode)
        assertTrue(after.state.unitCostReadOnly)
        assertEquals("9", after.state.unitCost)
        assertEquals("9", after.state.pricingDraft?.unitCost)
        assertTrue(after.state.pricingDraft?.unitCostReadOnly == true)
        after.discardPricing()
        after.save()
        advanceUntilIdle()

        assertEquals(PRODUCT_ID, after.state.savedProductId)
        assertEquals(
            listOf(UpdateProductRequest(name = OptionalField.Present("Upgraded Clima-Tact"))),
            repository.updates,
        )
    }

    private fun editor(
        type: ProductType = ProductType.ITEM,
        productId: String? = null,
        handle: SavedStateHandle = SavedStateHandle(),
    ): ProductEditorViewModel {
        return ProductEditorViewModel(repository, type, productId, handle).also { store.put("editor", it) }
    }

    private fun recreate(handle: SavedStateHandle): SavedStateHandle {
        val bytes = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).use { it.writeObject(handle.keys().associateWith { key -> handle.get<Any?>(key) }) }
        }.toByteArray()
        @Suppress("UNCHECKED_CAST")
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Map<String, Any?> }
        store.clear()
        return SavedStateHandle(restored)
    }

    private fun response(code: MipymeErrorCode, status: Int): NetworkError {
        return NetworkError.Response(MipymeError(code, "Fake catalog error", emptyList(), "request", status))
    }

    private class FakeProductEditorRepository : ProductEditorRepository {
        val context = MutableStateFlow(
            ProductEditorContext(
                canManageCatalog = true,
                isOnline = true,
                locations = listOf(location(WATER_7, "Water 7")),
                collections = listOf(Collection("wano", "Wano", "wano", TIME, TIME)),
            ),
        )
        val creates = mutableListOf<Pair<CreateProductRequest, String>>()
        val updates = mutableListOf<UpdateProductRequest>()
        val collectionCreates = mutableListOf<Pair<CreateCollectionRequest, String>>()
        var loseCreateResponse = false
        var createResponse: CompletableDeferred<Unit>? = null
        var nextCreateFailure: NetworkError? = null
        var failUpdate = false
        var loseUpdateResponse = false
        var loseCollectionResponse = false
        private var product = Product(
            id = PRODUCT_ID,
            name = "Clima-Tact",
            slug = "clima-tact",
            unitCost = Decimal("9.00"),
            price = Decimal("10.00"),
            productType = ProductType.ITEM,
            status = ProductStatus.DRAFT,
            imageUrl = ProductImage("https://example.com/clima-tact.webp"),
            collection = null,
            optionGroups = emptyList(),
            variants = listOf(
                ProductVariant(
                    id = "clima-tact-default",
                    name = "Clima-Tact",
                    sku = "CLIMA-TACT",
                    barcode = null,
                    price = null,
                    options = emptyList(),
                    inventoryLevels = listOf(
                        InventoryLevel("clima-tact-water7", "clima-tact-default", WATER_7, -1.25, 2.5, 5.0),
                    ),
                    createdAt = TIME,
                    updatedAt = TIME,
                ),
            ),
            createdAt = TIME,
            updatedAt = TIME,
            archivedAt = null,
        )
        val savedName: String get() = product.name
        val savedPrice: String get() = product.price.value

        fun activateProductWithSecondVariant() {
            product = product.copy(
                status = ProductStatus.ACTIVE,
                variants = product.variants + product.variants.single().copy(id = "clima-tact-alabasta"),
            )
        }

        override fun observeContext(): Flow<ProductEditorContext> {
            return context
        }

        override suspend fun findProduct(id: String): Either<NetworkError, Product> {
            assertEquals(PRODUCT_ID, id)
            return product.right()
        }

        override suspend fun createProduct(request: CreateProductRequest, idempotencyKey: String): Either<NetworkError, Product> {
            creates += request to idempotencyKey
            nextCreateFailure?.let { error ->
                nextCreateFailure = null
                return error.left()
            }
            product = product.copy(
                name = request.name,
                price = Decimal(request.price.value),
                unitCost = request.unitCost ?: Decimal("0"),
                productType = request.productType,
                imageUrl = null,
                variants = product.variants.map { it.copy(barcode = request.barcode) },
            )
            createResponse?.await()
            if (loseCreateResponse) {
                loseCreateResponse = false
                return NetworkError.Timeout(IllegalStateException()).left()
            }
            return product.right()
        }

        override suspend fun updateProduct(id: String, request: UpdateProductRequest): Either<NetworkError, Product> {
            assertEquals(PRODUCT_ID, id)
            updates += request
            if (failUpdate) {
                failUpdate = false
                return NetworkError.Timeout(IllegalStateException()).left()
            }
            product = product.copy(
                name = (request.name as? OptionalField.Present)?.value ?: product.name,
                price = (request.price as? OptionalField.Present)?.value?.let { Decimal(it.value) } ?: product.price,
                imageUrl = when (val imageUrl = request.imageUrl) {
                    OptionalField.Absent -> product.imageUrl
                    is OptionalField.Present -> imageUrl.value
                },
            )
            if (loseUpdateResponse) {
                loseUpdateResponse = false
                return NetworkError.Timeout(IllegalStateException()).left()
            }
            return product.right()
        }

        override suspend fun createCollection(
            request: CreateCollectionRequest,
            idempotencyKey: String,
        ): Either<NetworkError, Collection> {
            collectionCreates += request to idempotencyKey
            if (loseCollectionResponse) {
                loseCollectionResponse = false
                return NetworkError.Timeout(IllegalStateException()).left()
            }
            return Collection("baratie", request.name, "baratie", TIME, TIME).right()
        }

        override suspend fun uploadPhoto(productId: String, uri: Uri): Either<NetworkError, Product> {
            error("Unexpected photo upload")
        }
    }

    private companion object {
        const val PRODUCT_ID = "clima-tact"
        const val WATER_7 = "water7"
        val TIME: LocalDateTime = LocalDateTime.of(2026, 11, 17, 14, 16)
    }
}
