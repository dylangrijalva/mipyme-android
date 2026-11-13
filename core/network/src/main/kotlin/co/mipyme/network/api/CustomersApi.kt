package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.Customer
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateCustomerRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateCustomerRequest
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

class CustomersApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreateCustomerRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Customer> {
        return client.runSafe {
            post("/v1/customers") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PaginationRequest = PaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Customer>> {
        return client.runSafe {
            get("/v1/customers") {
                pagination(request.limit, request.cursor)
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Customer> {
        return client.runSafe {
            get("/v1/customers/${id.encodeURLPathPart()}")
        }
    }

    suspend fun update(id: String, request: UpdateCustomerRequest): Either<NetworkError, Customer> {
        return client.runSafe {
            patch("/v1/customers/${id.encodeURLPathPart()}") {
                jsonBody(request)
            }
        }
    }
}
