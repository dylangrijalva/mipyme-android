package co.mipyme.auth.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpCooldownTest {

    @Test
    fun `same email during cooldown is blocked`() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@example.com",
            requestedEmail = "ana@example.com",
        )

        assertTrue(blocking)
    }

    @Test
    fun `same email with different case or spacing is blocked`() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@example.com",
            requestedEmail = " Ana@Example.com ",
        )

        assertTrue(blocking)
    }

    @Test
    fun `corrected email during cooldown is allowed`() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@exmaple.com",
            requestedEmail = "ana@example.com",
        )

        assertFalse(blocking)
    }

    @Test
    fun `same email after cooldown is allowed`() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 0,
            cooldownEmail = "ana@example.com",
            requestedEmail = "ana@example.com",
        )

        assertFalse(blocking)
    }
}
