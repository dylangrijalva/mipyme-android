package co.mipyme.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    @Test
    fun `accepts well formed addresses`() {
        assertTrue(isValidEmail("ada@example.com"))
        assertTrue(isValidEmail("ada.lovelace+tag@mail.example.co"))
        assertTrue(isValidEmail("a_b-c@sub-domain.example.org"))
    }

    @Test
    fun `accepts surrounding whitespace`() {
        assertTrue(isValidEmail("  ada@example.com  "))
    }

    @Test
    fun `rejects blank input`() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("   "))
    }

    @Test
    fun `rejects missing or repeated at sign`() {
        assertFalse(isValidEmail("ada.example.com"))
        assertFalse(isValidEmail("ada@@example.com"))
        assertFalse(isValidEmail("ada@exa@mple.com"))
    }

    @Test
    fun `rejects missing local part or domain`() {
        assertFalse(isValidEmail("@example.com"))
        assertFalse(isValidEmail("ada@"))
    }

    @Test
    fun `rejects domain without a dot`() {
        assertFalse(isValidEmail("ada@example"))
    }

    @Test
    fun `rejects malformed domain dots`() {
        assertFalse(isValidEmail("ada@example."))
        assertFalse(isValidEmail("ada@.com"))
        assertFalse(isValidEmail("ada@example..com"))
    }

    @Test
    fun `rejects inner spaces`() {
        assertFalse(isValidEmail("ada lovelace@example.com"))
        assertFalse(isValidEmail("ada@exa mple.com"))
    }
}
