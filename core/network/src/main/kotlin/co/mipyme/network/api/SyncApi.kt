package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.SyncResponse
import co.mipyme.contract.request.SyncRequest
import co.mipyme.network.NetworkError
import co.mipyme.network.internal.jsonBody
import co.mipyme.network.internal.runSafe
import io.ktor.client.HttpClient
import io.ktor.client.request.post

class SyncApi internal constructor(private val client: HttpClient) {
    suspend fun readResources(request: SyncRequest): Either<NetworkError, SyncResponse> {
        return client.runSafe {
            post("/v1/sync") {
                jsonBody(request)
            }
        }
    }
}
