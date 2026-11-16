package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.foundation.MoshiTypography
import co.mipyme.ui.foundation.MoshiColors

enum class MoshiTextRole {
    Heading16,
    Heading20,
    Heading24,
    Heading32,
    Heading14,
    Heading40,
    Heading48,
    Heading56,
    Heading64,
    Heading72,
    Copy13,
    Copy14,
    Copy14Mono,
    Copy16,
    Copy18,
    Copy20,
    Copy24,
    Label12,
    Label13,
    Label14,
    Label16,
    Button12,
    Button14,
    Button16,
    Copy13Mono,
    Label12Mono,
    Label13Mono,
    Label14Mono,
    Label18,
    Label20,
}

enum class MoshiTextEmphasis {
    Default,
    Strong,
    Subtle,
}

internal val LocalMoshiTextRole = staticCompositionLocalOf { MoshiTextRole.Copy16 }
internal val LocalMoshiTextColor = staticCompositionLocalOf { Color.Unspecified }

internal fun MoshiTextRole.style(typography: MoshiTypography): TextStyle {
    return when (this) {
        MoshiTextRole.Heading16 -> typography.heading16
        MoshiTextRole.Heading20 -> typography.heading20
        MoshiTextRole.Heading24 -> typography.heading24
        MoshiTextRole.Heading32 -> typography.heading32
        MoshiTextRole.Heading14 -> typography.heading14
        MoshiTextRole.Heading40 -> typography.heading40
        MoshiTextRole.Heading48 -> typography.heading48
        MoshiTextRole.Heading56 -> typography.heading56
        MoshiTextRole.Heading64 -> typography.heading64
        MoshiTextRole.Heading72 -> typography.heading72
        MoshiTextRole.Copy13 -> typography.copy13
        MoshiTextRole.Copy14 -> typography.copy14
        MoshiTextRole.Copy14Mono -> typography.copy14Mono
        MoshiTextRole.Copy16 -> typography.copy16
        MoshiTextRole.Copy18 -> typography.copy18
        MoshiTextRole.Copy20 -> typography.copy20
        MoshiTextRole.Copy24 -> typography.copy24
        MoshiTextRole.Label12 -> typography.label12
        MoshiTextRole.Label13 -> typography.label13
        MoshiTextRole.Label14 -> typography.label14
        MoshiTextRole.Label16 -> typography.label16
        MoshiTextRole.Button12 -> typography.button12
        MoshiTextRole.Button14 -> typography.button14
        MoshiTextRole.Button16 -> typography.button16
        MoshiTextRole.Copy13Mono -> typography.copy13Mono
        MoshiTextRole.Label12Mono -> typography.label12Mono
        MoshiTextRole.Label13Mono -> typography.label13Mono
        MoshiTextRole.Label14Mono -> typography.label14Mono
        MoshiTextRole.Label18 -> typography.label18
        MoshiTextRole.Label20 -> typography.label20
    }
}

/** Only the 15 modifiers present in the retained Geist CSS are supported. */
internal fun MoshiTextRole.emphasisStyle(emphasis: MoshiTextEmphasis, colors: MoshiColors): SpanStyle {
    if (emphasis == MoshiTextEmphasis.Default) return SpanStyle()
    return when (this) {
        MoshiTextRole.Heading16 -> {
            require(emphasis == MoshiTextEmphasis.Subtle) { "heading16 supports subtle emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray900)
        }
        MoshiTextRole.Heading20 -> {
            require(emphasis == MoshiTextEmphasis.Subtle) { "heading20 supports subtle emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray900)
        }
        MoshiTextRole.Heading24 -> {
            require(emphasis == MoshiTextEmphasis.Subtle) { "heading24 supports subtle emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray900)
        }
        MoshiTextRole.Heading32 -> {
            require(emphasis == MoshiTextEmphasis.Subtle) { "heading32 supports subtle emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray900)
        }
        MoshiTextRole.Copy13 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy13 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy14 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy14 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy14Mono -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy14Mono supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy16 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy16 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy18 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy18 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy20 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy20 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Copy24 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "copy24 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(550), color = colors.gray1000)
        }
        MoshiTextRole.Label12 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "label12 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray1000)
        }
        MoshiTextRole.Label13 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "label13 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray1000)
        }
        MoshiTextRole.Label14 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "label14 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray1000)
        }
        MoshiTextRole.Label16 -> {
            require(emphasis == MoshiTextEmphasis.Strong) { "label16 supports strong emphasis" }
            SpanStyle(fontWeight = FontWeight(500), color = colors.gray1000)
        }
        else -> error("$this has no Geist emphasis modifier")
    }
}

/** Apply Geist emphasis to a range. Explicit text or component colors still take precedence. */
fun AnnotatedString.Builder.withMoshiEmphasis(
    block: AnnotatedString.Builder.() -> Unit,
) {
    withAnnotation(tag = MOSHI_EMPHASIS_TAG, annotation = "true", block = block)
}

private const val MOSHI_EMPHASIS_TAG = "co.mipyme.ui.emphasis"

internal fun emphasizedText(
    text: AnnotatedString,
    role: MoshiTextRole,
    emphasis: MoshiTextEmphasis,
    colors: MoshiColors,
    colorOverride: Color,
): AnnotatedString {
    return buildAnnotatedString {
        fun applyEmphasis(value: MoshiTextEmphasis, start: Int, end: Int) {
            val style = role.emphasisStyle(value, colors)
            addStyle(if (colorOverride != Color.Unspecified) style.copy(color = colorOverride) else style, start, end)
        }
        if (emphasis != MoshiTextEmphasis.Default) applyEmphasis(emphasis, 0, text.length)
        for (range in text.getStringAnnotations(MOSHI_EMPHASIS_TAG, 0, text.length)) {
            val inlineEmphasis = when (role) {
                MoshiTextRole.Heading16, MoshiTextRole.Heading20,
                MoshiTextRole.Heading24, MoshiTextRole.Heading32 -> MoshiTextEmphasis.Subtle
                else -> MoshiTextEmphasis.Strong
            }
            applyEmphasis(inlineEmphasis, range.start, range.end)
        }
        // Append caller annotations last so their explicit spans override the role's defaults.
        append(text)
    }
}

@Composable
fun MoshiText(
    text: String,
    modifier: Modifier = Modifier,
    role: MoshiTextRole = LocalMoshiTextRole.current,
    color: Color = Color.Unspecified,
    emphasis: MoshiTextEmphasis = MoshiTextEmphasis.Default,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    MoshiText(AnnotatedString(text), modifier, role, color, emphasis, textAlign, maxLines, overflow)
}

@Composable
fun MoshiText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    role: MoshiTextRole = LocalMoshiTextRole.current,
    color: Color = Color.Unspecified,
    emphasis: MoshiTextEmphasis = MoshiTextEmphasis.Default,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val inheritedColor = LocalMoshiTextColor.current
    val colorOverride = color.takeOrElse { inheritedColor }
    Text(
        text = emphasizedText(text, role, emphasis, Moshi.colors, colorOverride),
        modifier = modifier,
        color = colorOverride.takeOrElse { Moshi.colors.gray1000 },
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = role.style(Moshi.typography),
    )
}

@MoshiPreview
@Composable
private fun MoshiTextRolesPreview() {
    MoshiTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .background(Moshi.colors.background100)
                .padding(Moshi.spacing.space4),
        ) {
            for (role in MoshiTextRole.entries) {
                MoshiText(text = role.name, role = role)
            }
        }
    }
}
