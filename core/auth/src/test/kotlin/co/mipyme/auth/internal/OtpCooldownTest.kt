package co.mipyme.auth.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpCooldownTest {

    @Test
    fun same_email_during_cooldown_is_blocked() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@example.com",
            requestedEmail = "ana@example.com",
        )

        assertTrue(blocking)
    }

    @Test
    fun same_email_with_different_case_or_spacing_is_blocked() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@example.com",
            requestedEmail = " Ana@Example.com ",
        )

        assertTrue(blocking)
    }

    @Test
    fun corrected_email_during_cooldown_is_allowed() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 30,
            cooldownEmail = "ana@exmaple.com",
            requestedEmail = "ana@example.com",
        )

        assertFalse(blocking)
    }

    @Test
    fun same_email_after_cooldown_is_allowed() {
        val blocking = isOtpCooldownBlocking(
            secondsRemaining = 0,
            cooldownEmail = "ana@example.com",
            requestedEmail = "ana@example.com",
        )

        assertFalse(blocking)
    }
}
