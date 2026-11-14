package co.mipyme.sync.internal

import co.mipyme.contract.request.AuthorizeRealtimeChannelRequest
import co.mipyme.network.MipymeApi
import com.pusher.client.AuthorizationFailureException
import com.pusher.client.Authorizer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

internal class ChannelAuthorizer(private val api: MipymeApi) : Authorizer {
    override fun authorize(channelName: String, socketId: String): String {
        val result = runBlocking {
            withTimeoutOrNull(AUTHORIZATION_TIMEOUT) {
                api.realtime.authorizeChannel(AuthorizeRealtimeChannelRequest(socketId, channelName))
            }
        } ?: throw AuthorizationFailureException("Channel authorization timed out")
        return result.fold(
            ifLeft = { error -> throw AuthorizationFailureException("Channel authorization failed: $error") },
            ifRight = { authorization -> buildJsonObject { put("auth", authorization.auth) }.toString() },
        )
    }

    private companion object {
        private val AUTHORIZATION_TIMEOUT = 10.seconds
    }
}
