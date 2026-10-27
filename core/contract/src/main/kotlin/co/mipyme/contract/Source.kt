package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Source {
    @SerialName("pos.web")
    POS_WEB,

    @SerialName("pos.android")
    POS_ANDROID,

    @SerialName("pos.ios")
    POS_IOS,
}
