package co.mipyme.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import co.mipyme.ui.MoshiTheme

@Immutable
data class MoshiColors(
    val background: Background,
    val foreground: Foreground,
    val border: Border,
) {
    companion object {
        val background: Background
            @Composable
            @ReadOnlyComposable
            get() = MoshiTheme.colors.background

        val foreground: Foreground
            @Composable
            @ReadOnlyComposable
            get() = MoshiTheme.colors.foreground

        val border: Border
            @Composable
            @ReadOnlyComposable
            get() = MoshiTheme.colors.border

        val TextPrimary: Color
            @Composable
            @ReadOnlyComposable
            get() = foreground.primary

        val TextSecondary: Color
            @Composable
            @ReadOnlyComposable
            get() = foreground.secondary

        val TextTertiary: Color
            @Composable
            @ReadOnlyComposable
            get() = foreground.tertiary
    }

    @Immutable
    data class Background(
        val canvas: Color,
        val surface: Color,
        val elevated: Color,
        val selected: Color,
        val disabled: Color,
        val inverse: Color,
        val overlay: Color,
        val brand: Color,
        val action: BackgroundAction,
        val status: Status,
    )

    @Immutable
    data class BackgroundAction(
        val primary: PrimaryAction,
        val secondary: SecondaryAction,
        val destructive: DestructiveAction,
    )

    @Immutable
    data class PrimaryAction(
        val default: Color,
        val hover: Color,
    )

    @Immutable
    data class SecondaryAction(
        val hover: Color,
    )

    @Immutable
    data class DestructiveAction(
        val default: Color,
        val hover: Color,
    )

    @Immutable
    data class Status(
        val success: Color,
        val warning: Color,
        val error: Color,
        val info: Color,
    )

    @Immutable
    data class Foreground(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val disabled: Color,
        val inverse: Color,
        val link: Color,
        val onAction: OnAction,
        val action: ForegroundAction,
        val status: Status,
    )

    @Immutable
    data class OnAction(
        val primary: Color,
        val destructive: Color,
    )

    @Immutable
    data class ForegroundAction(
        val secondary: Color,
        val destructive: Color,
    )

    @Immutable
    data class Border(
        val default: Color,
        val strong: Color,
        val interactive: Color,
        val focus: Color,
        val status: Status,
    )
}

internal val lightMoshiColors = MoshiColors(
    background = MoshiColors.Background(
        canvas = Color(0xFFF0F0F2),
        surface = Color(0xFFFFFFFF),
        elevated = Color(0xFFFFFFFF),
        selected = Color(0xFFF7F7F7),
        disabled = Color(0xFFE5E5E5),
        inverse = Color(0xFF252525),
        overlay = Color(0x66000000),
        brand = Color(0xFFFF4C00),
        action = MoshiColors.BackgroundAction(
            primary = MoshiColors.PrimaryAction(
                default = Color(0xFF2B4FB9),
                hover = Color(0xFF002182),
            ),
            secondary = MoshiColors.SecondaryAction(
                hover = Color(0xFFE7EAF6),
            ),
            destructive = MoshiColors.DestructiveAction(
                default = Color(0xFFCA244D),
                hover = Color(0xFF64172B),
            ),
        ),
        status = MoshiColors.Status(
            success = Color(0xFFE9F6E9),
            warning = Color(0xFFFFF7C2),
            error = Color(0xFFFEEAED),
            info = Color(0xFFE6F4FE),
        ),
    ),
    foreground = MoshiColors.Foreground(
        primary = Color(0xFF252525),
        secondary = Color(0xFF666666),
        tertiary = Color(0xFF6B6B6B),
        disabled = Color(0xFF7A7A7A),
        inverse = Color(0xFFFFFFFF),
        link = Color(0xFF2B4FB9),
        onAction = MoshiColors.OnAction(
            primary = Color(0xFFFFFFFF),
            destructive = Color(0xFFFFFFFF),
        ),
        action = MoshiColors.ForegroundAction(
            secondary = Color(0xFF2B4FB9),
            destructive = Color(0xFFCA244D),
        ),
        status = MoshiColors.Status(
            success = Color(0xFF2A7E3B),
            warning = Color(0xFF4F3422),
            error = Color(0xFFCA244D),
            info = Color(0xFF113264),
        ),
    ),
    border = MoshiColors.Border(
        default = Color(0x1F000000),
        strong = Color(0x33000000),
        interactive = Color(0xFF5069C3),
        focus = Color(0xFF558EDD),
        status = MoshiColors.Status(
            success = Color(0xFFB2DDB5),
            warning = Color(0xFFF3D673),
            error = Color(0xFFF8BFC8),
            info = Color(0xFFACD8FC),
        ),
    ),
)

internal val darkMoshiColors = MoshiColors(
    background = MoshiColors.Background(
        canvas = Color(0xFF131419),
        surface = Color(0xFF262A34),
        elevated = Color(0xFF32353F),
        selected = Color(0xFF32353F),
        disabled = Color(0xFF1E222C),
        inverse = Color(0xFFFFFFFF),
        overlay = Color(0x99000000),
        brand = Color(0xFFFF4C00),
        action = MoshiColors.BackgroundAction(
            primary = MoshiColors.PrimaryAction(
                default = Color(0xFF2B4FB9),
                hover = Color(0xFF5069C3),
            ),
            secondary = MoshiColors.SecondaryAction(
                hover = Color(0xFF213554),
            ),
            destructive = MoshiColors.DestructiveAction(
                default = Color(0xFFB3445A),
                hover = Color(0xFF883447),
            ),
        ),
        status = MoshiColors.Status(
            success = Color(0xFF182A1D),
            warning = Color(0xFF302008),
            error = Color(0xFF3B121E),
            info = Color(0xFF0D2847),
        ),
    ),
    foreground = MoshiColors.Foreground(
        primary = Color(0xFFE5E5E5),
        secondary = Color(0xCCFFFFFF),
        tertiary = Color(0x99FFFFFF),
        disabled = Color(0x80FFFFFF),
        inverse = Color(0xFF252525),
        link = Color(0xFF9DA9CA),
        onAction = MoshiColors.OnAction(
            primary = Color(0xFFFFFFFF),
            destructive = Color(0xFFFFFFFF),
        ),
        action = MoshiColors.ForegroundAction(
            secondary = Color(0xFF9DA9CA),
            destructive = Color(0xFFFF949D),
        ),
        status = MoshiColors.Status(
            success = Color(0xFF71D083),
            warning = Color(0xFFFFCA16),
            error = Color(0xFFFF949D),
            info = Color(0xFF70B8FF),
        ),
    ),
    border = MoshiColors.Border(
        default = Color(0x1FFFFFFF),
        strong = Color(0x33FFFFFF),
        interactive = Color(0xFF9DA9CA),
        focus = Color(0xFF558EDD),
        status = MoshiColors.Status(
            success = Color(0xFF2D5637),
            warning = Color(0xFF5C3D05),
            error = Color(0xFF6F2539),
            info = Color(0xFF104D87),
        ),
    ),
)
