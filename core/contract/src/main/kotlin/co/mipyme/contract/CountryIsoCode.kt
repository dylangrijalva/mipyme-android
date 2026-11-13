package co.mipyme.contract

import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CountryIsoCode {
    @SerialName("zz")
    ZZ,

    @SerialName("ar")
    AR,

    @SerialName("bo")
    BO,

    @SerialName("br")
    BR,

    @SerialName("bz")
    BZ,

    @SerialName("cl")
    CL,

    @SerialName("co")
    CO,

    @SerialName("cr")
    CR,

    @SerialName("cu")
    CU,

    @SerialName("do")
    DO,

    @SerialName("ec")
    EC,

    @SerialName("gt")
    GT,

    @SerialName("hn")
    HN,

    @SerialName("ht")
    HT,

    @SerialName("mx")
    MX,

    @SerialName("ni")
    NI,

    @SerialName("pa")
    PA,

    @SerialName("pe")
    PE,

    @SerialName("pr")
    PR,

    @SerialName("py")
    PY,

    @SerialName("sv")
    SV,

    @SerialName("uy")
    UY,

    @SerialName("ve")
    VE,
}

fun CountryIsoCode.displayName(locale: Locale): String {
    return Locale.Builder().setRegion(name).build().getDisplayCountry(locale)
}
