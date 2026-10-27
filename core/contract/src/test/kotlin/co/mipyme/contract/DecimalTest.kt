package co.mipyme.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DecimalTest {
    @Test
    fun addsDecimalsUsingOperatorSyntax() {
        assertEquals(Decimal("12.50"), Decimal("10.25") + Decimal("2.25"))
    }

    @Test
    fun subtractsDecimalsUsingOperatorSyntax() {
        assertEquals(Decimal("8.00"), Decimal("10.25") - Decimal("2.25"))
    }

    @Test
    fun multipliesDecimalsUsingOperatorSyntax() {
        assertEquals(Decimal("2.50"), Decimal("1.25") * Decimal("2"))
    }

    @Test
    fun dividesDecimalsUsingOperatorSyntax() {
        assertEquals(Decimal("0.13"), Decimal("1") / Decimal("8"))
    }

    @Test
    fun addsDecimalsUsingInfixSyntax() {
        assertEquals(Decimal("12.50"), Decimal("10.25") plus Decimal("2.25"))
    }

    @Test
    fun subtractsDecimalsUsingInfixSyntax() {
        assertEquals(Decimal("8.00"), Decimal("10.25") minus Decimal("2.25"))
    }

    @Test
    fun multipliesDecimalsUsingInfixSyntax() {
        assertEquals(Decimal("2.50"), Decimal("1.25") multiply Decimal("2"))
    }

    @Test
    fun dividesUsingTwoDecimalPlacesAndHalfUpRounding() {
        assertEquals(Decimal("0.13"), Decimal("1") divide Decimal("8"))
    }

    @Test
    fun addsWithoutFloatingPointPrecisionLoss() {
        assertEquals(Decimal("0.3"), Decimal("0.1") + Decimal("0.2"))
        assertEquals(Decimal("9007199254740993.01"), Decimal("9007199254740993") + Decimal("0.01"))
    }

    @Test
    fun subtractsWithoutFloatingPointPrecisionLoss() {
        assertEquals(Decimal("0.1"), Decimal("0.3") - Decimal("0.2"))
        assertEquals(Decimal("0.01"), Decimal("9007199254740993.01") - Decimal("9007199254740993"))
    }

    @Test
    fun additionAndSubtractionPreserveTheLargerOperandScale() {
        assertEquals(Decimal("3.2300"), Decimal("1.2300") + Decimal("2"))
        assertEquals(Decimal("3.2300"), Decimal("2") + Decimal("1.2300"))
        assertEquals(Decimal("-0.7700"), Decimal("1.2300") - Decimal("2"))
        assertEquals(Decimal("0.7700"), Decimal("2") - Decimal("1.2300"))
    }

    @Test
    fun addsAndSubtractsNegativeValues() {
        assertEquals(Decimal("-8.00"), Decimal("-10.25") + Decimal("2.25"))
        assertEquals(Decimal("-12.50"), Decimal("-10.25") + Decimal("-2.25"))
        assertEquals(Decimal("12.50"), Decimal("10.25") - Decimal("-2.25"))
        assertEquals(Decimal("-12.50"), Decimal("-10.25") - Decimal("2.25"))
    }

    @Test
    fun addingAndSubtractingZeroPreserveTheValue() {
        assertEquals(Decimal("1.25"), Decimal("1.25") + Decimal("0"))
        assertEquals(Decimal("1.25"), Decimal("0") + Decimal("1.25"))
        assertEquals(Decimal("1.25"), Decimal("1.25") - Decimal("0"))
        assertEquals(Decimal("-1.25"), Decimal("0") - Decimal("1.25"))
    }

    @Test
    fun cancellationPreservesScale() {
        assertEquals(Decimal("0.00"), Decimal("1.25") + Decimal("-1.25"))
        assertEquals(Decimal("0.00"), Decimal("1.25") - Decimal("1.25"))
    }

    @Test
    fun multiplicationPreservesPrecisionAndCombinedScale() {
        assertEquals(Decimal("0.02"), Decimal("0.1") * Decimal("0.2"))
        assertEquals(Decimal("3.0750"), Decimal("1.230") * Decimal("2.5"))
        assertEquals(Decimal("18014398509481986"), Decimal("9007199254740993") * Decimal("2"))
    }

    @Test
    fun multiplicationHandlesNegativeOperands() {
        assertEquals(Decimal("-2.50"), Decimal("-1.25") * Decimal("2"))
        assertEquals(Decimal("-2.50"), Decimal("1.25") * Decimal("-2"))
        assertEquals(Decimal("2.50"), Decimal("-1.25") * Decimal("-2"))
    }

    @Test
    fun multiplyingByZeroPreservesCombinedScale() {
        assertEquals(Decimal("0.000"), Decimal("1.25") * Decimal("0.0"))
        assertEquals(Decimal("0.000"), Decimal("0.0") * Decimal("1.25"))
    }

    @Test
    fun divisionAlwaysReturnsTwoDecimalPlaces() {
        assertEquals(Decimal("2.00"), Decimal("4") / Decimal("2"))
        assertEquals(Decimal("0.50"), Decimal("1.0000") / Decimal("2.0"))
        assertEquals(Decimal("0.00"), Decimal("0") / Decimal("3"))
        assertEquals(Decimal("9007199254740993.00"), Decimal("18014398509481986") / Decimal("2"))
    }

    @Test
    fun divisionRoundsOnEitherSideOfHalfACent() {
        assertEquals(Decimal("1.23"), Decimal("1.2349") / Decimal("1"))
        assertEquals(Decimal("1.24"), Decimal("1.2351") / Decimal("1"))
        assertEquals(Decimal("-1.23"), Decimal("-1.2349") / Decimal("1"))
        assertEquals(Decimal("-1.24"), Decimal("-1.2351") / Decimal("1"))
    }

    @Test
    fun divisionRoundsTiesAwayFromZero() {
        assertEquals(Decimal("1.23"), Decimal("1.225") / Decimal("1"))
        assertEquals(Decimal("-1.23"), Decimal("-1.225") / Decimal("1"))
        assertEquals(Decimal("10.00"), Decimal("9.995") / Decimal("1"))
        assertEquals(Decimal("-10.00"), Decimal("-9.995") / Decimal("1"))
    }

    @Test
    fun divisionRoundsRepeatingQuotients() {
        assertEquals(Decimal("0.33"), Decimal("1") / Decimal("3"))
        assertEquals(Decimal("0.67"), Decimal("2") / Decimal("3"))
    }

    @Test
    fun divisionHandlesNegativeDivisors() {
        assertEquals(Decimal("-0.13"), Decimal("1") / Decimal("-8"))
        assertEquals(Decimal("0.13"), Decimal("-1") / Decimal("-8"))
    }

    @Test
    fun dividingByZeroThrowsAnArithmeticException() {
        for (zero in listOf("0", "0.00", "-0.00")) {
            for (numerator in listOf("1", "0")) {
                assertThrows("$numerator / $zero", ArithmeticException::class.java) {
                    Decimal(numerator) / Decimal(zero)
                }
                assertThrows("$numerator divide $zero", ArithmeticException::class.java) {
                    Decimal(numerator) divide Decimal(zero)
                }
            }
        }
    }

    @Test
    fun arithmeticRejectsInvalidOperands() {
        val operations: Map<String, (Decimal, Decimal) -> Decimal> = mapOf(
            "plus" to { left, right -> left + right },
            "minus" to { left, right -> left - right },
            "times" to { left, right -> left * right },
            "multiply" to { left, right -> left multiply right },
            "div" to { left, right -> left / right },
            "divide" to { left, right -> left divide right },
        )

        for ((name, operation) in operations) {
            for (invalid in listOf("", "abc", "1,25", "1.2.3", " 1.25 ", "NaN", "Infinity")) {
                assertThrows("Invalid left operand '$invalid' for $name", NumberFormatException::class.java) {
                    operation(Decimal(invalid), Decimal("1"))
                }
                assertThrows("Invalid right operand '$invalid' for $name", NumberFormatException::class.java) {
                    operation(Decimal("1"), Decimal(invalid))
                }
            }
        }
    }
}
