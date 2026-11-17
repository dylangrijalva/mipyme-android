package co.mipyme.ui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class MoshiColors(
    val foreground: ForegroundColors,
    val background: BackgroundColors,
    val border: BorderColors,
    val action: ActionColors,
    val selection: SelectionColors,
    val control: ControlColors,
    val progress: ProgressColors,
    val indicator: IndicatorColors,
    val overlay: OverlayColors,
    val status: StatusColors,
    val identity: IdentityColors,
)

@Immutable
data class ForegroundColors(
    val primary: Color,
    val secondary: Color,
    val disabled: Color,
    val disabledStrong: Color,
    val inverse: Color,
    val accent: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
)

@Immutable
data class BackgroundColors(
    val surface: Color,
    val subtle: Color,
    val elevated: Color,
    val disabled: Color,
    val selected: Color,
    val selectedStrong: Color,
    val inverse: Color,
    val accent: Color,
)

@Immutable
data class BorderColors(
    val default: Color,
    val hovered: Color,
    val active: Color,
    val focus: Color,
    val selected: Color,
    val error: Color,
    val danger: Color,
)

@Immutable
data class ActionColors(
    val primary: ActionTreatment,
    val secondary: SecondaryActionTreatment,
    val subtle: SubtleActionTreatment,
    val danger: DangerActionTreatment,
)

@Immutable
data class ActionTreatment(val background: Color, val foreground: Color, val hovered: Color)

@Immutable
data class SecondaryActionTreatment(val background: Color, val hovered: Color)

@Immutable
data class SubtleActionTreatment(val hovered: Color)

@Immutable
data class DangerActionTreatment(val background: Color, val hovered: Color, val foreground: Color)

@Immutable
data class SelectionColors(
    val background: Color,
    val hovered: Color,
    val foreground: Color,
    val border: Color,
    val indicator: Color,
    val disabledFill: Color,
    val disabledDot: Color,
)

@Immutable
data class ControlColors(val border: Color, val mutedBorder: Color, val disabledBorder: Color)

@Immutable
data class ProgressColors(val foreground: Color, val track: Color, val subtleTrack: Color)

@Immutable
data class IndicatorColors(val inactive: Color)

@Immutable
data class OverlayColors(val scrim: Color, val lightScrim: Color, val handle: Color)

@Immutable
data class StatusColors(
    val neutral: StatusTreatment,
    val information: StatusTreatment,
    val warning: StatusTreatment,
    val danger: StatusTreatment,
    val success: SuccessStatusTreatment,
)

@Immutable
data class StatusTreatment(
    val background: Color,
    val border: Color,
    val foreground: Color,
    val strongForeground: Color,
    val focus: Color,
)

@Immutable
data class SuccessStatusTreatment(val background: Color, val foreground: Color)

@Immutable
data class IdentityColors(val swatches: List<Pair<Color, Color>>)

internal fun MoshiPalette.toSemanticColors(): MoshiColors {
    val darkTheme = background100.luminance() < 0.5f
    return MoshiColors(
        foreground = ForegroundColors(
            primary = gray1000,
            secondary = gray900,
            disabled = gray700,
            disabledStrong = gray800,
            inverse = background100,
            accent = blue900,
            danger = red900,
            success = green900,
            warning = amber900,
        ),
        background = BackgroundColors(
            surface = background100,
            subtle = gray100,
            elevated = if (darkTheme) gray100 else background100,
            disabled = gray100,
            selected = gray200,
            selectedStrong = gray300,
            inverse = gray1000,
            accent = blue900,
        ),
        border = BorderColors(
            default = gray400,
            hovered = gray500,
            active = gray600,
            focus = blue900,
            selected = blue600,
            error = red900,
            danger = red400,
        ),
        action = ActionColors(
            primary = ActionTreatment(
                background = gray1000,
                foreground = background100,
                hovered = Color.hsl(0f, 0f, if (darkTheme) 0.8f else 0.22f),
            ),
            secondary = SecondaryActionTreatment(background100, grayAlpha200),
            subtle = SubtleActionTreatment(gray200),
            danger = DangerActionTreatment(
                background = red800,
                hovered = if (darkTheme) red900 else Color(0xFFAE292F),
                foreground = if (darkTheme) Color.White else Color(0xFFF5F5F5),
            ),
        ),
        selection = SelectionColors(
            background = blue100,
            hovered = blue200,
            foreground = blue900,
            border = blue600,
            indicator = blue900,
            disabledFill = gray600,
            disabledDot = gray900,
        ),
        control = ControlColors(gray700, gray500, gray400),
        progress = ProgressColors(gray1000, gray400, gray100),
        indicator = IndicatorColors(gray500),
        overlay = OverlayColors(grayAlpha700, Color(0x70000000), gray500),
        status = StatusColors(
            neutral = StatusTreatment(gray100, gray400, gray900, gray1000, blue600),
            information = StatusTreatment(blue100, blue400, blue900, blue1000, blue600),
            warning = StatusTreatment(amber100, amber400, amber900, amber1000, amber700),
            danger = StatusTreatment(red100, red400, red900, red1000, red700),
            success = SuccessStatusTreatment(green100, green900),
        ),
        identity = IdentityColors(
            listOf(
                blue200 to blue1000,
                green200 to green1000,
                teal200 to teal1000,
                purple200 to purple1000,
                pink200 to pink1000,
                amber200 to amber1000,
            ),
        ),
    )
}
