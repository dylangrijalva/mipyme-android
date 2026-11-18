package co.mipyme.screen.product_editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.contract.Collection
import co.mipyme.contract.Decimal
import co.mipyme.contract.OptionalField
import co.mipyme.contract.Product
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.UpdateProductRequest
import co.mipyme.data.product_editor.ProductEditorContext
import co.mipyme.data.product_editor.ProductEditorRepository
import co.mipyme.data.product_editor.ProductPhotoUploadCheckpoint
import co.mipyme.network.NetworkError
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductEditorRecoveryTest {
    @Test
    fun recreatingAfterLostPhotoVerificationRetainsTheProductAndCanExplicitlyClearThePhoto() = runBlocking {
        val catalog = Catalog()
        val store = ViewModelStore()
        val handle = SavedStateHandle()
        val uri = Uri.parse("content://co.mipyme.dev.product-photo/product-photos/log-pose.webp")
        try {
            val before = withContext(Dispatchers.Main) {
                ProductEditorViewModel(catalog.session(), ProductType.SERVICE, savedState = handle)
                    .also { store.put("editor", it) }
            }
            withTimeout(5_000) { before.stateFlow.first { it.canEdit } }
            withContext(Dispatchers.Main) {
                before.setName("Navigation lesson")
                before.setPrice("12.5")
                assertTrue(before.setPhoto(uri))
                before.save()
            }
            withTimeout(5_000) { before.stateFlow.first { it.error == ProductEditorError.PHOTO_SAVE } }
            assertEquals(ProductImage("https://example.com/log-pose.webp"), catalog.product?.imageUrl)

            val after = withContext(Dispatchers.Main) {
                val snapshot = handle.keys().associateWith { key -> handle.get<Any?>(key) }
                store.clear()
                ProductEditorViewModel(catalog.session(), ProductType.SERVICE, savedState = SavedStateHandle(snapshot))
                    .also { store.put("editor", it) }
            }
            withTimeout(5_000) { after.stateFlow.first { it.canEdit } }
            assertEquals(uri, after.state.photoUri)
            assertEquals("uploaded-image", catalog.restoredPhoto?.imageId)
            assertEquals(ProductEditorError.PHOTO_SAVE, after.state.error)

            withContext(Dispatchers.Main) {
                after.removePhoto()
                after.save()
            }
            withTimeout(5_000) { after.stateFlow.first { it.savedProductId == PRODUCT_ID } }
            assertEquals(1, catalog.creates)
            assertEquals(listOf(UpdateProductRequest(imageUrl = OptionalField.Present(null))), catalog.updates)
            assertNull(catalog.product?.imageUrl)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    private class Catalog {
        var product: Product? = null
        var restoredPhoto: ProductPhotoUploadCheckpoint? = null
        var creates = 0
        val updates = mutableListOf<UpdateProductRequest>()

        fun session(): ProductEditorRepository {
            return object : ProductEditorRepository {
                private val checkpoint = MutableStateFlow<ProductPhotoUploadCheckpoint?>(null)

                override fun observeContext(): Flow<ProductEditorContext> {
                    return flowOf(ProductEditorContext(canManageCatalog = true, isOnline = true))
                }

                override suspend fun findProduct(id: String): Either<NetworkError, Product> {
                    return requireNotNull(product).right()
                }

                override suspend fun createProduct(
                    request: CreateProductRequest,
                    idempotencyKey: String,
                ): Either<NetworkError, Product> {
                    creates += 1
                    val time = LocalDateTime.of(2026, 11, 17, 15, 0)
                    return Product(
                        id = PRODUCT_ID,
                        name = request.name,
                        price = Decimal(request.price.value),
                        slug = "navigation-lesson",
                        unitCost = Decimal("0"),
                        productType = ProductType.SERVICE,
                        status = ProductStatus.ACTIVE,
                        imageUrl = null,
                        collection = null,
                        optionGroups = emptyList(),
                        variants = emptyList(),
                        createdAt = time,
                        updatedAt = time,
                        archivedAt = null,
                    ).also { product = it }.right()
                }

                override suspend fun updateProduct(id: String, request: UpdateProductRequest): Either<NetworkError, Product> {
                    assertEquals(PRODUCT_ID, id)
                    updates += request
                    return requireNotNull(product).copy(imageUrl = null).also { product = it }.right()
                }

                override suspend fun createCollection(
                    request: CreateCollectionRequest,
                    idempotencyKey: String,
                ): Either<NetworkError, Collection> {
                    error("Unexpected collection creation")
                }

                override suspend fun uploadPhoto(productId: String, uri: Uri): Either<NetworkError, Product> {
                    checkpoint.value = ProductPhotoUploadCheckpoint(
                        productId,
                        uri.toString(),
                        "nami",
                        "strawhatpirates",
                        "uploaded-image",
                    )
                    product = requireNotNull(product).copy(imageUrl = ProductImage("https://example.com/log-pose.webp"))
                    return NetworkError.Timeout(IllegalStateException()).left()
                }

                override fun observePhotoUploadCheckpoint(): Flow<ProductPhotoUploadCheckpoint?> {
                    return checkpoint
                }

                override fun restorePhotoUploadCheckpoint(checkpoint: ProductPhotoUploadCheckpoint) {
                    restoredPhoto = checkpoint
                    this.checkpoint.value = checkpoint
                }

                override fun discardPhotoUpload() {
                    checkpoint.value = null
                }
            }
        }
    }

    private companion object {
        const val PRODUCT_ID = "navigation-lesson"
    }
}
