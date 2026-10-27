package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class MipymeError(
    val code: MipymeErrorCode,
    val message: String,
    val details: List<ErrorDetail>,
    val requestId: String,
    val status: Int,
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val field: String? = null,
)
