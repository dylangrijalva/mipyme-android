package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.Location
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateLocationRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateLocationRequest
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

class LocationsApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreateLocationRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Location> {
        return client.runSafe {
            post("/v1/locations") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PaginationRequest = PaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Location>> {
        return client.runSafe {
            get("/v1/locations") {
                pagination(request.limit, request.cursor)
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Location> {
        return client.runSafe {
            get("/v1/locations/${id.encodeURLPathPart()}")
        }
    }

    suspend fun update(id: String, request: UpdateLocationRequest): Either<NetworkError, Location> {
        return client.runSafe {
            patch("/v1/locations/${id.encodeURLPathPart()}") {
                jsonBody(request)
            }
        }
    }
}
