package co.mipyme.ui.foundation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticColorsTest {
    @Test
    fun lightRolesMatchRetainedPaletteAndAuthoredColors() {
        val palette = lightMoshiPalette()
        assertEquals(expectedSemanticColors(palette, darkTheme = false), palette.toSemanticColors())
    }

    @Test
    fun darkRolesMatchRetainedPaletteAndAuthoredColors() {
        val palette = darkMoshiPalette()
        assertEquals(expectedSemanticColors(palette, darkTheme = true), palette.toSemanticColors())
    }

    @Test
    fun scrimRolesPreserveThemeSpecificSheetAndLightDrawerValues() {
        val lightPalette = lightMoshiPalette()
        val darkPalette = darkMoshiPalette()
        assertEquals(lightPalette.grayAlpha700, lightPalette.toSemanticColors().overlay.scrim)
        assertEquals(darkPalette.grayAlpha700, darkPalette.toSemanticColors().overlay.scrim)
        assertEquals(lightPalette.grayAlpha700, lightPalette.toSemanticColors().overlay.lightScrim)
        assertEquals(lightPalette.grayAlpha700, darkPalette.toSemanticColors().overlay.lightScrim)
    }

    private fun expectedSemanticColors(palette: MoshiPalette, darkTheme: Boolean): MoshiColors {
        return MoshiColors(
            foreground = ForegroundColors(palette.gray1000, palette.gray900, palette.gray700, palette.gray800, palette.background100, palette.blue900, palette.red900, palette.green900, palette.amber900),
            background = BackgroundColors(palette.background100, palette.gray100, if (darkTheme) palette.gray100 else palette.background100, palette.gray100, palette.gray200, palette.gray300, palette.gray1000, palette.blue900),
            border = BorderColors(palette.gray400, palette.gray500, palette.gray600, palette.blue900, palette.blue600, palette.red900, palette.red400),
            action = ActionColors(
                ActionTreatment(palette.gray1000, palette.background100, Color.hsl(0f, 0f, if (darkTheme) 0.8f else 0.22f)),
                SecondaryActionTreatment(palette.background100, palette.grayAlpha200),
                SubtleActionTreatment(palette.gray200),
                DangerActionTreatment(
                    palette.red800,
                    if (darkTheme) palette.red900 else Color(0xFFAE292F),
                    if (darkTheme) Color.White else Color(0xFFF5F5F5),
                ),
            ),
            selection = SelectionColors(palette.blue100, palette.blue200, palette.blue900, palette.blue600, palette.blue900, palette.gray600, palette.gray900),
            control = ControlColors(palette.gray700, palette.gray500, palette.gray400),
            progress = ProgressColors(palette.gray1000, palette.gray400, palette.gray100),
            indicator = IndicatorColors(palette.gray500),
            overlay = OverlayColors(palette.grayAlpha700, Color(0x70000000), palette.gray500),
            status = StatusColors(
                StatusTreatment(palette.gray100, palette.gray400, palette.gray900, palette.gray1000, palette.blue600),
                StatusTreatment(palette.blue100, palette.blue400, palette.blue900, palette.blue1000, palette.blue600),
                StatusTreatment(palette.amber100, palette.amber400, palette.amber900, palette.amber1000, palette.amber700),
                StatusTreatment(palette.red100, palette.red400, palette.red900, palette.red1000, palette.red700),
                SuccessStatusTreatment(palette.green100, palette.green900),
            ),
            identity = IdentityColors(
                listOf(
                    palette.blue200 to palette.blue1000,
                    palette.green200 to palette.green1000,
                    palette.teal200 to palette.teal1000,
                    palette.purple200 to palette.purple1000,
                    palette.pink200 to palette.pink1000,
                    palette.amber200 to palette.amber1000,
                ),
            ),
        )
    }
}
