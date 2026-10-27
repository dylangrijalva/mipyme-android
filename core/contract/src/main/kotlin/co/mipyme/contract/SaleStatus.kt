package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SaleStatus {
    @SerialName("fulfilled")
    FULFILLED,

    @SerialName("pending")
    PENDING,

    @SerialName("voided")
    VOIDED,
}
