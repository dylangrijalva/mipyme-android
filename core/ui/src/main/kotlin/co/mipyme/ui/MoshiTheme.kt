package co.mipyme.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import co.mipyme.ui.foundation.MoshiColors
import co.mipyme.ui.foundation.MoshiSpacing
import co.mipyme.ui.foundation.MoshiTypography
import co.mipyme.ui.foundation.darkMoshiColors
import co.mipyme.ui.foundation.lightMoshiColors

private val LocalMoshiColors = staticCompositionLocalOf<MoshiColors> {
    error("MoshiTheme.colors must be read inside MoshiTheme")
}

private val LocalMoshiTypography = staticCompositionLocalOf<MoshiTypography> {
    error("MoshiTheme.typography must be read inside MoshiTheme")
}

private val LocalMoshiSpacing = staticCompositionLocalOf<MoshiSpacing> {
    error("MoshiTheme.spacing must be read inside MoshiTheme")
}

@Composable
fun MoshiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkMoshiColors() else lightMoshiColors()

    CompositionLocalProvider(
        LocalMoshiColors provides colors,
        LocalMoshiTypography provides MoshiTypography(),
        LocalMoshiSpacing provides MoshiSpacing(),
        content = content,
    )
}

object MoshiTheme {
    val colors: MoshiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMoshiColors.current

    val typography: MoshiTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMoshiTypography.current

    val spacing: MoshiSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMoshiSpacing.current
}
