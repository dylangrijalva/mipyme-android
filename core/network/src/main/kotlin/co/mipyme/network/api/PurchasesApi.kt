package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.Purchase
import co.mipyme.contract.request.CreatePurchaseRequest
import co.mipyme.contract.request.PurchasePaginationRequest
import co.mipyme.contract.request.VoidPurchaseRequest
import co.mipyme.network.NetworkError
import co.mipyme.network.internal.idempotencyKey
import co.mipyme.network.internal.jsonBody
import co.mipyme.network.internal.pagination
import co.mipyme.network.internal.runSafe
import co.mipyme.network.internal.toQueryValue
import co.mipyme.network.internal.wireName
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.encodeURLPathPart

class PurchasesApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreatePurchaseRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Purchase> {
        return client.runSafe {
            post("/v1/purchases") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PurchasePaginationRequest = PurchasePaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Purchase>> {
        return client.runSafe {
            get("/v1/purchases") {
                pagination(request.limit, request.cursor)
                request.locationId?.let { parameter("location_id", it.value) }
                request.supplierId?.let { parameter("supplier_id", it.value) }
                request.status?.let { parameter("status", it.wireName()) }
                request.from?.let { parameter("from", it.toQueryValue()) }
                request.to?.let { parameter("to", it.toQueryValue()) }
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Purchase> {
        return client.runSafe {
            get("/v1/purchases/${id.encodeURLPathPart()}")
        }
    }

    suspend fun void(
        id: String,
        request: VoidPurchaseRequest = VoidPurchaseRequest(),
        idempotencyKey: String? = null,
    ): Either<NetworkError, Purchase> {
        return client.runSafe {
            post("/v1/purchases/${id.encodeURLPathPart()}/void") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }
}
