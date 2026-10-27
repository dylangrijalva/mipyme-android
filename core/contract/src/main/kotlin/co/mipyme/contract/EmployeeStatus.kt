package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EmployeeStatus {
    @SerialName("invited")
    INVITED,

    @SerialName("enabled")
    ENABLED,

    @SerialName("disabled")
    DISABLED,
}
