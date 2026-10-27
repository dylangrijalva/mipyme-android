package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class BusinessError(
    val code: MipymeErrorCode,
    val message: String,
    val details: List<ErrorDetail>,
)
