package co.mipyme.data.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system follows the device appearance`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(isSystemDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(isSystemDark = false))
    }

    @Test
    fun `light is never dark`() {
        assertEquals(false, ThemeMode.LIGHT.isDark(isSystemDark = true))
        assertEquals(false, ThemeMode.LIGHT.isDark(isSystemDark = false))
    }

    @Test
    fun `dark is always dark`() {
        assertEquals(true, ThemeMode.DARK.isDark(isSystemDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(isSystemDark = false))
    }
}
