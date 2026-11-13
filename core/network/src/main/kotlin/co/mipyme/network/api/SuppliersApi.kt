package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.Supplier
import co.mipyme.contract.request.CreateSupplierRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateSupplierRequest
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

class SuppliersApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreateSupplierRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Supplier> {
        return client.runSafe {
            post("/v1/suppliers") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PaginationRequest = PaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Supplier>> {
        return client.runSafe {
            get("/v1/suppliers") {
                pagination(request.limit, request.cursor)
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Supplier> {
        return client.runSafe {
            get("/v1/suppliers/${id.encodeURLPathPart()}")
        }
    }

    suspend fun update(id: String, request: UpdateSupplierRequest): Either<NetworkError, Supplier> {
        return client.runSafe {
            patch("/v1/suppliers/${id.encodeURLPathPart()}") {
                jsonBody(request)
            }
        }
    }
}
