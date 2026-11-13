package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.Sale
import co.mipyme.contract.request.CreateSaleRequest
import co.mipyme.contract.request.SalePaginationRequest
import co.mipyme.contract.request.VoidSaleRequest
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

class SalesApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreateSaleRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Sale> {
        return client.runSafe {
            post("/v1/sales") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: SalePaginationRequest = SalePaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Sale>> {
        return client.runSafe {
            get("/v1/sales") {
                pagination(request.limit, request.cursor)
                request.locationId?.let { parameter("location_id", it.value) }
                request.customerId?.let { parameter("customer_id", it.value) }
                request.status?.let { parameter("status", it.wireName()) }
                request.from?.let { parameter("from", it.toQueryValue()) }
                request.to?.let { parameter("to", it.toQueryValue()) }
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Sale> {
        return client.runSafe {
            get("/v1/sales/${id.encodeURLPathPart()}")
        }
    }

    suspend fun void(
        id: String,
        request: VoidSaleRequest = VoidSaleRequest(),
        idempotencyKey: String? = null,
    ): Either<NetworkError, Sale> {
        return client.runSafe {
            post("/v1/sales/${id.encodeURLPathPart()}/void") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }
}
