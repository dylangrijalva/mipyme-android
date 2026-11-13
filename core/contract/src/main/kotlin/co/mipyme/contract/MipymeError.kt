package co.mipyme.contract

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MipymeError(
    val code: MipymeErrorCode,
    val message: String,
    val details: List<ErrorDetail>,
    // The authentication failure response spells this `requestId`, unlike the documented `request_id`.
    @JsonNames("requestId")
    val requestId: String,
    val status: Int,
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val field: String? = null,
)
