package co.mipyme.ui.foundation

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiTextEmphasis
import co.mipyme.ui.component.emphasisStyle
import co.mipyme.ui.component.emphasizedText
import co.mipyme.ui.component.style
import co.mipyme.ui.component.withMoshiEmphasis
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import org.junit.Assert.*
import org.junit.Test

class GeistTokensTest {
    private fun rows(name: String): List<List<String>> {
        return javaClass.getResourceAsStream("/$name")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }
    }

    private fun palette(theme: String, p3: Boolean = false): Map<String, Color> {
        val colors = if (theme == "light") lightMoshiPalette(p3) else darkMoshiPalette(p3)
        return mapOf(
            "gray100" to colors.gray100,
            "gray200" to colors.gray200,
            "gray300" to colors.gray300,
            "gray400" to colors.gray400,
            "gray500" to colors.gray500,
            "gray600" to colors.gray600,
            "gray700" to colors.gray700,
            "gray800" to colors.gray800,
            "gray900" to colors.gray900,
            "gray1000" to colors.gray1000,
            "grayAlpha100" to colors.grayAlpha100,
            "grayAlpha200" to colors.grayAlpha200,
            "grayAlpha300" to colors.grayAlpha300,
            "grayAlpha400" to colors.grayAlpha400,
            "grayAlpha500" to colors.grayAlpha500,
            "grayAlpha600" to colors.grayAlpha600,
            "grayAlpha700" to colors.grayAlpha700,
            "grayAlpha800" to colors.grayAlpha800,
            "grayAlpha900" to colors.grayAlpha900,
            "grayAlpha1000" to colors.grayAlpha1000,
            "background100" to colors.background100,
            "background200" to colors.background200,
            "blue100" to colors.blue100,
            "blue200" to colors.blue200,
            "blue300" to colors.blue300,
            "blue400" to colors.blue400,
            "blue500" to colors.blue500,
            "blue600" to colors.blue600,
            "blue700" to colors.blue700,
            "blue800" to colors.blue800,
            "blue900" to colors.blue900,
            "blue1000" to colors.blue1000,
            "red100" to colors.red100,
            "red200" to colors.red200,
            "red300" to colors.red300,
            "red400" to colors.red400,
            "red500" to colors.red500,
            "red600" to colors.red600,
            "red700" to colors.red700,
            "red800" to colors.red800,
            "red900" to colors.red900,
            "red1000" to colors.red1000,
            "amber100" to colors.amber100,
            "amber200" to colors.amber200,
            "amber300" to colors.amber300,
            "amber400" to colors.amber400,
            "amber500" to colors.amber500,
            "amber600" to colors.amber600,
            "amber700" to colors.amber700,
            "amber800" to colors.amber800,
            "amber900" to colors.amber900,
            "amber1000" to colors.amber1000,
            "green100" to colors.green100,
            "green200" to colors.green200,
            "green300" to colors.green300,
            "green400" to colors.green400,
            "green500" to colors.green500,
            "green600" to colors.green600,
            "green700" to colors.green700,
            "green800" to colors.green800,
            "green900" to colors.green900,
            "green1000" to colors.green1000,
            "teal100" to colors.teal100,
            "teal200" to colors.teal200,
            "teal300" to colors.teal300,
            "teal400" to colors.teal400,
            "teal500" to colors.teal500,
            "teal600" to colors.teal600,
            "teal700" to colors.teal700,
            "teal800" to colors.teal800,
            "teal900" to colors.teal900,
            "teal1000" to colors.teal1000,
            "purple100" to colors.purple100,
            "purple200" to colors.purple200,
            "purple300" to colors.purple300,
            "purple400" to colors.purple400,
            "purple500" to colors.purple500,
            "purple600" to colors.purple600,
            "purple700" to colors.purple700,
            "purple800" to colors.purple800,
            "purple900" to colors.purple900,
            "purple1000" to colors.purple1000,
            "pink100" to colors.pink100,
            "pink200" to colors.pink200,
            "pink300" to colors.pink300,
            "pink400" to colors.pink400,
            "pink500" to colors.pink500,
            "pink600" to colors.pink600,
            "pink700" to colors.pink700,
            "pink800" to colors.pink800,
            "pink900" to colors.pink900,
            "pink1000" to colors.pink1000,
        )
    }

    @Test
    fun allAuthoredFallbacksAndCssAlphaOrderingMatch() {
        val rows = rows("geist-colors.tsv")
        assertEquals(184, rows.size)
        val number = Regex("[0-9.]+")
        for ((theme, name, css) in rows) {
            val actual = palette(theme).getValue(name)
            assertEquals(ColorSpaces.Srgb, actual.colorSpace)
            if (css.startsWith("#")) {
                val rgba = css.drop(1)
                val expected = (rgba.takeLast(2) + rgba.dropLast(2)).toLong(16).toInt()
                assertEquals("$theme $name", expected, actual.toArgb())
            } else {
                val (h, s, l) = number.findAll(css).map { it.value.toFloat() }.toList()
                // Independent HSL conversion, checking unrounded channel values.
                val a = s / 100f * minOf(l / 100f, 1f - l / 100f)
                fun channel(n: Float): Float {
                    val k = (n + h / 30f) % 12f
                    return l / 100f - a * maxOf(-1f, minOf(k - 3f, 9f - k, 1f))
                }
                assertEquals("$theme $name red", channel(0f), actual.red, 0.002f)
                assertEquals("$theme $name green", channel(8f), actual.green, 0.002f)
                assertEquals("$theme $name blue", channel(4f), actual.blue, 0.002f)
            }
        }
        assertEquals(13f / 255f, lightMoshiPalette().grayAlpha100.alpha, 0.00001f)
        assertEquals(15f / 255f, darkMoshiPalette().grayAlpha100.alpha, 0.00001f)
    }

    @Test
    fun everyWideGamutOverrideRetainsP3AndMatchesIndependentConversion() {
        val rows = rows("geist-p3.tsv")
        assertEquals(140, rows.size)
        for (row in rows) {
            val color = palette(row[0], true).getValue(row[1])
            assertEquals(ColorSpaces.DisplayP3, color.colorSpace)
            assertEquals(row.take(2).toString(), row[3].toFloat(), color.red, 0.002f)
            assertEquals(row.take(2).toString(), row[4].toFloat(), color.green, 0.002f)
            assertEquals(row.take(2).toString(), row[5].toFloat(), color.blue, 0.002f)
        }
        assertEquals(ColorSpaces.Srgb, lightMoshiPalette(true).gray100.colorSpace)
        assertEquals(ColorSpaces.Srgb, darkMoshiPalette(true).background100.colorSpace)
    }

    @Test
    fun completeTypographyTaxonomyAndUnspecifiedTrackingMatch() {
        val typography = MoshiTypography()
        val styles = mapOf(
            "heading16" to typography.heading16,
            "heading20" to typography.heading20,
            "heading24" to typography.heading24,
            "heading32" to typography.heading32,
            "heading14" to typography.heading14,
            "heading40" to typography.heading40,
            "heading48" to typography.heading48,
            "heading56" to typography.heading56,
            "heading64" to typography.heading64,
            "heading72" to typography.heading72,
            "copy13" to typography.copy13,
            "copy14" to typography.copy14,
            "copy14Mono" to typography.copy14Mono,
            "copy16" to typography.copy16,
            "copy18" to typography.copy18,
            "copy20" to typography.copy20,
            "copy24" to typography.copy24,
            "label12" to typography.label12,
            "label13" to typography.label13,
            "label14" to typography.label14,
            "label16" to typography.label16,
            "button12" to typography.button12,
            "button14" to typography.button14,
            "button16" to typography.button16,
            "copy13Mono" to typography.copy13Mono,
            "label12Mono" to typography.label12Mono,
            "label13Mono" to typography.label13Mono,
            "label14Mono" to typography.label14Mono,
            "label18" to typography.label18,
            "label20" to typography.label20,
        )
        assertEquals(30, MoshiTextRole.entries.size)
        assertEquals(30, rows("geist-type.tsv").size)
        for ((name, size, height, weight, tracking) in rows("geist-type.tsv")) {
            val style = styles.getValue(name)
            assertEquals(size.toFloat().sp, style.fontSize)
            assertEquals(height.toFloat().sp, style.lineHeight)
            assertEquals(weight.toInt(), style.fontWeight!!.weight)
            assertEquals(if (tracking == "unspecified") TextUnit.Unspecified else tracking.toFloat().sp, style.letterSpacing)
            val role = MoshiTextRole.valueOf(name.replaceFirstChar { it.uppercase() })
            assertEquals(style, role.style(typography))
        }
    }

    @Test
    fun exactModifiersAndExplicitOrDisabledColorsTakePrecedence() {
        val colors = lightMoshiPalette().toSemanticColors()
        var count = 0
        for (role in MoshiTextRole.entries) {
            val emphasis = if (role.name in listOf("Heading16", "Heading20", "Heading24", "Heading32")) {
                MoshiTextEmphasis.Subtle
            } else MoshiTextEmphasis.Strong
            val span = runCatching { role.emphasisStyle(emphasis, colors) }.getOrNull() ?: continue
            count++
            assertEquals(if (role.name.startsWith("Copy")) 550 else 500, span.fontWeight!!.weight)
            assertEquals(if (emphasis == MoshiTextEmphasis.Subtle) colors.foreground.secondary else colors.foreground.primary, span.color)
        }
        assertEquals(15, count)
        val annotated = buildAnnotatedString {
            append("Copy ")
            withMoshiEmphasis { append("Strong") }
        }
        val normal = emphasizedText(annotated, MoshiTextRole.Copy16, MoshiTextEmphasis.Default, colors, Color.Unspecified)
        assertEquals(550, normal.spanStyles.single().item.fontWeight!!.weight)
        assertEquals(5, normal.spanStyles.single().start)
        for (override in listOf(colors.foreground.disabled, colors.foreground.danger)) {
            val text = emphasizedText(annotated, MoshiTextRole.Copy16, MoshiTextEmphasis.Strong, colors, override)
            assertTrue(text.spanStyles.all { it.item.color == override })
        }
    }
    @Test
    fun explicitCallerSpansOverrideWholeRoleAndInlineEmphasis() {
        val caller = SpanStyle(color = Color.Red, fontWeight = FontWeight.Normal)
        val text = buildAnnotatedString {
            withMoshiEmphasis { withStyle(caller) { append("Copy") } }
        }
        val result = emphasizedText(text, MoshiTextRole.Copy16, MoshiTextEmphasis.Strong,
            lightMoshiPalette().toSemanticColors(), Color.Unspecified)
        // Later spans win in AnnotatedString. Caller styling must remain the last layer.
        assertEquals(caller, result.spanStyles.last().item)
        assertEquals(3, result.spanStyles.size)
        assertEquals(550, result.spanStyles.first().item.fontWeight!!.weight)
    }

}
