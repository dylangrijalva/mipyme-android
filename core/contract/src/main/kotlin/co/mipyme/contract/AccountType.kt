package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    @SerialName("owner")
    OWNER,

    @SerialName("staff")
    STAFF,

    @SerialName("internal")
    INTERNAL,
}
