package co.mipyme.network.api

import arrow.core.Either
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

class ProductsApi internal constructor(private val client: HttpClient) {
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
}
