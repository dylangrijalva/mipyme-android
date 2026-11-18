package co.mipyme.network.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.Product
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateProductRequest
import co.mipyme.network.NetworkError
import co.mipyme.network.internal.idempotencyKey
import co.mipyme.network.internal.jsonBody
import co.mipyme.network.internal.pagination
import co.mipyme.network.internal.runSafe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.encodeURLPathPart

class ProductsApi internal constructor(
    private val client: HttpClient,
    private val uploadClient: () -> HttpClient,
) {
    suspend fun create(
        request: CreateProductRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Product> {
        return client.runSafe {
            post("/v1/products") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PaginationRequest = PaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Product>> {
        return client.runSafe {
            get("/v1/products") {
                pagination(request.limit, request.cursor)
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Product> {
        return client.runSafe {
            get("/v1/products/${id.encodeURLPathPart()}")
        }
    }

    suspend fun update(id: String, request: UpdateProductRequest): Either<NetworkError, Product> {
        return client.runSafe {
            patch("/v1/products/${id.encodeURLPathPart()}") {
                jsonBody(request)
            }
        }
    }

    suspend fun storeImage(id: String, webp: ByteArray): Either<NetworkError, String> {
        if (webp.size !in 12..MAX_PRODUCT_PHOTO_BYTES) {
            return NetworkError.Unexpected(IllegalArgumentException("Invalid product photo size")).left()
        }
        return authorizeImageUpload(id, webp.size).flatMap { authorization ->
            uploadClient().putProductImage(authorization, webp).map { authorization.id }
        }
    }

    internal suspend fun authorizeImageUpload(
        id: String,
        size: Int,
    ): Either<NetworkError, ProductImageUploadAuthorization> {
        return client.runSafe<ProductImageUploadAuthorization> {
            post("/v1/products/${id.encodeURLPathPart()}/image-uploads") {
                jsonBody(AuthorizeProductImageUploadRequest(size))
            }
        }.mapLeft { error ->
            // An authorization response that fails to decode may contain the signed URL.
            if (error is NetworkError.Serialization) {
                NetworkError.Serialization(IllegalStateException("Invalid photo upload authorization response"))
            } else {
                error
            }
        }
    }

    suspend fun verifyImageUpload(id: String, imageId: String): Either<NetworkError, Product> {
        return client.runSafe {
            post("/v1/products/${id.encodeURLPathPart()}/image-uploads/${imageId.encodeURLPathPart()}/verify")
        }
    }
}
