package co.mipyme.data.product_editor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.Product
import co.mipyme.network.NetworkError
import co.mipyme.network.api.ProductsApi
import java.io.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ProductPhotoUploadCheckpoint(
    val productId: String,
    val uri: String,
    val accountId: String,
    val merchantId: String,
    val imageId: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class ProductPhotoSelection(
    val productId: String,
    val uri: String,
    val accountId: String,
    val merchantId: String,
)

internal class ProductPhotoUpload(private val products: ProductsApi) {
    private val lock = Mutex()
    private val pending = MutableStateFlow<PendingUpload?>(null)

    fun observeCheckpoint(): Flow<ProductPhotoUploadCheckpoint?> {
        return pending.map { it?.checkpoint() }.distinctUntilChanged()
    }

    fun restore(checkpoint: ProductPhotoUploadCheckpoint) {
        val selection = ProductPhotoSelection(checkpoint.productId, checkpoint.uri, checkpoint.accountId, checkpoint.merchantId)
        if (checkpoint.imageId.isNotBlank()) {
            pending.compareAndSet(null, PendingUpload(selection, checkpoint.imageId))
        }
    }

    fun discard() {
        pending.value = null
    }

    suspend fun finish(
        selection: ProductPhotoSelection,
        checkAccess: suspend () -> NetworkError?,
        prepare: suspend () -> Either<NetworkError, ByteArray>,
    ): Either<NetworkError, Product> {
        return lock.withLock {
            var upload = pending.value?.takeIf { it.selection == selection }
                ?: PendingUpload(selection).also { pending.value = it }

            val uploadedId = upload.imageId
            if (uploadedId != null) {
                val result = verify(upload, uploadedId, checkAccess)
                if (result.leftOrNull()?.requiresNewUpload() != true) return@withLock result
                val replacement = PendingUpload(selection)
                if (!pending.compareAndSet(upload, replacement)) return@withLock selectionChanged().left()
                upload = replacement
            }

            attemptError(upload, checkAccess)?.let { return@withLock it.left() }
            prepare().flatMap { bytes ->
                val error = attemptError(upload, checkAccess)
                if (error != null) error.left() else products.storeImage(selection.productId, bytes)
            }.flatMap { imageId ->
                // A successful PUT only needs verification on the next save attempt.
                val stored = PendingUpload(selection, imageId)
                if (!pending.compareAndSet(upload, stored)) return@flatMap selectionChanged().left()
                verify(stored, imageId, checkAccess)
            }
        }
    }

    private suspend fun verify(
        upload: PendingUpload,
        imageId: String,
        checkAccess: suspend () -> NetworkError?,
    ): Either<NetworkError, Product> {
        attemptError(upload, checkAccess)?.let { return it.left() }
        val result = products.verifyImageUpload(upload.selection.productId, imageId)
        if (result.isRight()) pending.compareAndSet(upload, null)
        return result
    }

    private suspend fun attemptError(
        upload: PendingUpload,
        checkAccess: suspend () -> NetworkError?,
    ): NetworkError? {
        val error = checkAccess()
        return if (pending.value == upload) error else selectionChanged()
    }

    private fun selectionChanged(): NetworkError {
        return NetworkError.Unexpected(IllegalStateException("Photo selection changed"))
    }

    private class PendingUpload(val selection: ProductPhotoSelection, val imageId: String? = null) {
        fun checkpoint(): ProductPhotoUploadCheckpoint? {
            val id = imageId ?: return null
            return ProductPhotoUploadCheckpoint(
                selection.productId,
                selection.uri,
                selection.accountId,
                selection.merchantId,
                id,
            )
        }
    }
}

private fun NetworkError.requiresNewUpload(): Boolean {
    return this is NetworkError.Response &&
        (error.code == MipymeErrorCode.RESOURCE_NOT_FOUND || error.code == MipymeErrorCode.CONFLICT_ERROR)
}
