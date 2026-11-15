package co.mipyme.sync.internal

import co.mipyme.network.MipymeApi
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

internal class PusherRealtimeClient(
    private val key: String,
    private val cluster: String,
    private val api: MipymeApi,
) : RealtimeClient {
    override fun changesFlow(merchantId: String): Flow<String> {
        return callbackFlow {
            val closed = AtomicBoolean(false)
            val options = PusherOptions().setCluster(cluster)
            options.authorizer = ChannelAuthorizer(api)
            val pusher = Pusher(key, options)
            val channelName = "private-$merchantId"

            pusher.connect(
                object : ConnectionEventListener {
                    override fun onConnectionStateChange(change: ConnectionStateChange?) {
                        Timber.d("Realtime connection moved to %s", change?.currentState)
                        if (closed.get() && change?.currentState == ConnectionState.CONNECTED) {
                            pusher.disconnect()
                        }
                    }

                    override fun onError(message: String?, code: String?, exception: Exception?) {
                        Timber.w(exception, "Realtime connection error: %s", message)
                    }
                },
                ConnectionState.ALL,
            )
            pusher.subscribePrivate(
                channelName,
                object : PrivateChannelEventListener {
                    override fun onAuthenticationFailure(message: String?, exception: Exception?) {
                        Timber.w(exception, "Realtime channel authorization failed: %s", message)
                    }

                    override fun onSubscriptionSucceeded(channelName: String?) {
                        Timber.d("Subscribed to %s", channelName)
                    }

                    override fun onEvent(event: PusherEvent?) {
                        if (closed.get()) return
                        trySend(event?.data.resourceType())
                    }
                },
                "mipyme.changes",
            )

            awaitClose {
                closed.set(true)
                runCatching { pusher.unsubscribe(channelName) }
                pusher.disconnect()
            }
        }
    }

    private fun String?.resourceType(): String {
        if (this == null) return UNKNOWN_TYPE
        return runCatching {
            Json.parseToJsonElement(this).jsonObject["type"]?.jsonPrimitive?.content
        }.getOrNull() ?: UNKNOWN_TYPE
    }

    private companion object {
        private const val UNKNOWN_TYPE = "unknown"
    }
}
