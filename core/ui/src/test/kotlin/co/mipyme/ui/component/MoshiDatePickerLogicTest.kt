package co.mipyme.ui.component

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class MoshiDatePickerLogicTest {
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun strictDatesRespectLocaleOrderAndLeapYears() {
        assertEquals(LocalDate.of(2024, 2, 29), parseMoshiDate("2/29/2024", Locale.US))
        assertNull(parseMoshiDate("2/29/2025", Locale.US))
        assertNull(parseMoshiDate("4/31/2026", Locale.US))
        assertNull(parseMoshiDate("2/20/26", Locale.US))
        assertNull(parseMoshiDate("2/20/0000", Locale.US))
        assertNull(parseMoshiDate("2/20/2026 extra", Locale.US))
        assertEquals(LocalDate.of(2026, 11, 16), parseMoshiDate("16/11/2026", Locale.forLanguageTag("es-NI")))
        assertNull(parseMoshiDate("11/16/2026", Locale.UK))
        for (tag in listOf("en-US", "en-GB", "es-NI", "ar-EG", "ja-JP", "th-TH")) {
            val locale = Locale.forLanguageTag(tag)
            assertEquals(tag, today, parseMoshiDate(today.format(dateInputFormatter(locale)), locale))
        }
    }

    @Test
    fun relativeGrammarIsDeterministicAndRejectsUnsupportedText() {
        assertEquals(today, parseMoshiNaturalDate("today", Locale.US, today))
        assertEquals(today.plusDays(1), parseMoshiNaturalDate("tomorrow", Locale.US, today))
        assertEquals(today.plusDays(7), parseMoshiNaturalDate("next monday", Locale.US, today))
        assertEquals(today.plusDays(1), parseMoshiNaturalDate("next tuesday", Locale.US, today))
        assertEquals(today.plusWeeks(2), parseMoshiNaturalDate("in 2 weeks", Locale.US, today))
        val spanish = Locale.forLanguageTag("es")
        assertEquals(today, parseMoshiNaturalDate("hoy", spanish, today))
        assertEquals(today.plusDays(1), parseMoshiNaturalDate(" MAÑANA ", spanish, today))
        assertEquals(today.plusDays(2), parseMoshiNaturalDate("el próximo miércoles", spanish, today))
        assertEquals(today.plusDays(7), parseMoshiNaturalDate("próximo lunes", spanish, today))
        assertEquals(today.plusDays(3), parseMoshiNaturalDate("en 3 días", spanish, today))
        assertEquals(today.plusWeeks(2), parseMoshiNaturalDate("en 2 semanas", spanish, today))
        for (text in listOf("next month", "yesterday", "in -1 days", "in 99999999999999999999 weeks", "in 9223372036854775807 days", "in 9223372036854775807 weeks")) {
            assertNull(text, parseMoshiNaturalDate(text, Locale.US, today))
        }
        assertNull(parseMoshiNaturalDate("today", spanish, today))
        assertEquals(today, parseMoshiNaturalDate("today", Locale.FRENCH, today))
        assertEquals(today.plusDays(1), parseMoshiNaturalDate("mañana", Locale.FRENCH, today))
        assertNull(parseMoshiNaturalDate("tomorrow", Locale.US, LocalDate.of(9999, 12, 31)))
    }

    @Test
    fun rangesAndPresetsRejectDisabledInteriorAndBounds() {
        val constraints = MoshiDateConstraints(today, today.plusDays(10)) { it == today.plusDays(3) }
        assertTrue(constraints.allows(today))
        assertTrue(constraints.allows(MoshiDateRange(today, today.plusDays(2))))
        assertFalse(constraints.allows(today.minusDays(1)))
        assertFalse(constraints.allows(MoshiDateRange(today, today.plusDays(4))))
        assertFalse(constraints.allows(MoshiDateRange(today.plusDays(2), today)))
        assertFalse(constraints.allows(MoshiDateRange(today, today.plusDays(11))))
        assertFalse(constraints.allows(MoshiDatePreset("Closed", today.plusDays(3)).date))
        assertTrue(MoshiDateConstraints().allows(MoshiDateRange(LocalDate.of(9999, 12, 31), LocalDate.of(9999, 12, 31))))
        assertFalse(MoshiDateConstraints().allows(LocalDate.MIN))
        assertFalse(MoshiDateConstraints().allows(LocalDate.MAX))
        assertFalse(constraints.allows(MoshiDateRangePreset("Closed", MoshiDateRange(today, today.plusDays(4))).range))
    }
}
