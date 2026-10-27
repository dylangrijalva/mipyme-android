package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class RealtimeChannelAuthorization(
    val auth: String,
)
