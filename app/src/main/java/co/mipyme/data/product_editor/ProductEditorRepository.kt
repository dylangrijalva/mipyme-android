package co.mipyme.data.product_editor

import android.content.Context
import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.contract.Collection
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.Location
import co.mipyme.contract.MipymeError
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.PosPermission
import co.mipyme.contract.Product
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.UpdateProductRequest
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.database.CatalogEditorCache
import co.mipyme.database.MipymeCache
import co.mipyme.network.MipymeApi
import co.mipyme.network.NetworkError
import co.mipyme.sync.NetworkConnectivity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber

data class ProductEditorContext(
    val canManageCatalog: Boolean = false,
    val isOnline: Boolean = false,
    val locations: List<Location> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val country: CountryIsoCode? = null,
)

interface ProductEditorRepository {
    fun observeContext(): Flow<ProductEditorContext>

    suspend fun findProduct(id: String): Either<NetworkError, Product>

    suspend fun createProduct(request: CreateProductRequest, idempotencyKey: String): Either<NetworkError, Product>

    suspend fun updateProduct(id: String, request: UpdateProductRequest): Either<NetworkError, Product>

    suspend fun createCollection(
        request: CreateCollectionRequest,
        idempotencyKey: String,
    ): Either<NetworkError, Collection>

    suspend fun uploadPhoto(productId: String, uri: Uri): Either<NetworkError, Product>

    fun observePhotoUploadCheckpoint(): Flow<ProductPhotoUploadCheckpoint?> {
        return flowOf(null)
    }

    fun restorePhotoUploadCheckpoint(checkpoint: ProductPhotoUploadCheckpoint) {}

    fun discardPhotoUpload() {}
}

class DefaultProductEditorRepository(
    private val cache: MipymeCache,
    private val editorCache: CatalogEditorCache,
    private val api: MipymeApi,
    private val employees: CurrentEmployeeRepository,
    private val locations: LocationSelectionRepository,
    private val connectivity: NetworkConnectivity,
    private val authService: AuthService,
    context: Context,
) : ProductEditorRepository {
    private val photoEncoder = ProductPhotoEncoder(context.contentResolver)
    private val photoUpload = ProductPhotoUpload(api.products)

    override fun observeContext(): Flow<ProductEditorContext> {
        return combine(
            employees.currentEmployeeFlow,
            connectivity.observeOnline(),
            locations.accessibleLocationsFlow,
            cache.observeCollections(),
            cache.observeMerchant(),
        ) { employee, isOnline, locations, collections, merchant ->
            ProductEditorContext(
                canManageCatalog = employee.canManageCatalog(),
                isOnline = isOnline,
                locations = locations,
                collections = collections,
                country = merchant?.country,
            )
        }.distinctUntilChanged()
    }

    override suspend fun findProduct(id: String): Either<NetworkError, Product> {
        val session = session() ?: return permissionDenied().left()
        val result = if (connectivity.isOnline) api.products.find(id) else offline().left()
        if (!session.isCurrent()) return permissionDenied().left()
        return when (result) {
            is Either.Right -> {
                storeVerified { editorCache.storeProduct(result.value, session.merchantId) }
                result
            }

            is Either.Left -> {
                when (result.value) {
                    is NetworkError.NetworkUnavailable, is NetworkError.Timeout -> {
                        val cached = Either.catch { editorCache.findProduct(id, session.merchantId) }.getOrNull()
                        if (!session.isCurrent()) return permissionDenied().left()
                        cached?.right() ?: result
                    }

                    else -> result
                }
            }
        }
    }

    override suspend fun createProduct(
        request: CreateProductRequest,
        idempotencyKey: String,
    ): Either<NetworkError, Product> {
        return write(editorCache::storeProduct) { api.products.create(request, idempotencyKey) }
    }

    override suspend fun updateProduct(id: String, request: UpdateProductRequest): Either<NetworkError, Product> {
        return write(editorCache::storeProduct) { api.products.update(id, request) }
    }

    override suspend fun createCollection(
        request: CreateCollectionRequest,
        idempotencyKey: String,
    ): Either<NetworkError, Collection> {
        return write(editorCache::storeCollection) { api.collections.create(request, idempotencyKey) }
    }

    override suspend fun uploadPhoto(productId: String, uri: Uri): Either<NetworkError, Product> {
        return write(editorCache::storeProduct) { session ->
            val selection = ProductPhotoSelection(productId, uri.toString(), session.accountId, session.merchantId)
            photoUpload.finish(
                selection = selection,
                checkAccess = {
                    when {
                        !session.canManageCatalog() -> permissionDenied()
                        !connectivity.isOnline -> offline()
                        else -> null
                    }
                },
            ) {
                photoEncoder.encode(uri).fold(
                    onSuccess = { it.right() },
                    onFailure = { NetworkError.Unexpected(IOException("Could not prepare the product photo")).left() },
                )
            }
        }
    }

    override fun discardPhotoUpload() {
        photoUpload.discard()
    }

    override fun observePhotoUploadCheckpoint(): Flow<ProductPhotoUploadCheckpoint?> {
        return photoUpload.observeCheckpoint()
    }

    override fun restorePhotoUploadCheckpoint(checkpoint: ProductPhotoUploadCheckpoint) {
        val session = CatalogSession(checkpoint.accountId, checkpoint.merchantId)
        if (authService.authStateFlow.value == AuthState.Initializing || session.isCurrent()) {
            photoUpload.restore(checkpoint)
        }
    }

    private suspend fun <T> write(
        store: suspend (T, String) -> Unit,
        send: suspend (CatalogSession) -> Either<NetworkError, T>,
    ): Either<NetworkError, T> {
        val session = session() ?: return permissionDenied().left()
        if (!connectivity.isOnline) return offline().left()
        val result = send(session)
        if (!session.isCurrent()) return permissionDenied().left()
        result.onRight { value -> storeVerified { store(value, session.merchantId) } }
        return result
    }

    private suspend fun session(): CatalogSession? {
        val account = (authService.authStateFlow.value as? AuthState.Authenticated)?.account ?: return null
        val merchantId = account.publicMetadata.merchantId ?: return null
        val session = CatalogSession(account.id, merchantId)
        return session.takeIf { it.canManageCatalog() }
    }

    private suspend fun CatalogSession.canManageCatalog(): Boolean {
        return isCurrent() && employees.currentEmployeeFlow.first().canManageCatalog() && isCurrent()
    }

    private fun CatalogSession.isCurrent(): Boolean {
        val current = (authService.authStateFlow.value as? AuthState.Authenticated)?.account
        return current?.id == accountId && current.publicMetadata.merchantId == merchantId
    }

    private suspend fun storeVerified(store: suspend () -> Unit) {
        Either.catch { store() }.onLeft { error ->
            Timber.w(error, "Could not cache the confirmed catalog response")
        }
    }

    private data class CatalogSession(val accountId: String, val merchantId: String)
}

private fun Employee?.canManageCatalog(): Boolean {
    return this != null &&
        (isOwner || (status == EmployeeStatus.ENABLED && PosPermission.MANAGE_CATALOG in permissions))
}

private fun permissionDenied(): NetworkError {
    return NetworkError.Response(
        MipymeError(
            code = MipymeErrorCode.FORBIDDEN_ERROR,
            message = "Catalog permission required",
            details = emptyList(),
            requestId = "",
            status = 403,
        ),
    )
}

private fun offline(): NetworkError {
    return NetworkError.NetworkUnavailable(IOException("No validated network for catalog access"))
}
