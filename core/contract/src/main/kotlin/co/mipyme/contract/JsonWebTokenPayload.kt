package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class JsonWebTokenPayload(
    val sub: String,
    val email: String,
)
