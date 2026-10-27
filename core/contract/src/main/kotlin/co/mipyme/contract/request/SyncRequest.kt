package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val syncToken: String,
)
