package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizeRealtimeChannelRequest(
    val socketId: String,
    val channelName: String,
)
