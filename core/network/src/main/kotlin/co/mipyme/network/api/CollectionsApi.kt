package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.Collection
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateCollectionRequest
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

class CollectionsApi internal constructor(private val client: HttpClient) {
    suspend fun create(
        request: CreateCollectionRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Collection> {
        return client.runSafe {
            post("/v1/collections") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }

    suspend fun list(
        request: PaginationRequest = PaginationRequest(),
    ): Either<NetworkError, PaginationResponse<Collection>> {
        return client.runSafe {
            get("/v1/collections") {
                pagination(request.limit, request.cursor)
            }
        }
    }

    suspend fun find(id: String): Either<NetworkError, Collection> {
        return client.runSafe {
            get("/v1/collections/${id.encodeURLPathPart()}")
        }
    }

    suspend fun update(id: String, request: UpdateCollectionRequest): Either<NetworkError, Collection> {
        return client.runSafe {
            patch("/v1/collections/${id.encodeURLPathPart()}") {
                jsonBody(request)
            }
        }
    }
}
