package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PurchaseStatus {
    @SerialName("fulfilled")
    FULFILLED,

    @SerialName("voided")
    VOIDED,
}
