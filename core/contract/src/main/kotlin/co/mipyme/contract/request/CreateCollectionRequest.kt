package co.mipyme.contract.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCollectionRequest(
    val name: String,
)
