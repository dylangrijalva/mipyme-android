package co.mipyme.database.internal

import co.mipyme.database.FIXTURE_TIME
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatabaseTypeConvertersTest {
    private val converters = DatabaseTypeConverters()

    @Test
    fun `local date time round trips through its ISO string`() {
        val text = converters.fromLocalDateTime(FIXTURE_TIME)

        assertEquals("2026-11-14T10:30:15.123", text)
        assertEquals(FIXTURE_TIME, converters.toLocalDateTime(text))
    }

    @Test
    fun `local date time without fractional seconds round trips`() {
        val value = LocalDateTime.of(2026, 1, 2, 3, 4, 5)

        assertEquals(value, converters.toLocalDateTime(converters.fromLocalDateTime(value)))
    }

    @Test
    fun `null local date time stays null`() {
        assertNull(converters.fromLocalDateTime(null))
        assertNull(converters.toLocalDateTime(null))
    }
}
