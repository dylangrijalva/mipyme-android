package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CurrencyCode {
    @SerialName("xxx")
    XXX,

    @SerialName("ars")
    ARS,

    @SerialName("bob")
    BOB,

    @SerialName("brl")
    BRL,

    @SerialName("bzd")
    BZD,

    @SerialName("clp")
    CLP,

    @SerialName("cop")
    COP,

    @SerialName("crc")
    CRC,

    @SerialName("cup")
    CUP,

    @SerialName("dop")
    DOP,

    @SerialName("usd")
    USD,

    @SerialName("gtq")
    GTQ,

    @SerialName("hnl")
    HNL,

    @SerialName("htg")
    HTG,

    @SerialName("mxn")
    MXN,

    @SerialName("nio")
    NIO,

    @SerialName("pab")
    PAB,

    @SerialName("pen")
    PEN,

    @SerialName("pyg")
    PYG,

    @SerialName("uyu")
    UYU,

    @SerialName("ves")
    VES,
}
