package co.mipyme.ui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.sp
import co.mipyme.ui.R

private val GeistFontFamily = FontFamily(
    Font(R.font.geist_thin, FontWeight.Thin),
    Font(R.font.geist_extralight, FontWeight.ExtraLight),
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_550, FontWeight(550)),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
    Font(R.font.geist_extrabold, FontWeight.ExtraBold),
    Font(R.font.geist_black, FontWeight.Black),
)

private val GeistMonoFontFamily = FontFamily(
    Font(R.font.geist_mono_400, FontWeight.Normal),
    Font(R.font.geist_mono_550, FontWeight(550)),
)

/** Sizes, line heights and explicit tracking scale with Android font preferences. */
@Immutable
data class MoshiTypography(
    val heading16: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -0.32.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading20: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -0.4.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading24: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -0.96.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading32: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -1.28.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading14: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -0.28.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading40: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -2.4.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading48: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -2.88.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading56: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -3.36.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading64: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -3.84.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val heading72: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        fontWeight = FontWeight(600),
        letterSpacing = -4.32.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy13: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy14: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy14Mono: TextStyle = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy16: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy18: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy20: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 20.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy24: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label12: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label13: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label14: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label16: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val button12: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(500),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val button14: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(500),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val button16: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(500),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val copy13Mono: TextStyle = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label12Mono: TextStyle = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label13Mono: TextStyle = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label14Mono: TextStyle = TextStyle(
        fontFamily = GeistMonoFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label18: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val label20: TextStyle = TextStyle(
        fontFamily = GeistFontFamily,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)
