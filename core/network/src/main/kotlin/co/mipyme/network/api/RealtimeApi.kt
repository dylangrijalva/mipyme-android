package co.mipyme.network.api

import arrow.core.Either
import co.mipyme.contract.RealtimeChannelAuthorization
import co.mipyme.contract.request.AuthorizeRealtimeChannelRequest
import co.mipyme.network.NetworkError
import co.mipyme.network.internal.runSafe
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters

class RealtimeApi internal constructor(private val client: HttpClient) {
    suspend fun authorizeChannel(
        request: AuthorizeRealtimeChannelRequest,
    ): Either<NetworkError, RealtimeChannelAuthorization> {
        return client.runSafe {
            submitForm(
                url = "/v1/realtime/subscriptions",
                formParameters = Parameters.build {
                    append("socket_id", request.socketId)
                    append("channel_name", request.channelName)
                },
            )
        }
    }
}
