package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.JsonWebTokenPayload
import co.mipyme.contract.Merchant
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.network.NetworkError
import co.mipyme.network.internal.idempotencyKey
import co.mipyme.network.internal.jsonBody
import co.mipyme.network.internal.runSafe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post

class AccountsApi internal constructor(private val client: HttpClient) {
    suspend fun currentUser(): Either<NetworkError, JsonWebTokenPayload> {
        return client.runSafe {
            get("/me")
        }
    }

    suspend fun onboard(
        request: OnboardAccountRequest,
        idempotencyKey: String? = null,
    ): Either<NetworkError, Merchant> {
        return client.runSafe {
            post("/v1/onboarding") {
                jsonBody(request)
                idempotencyKey(idempotencyKey)
            }
        }
    }
}
