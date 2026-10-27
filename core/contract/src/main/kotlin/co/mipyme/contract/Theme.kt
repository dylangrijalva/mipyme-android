package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Theme {
    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK,

    @SerialName("follow_system")
    FOLLOW_SYSTEM
}
