package co.mipyme.contract

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Decimal(val value: String)

@Serializable
@JvmInline
value class PositiveDecimal(val value: String)

infix operator fun Decimal.plus(other: Decimal): Decimal {
    return Decimal(BigDecimal(value).add(BigDecimal(other.value)).toPlainString())
}

infix operator fun Decimal.minus(other: Decimal): Decimal {
    return Decimal(BigDecimal(value).subtract(BigDecimal(other.value)).toPlainString())
}

infix fun Decimal.multiply(other: Decimal): Decimal {
    return Decimal(BigDecimal(value).multiply(BigDecimal(other.value)).toPlainString())
}

operator fun Decimal.times(other: Decimal): Decimal {
    return this multiply other
}

infix fun Decimal.divide(other: Decimal): Decimal {
    return Decimal(BigDecimal(value).divide(BigDecimal(other.value), 2, RoundingMode.HALF_UP).toPlainString())
}

operator fun Decimal.div(other: Decimal): Decimal {
    return this divide other
}
