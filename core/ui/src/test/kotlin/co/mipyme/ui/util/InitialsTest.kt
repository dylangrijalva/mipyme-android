package co.mipyme.ui.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun usesFirstTwoWordsAndUppercasesThem() {
        assertEquals("FV", "foosha village".initials())
        assertEquals("WC", "whole cake island".initials())
    }

    @Test
    fun singleWordHasOneInitial() {
        assertEquals("N", "nami".initials())
        assertEquals("N", "n".initials())
    }

    @Test
    fun ignoresLeadingTrailingAndRepeatedWhitespace() {
        assertEquals("FV", "  foosha   village  ".initials())
        assertEquals("FV", "\tfoosha\n\rvillage\t".initials())
        assertEquals("FV", "\u00A0foosha\u2003village\u00A0".initials())
    }

    @Test
    fun blankTextHasNoInitials() {
        assertEquals("", "".initials())
        assertEquals("", " \t\n\r\u00A0\u2003 ".initials())
    }

    @Test
    fun preservesAccentedInitials() {
        assertEquals("ÁÑ", "álamo ñandú".initials())
    }

    @Test
    fun preservesSupplementaryUnicodeCharacters() {
        assertEquals("\uD801\uDC00V", "\uD801\uDC28 village".initials())
    }

    @Test
    fun uppercasingDoesNotDependOnDeviceLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("ID", "impel down".initials())
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
