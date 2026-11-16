package co.mipyme.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import co.mipyme.ui.foundation.MoshiColors
import co.mipyme.ui.foundation.MoshiSpacing
import co.mipyme.ui.foundation.MoshiTypography
import co.mipyme.ui.foundation.darkMoshiColors
import co.mipyme.ui.foundation.lightMoshiColors

private val LocalMoshiColors = staticCompositionLocalOf<MoshiColors> {
    error("Moshi.colors must be read inside MoshiTheme")
}

private val LocalMoshiTypography = staticCompositionLocalOf<MoshiTypography> {
    error("Moshi.typography must be read inside MoshiTheme")
}

private val LocalMoshiSpacing = staticCompositionLocalOf<MoshiSpacing> {
    error("Moshi.spacing must be read inside MoshiTheme")
}

@Composable
fun MoshiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Compose UI 1.11 converts solid paint colors to sRGB. Use Geist's authored fallback,
    // rather than clipping its P3 override through that renderer.
    val colors = remember(darkTheme) {
        if (darkTheme) darkMoshiColors() else lightMoshiColors()
    }
    val typography = remember { MoshiTypography() }
    val configuration = LocalConfiguration.current
    val themedConfiguration = remember(configuration, darkTheme) {
        Configuration(configuration).apply {
            val nightMode = if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
    }
    val context = LocalContext.current
    val themedContext = remember(context, themedConfiguration) {
        ContextThemeWrapper(context.createConfigurationContext(themedConfiguration), 0).apply {
            theme.setTo(context.theme)
        }
    }

    CompositionLocalProvider(
        LocalConfiguration provides themedConfiguration,
        LocalContext provides themedContext,
        LocalResources provides themedContext.resources,
        LocalMoshiColors provides colors,
        LocalMoshiTypography provides typography,
        LocalMoshiSpacing provides MoshiSpacing(),
        content = content,
    )
}

object Moshi {
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
