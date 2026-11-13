package co.mipyme

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import co.mipyme.navigation.NavigationRoot
import co.mipyme.ui.MoshiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import kotlin.time.Duration.Companion.milliseconds

fun Module.mainScreenModule(): Module {
    viewModelOf(::MainViewModel)
    return this
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.uiState.launchDestination == LaunchDestination.Idle }
            setOnExitAnimationListener { provider -> playSplashExitAnimation(provider) }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()
            val startRoute = state.launchDestination.toRoute()

            MoshiTheme {
                if (startRoute != null) {
                    NavigationRoot(startRoute = startRoute)
                }
            }
        }
    }

    private fun playSplashExitAnimation(provider: SplashScreenViewProvider) {
        lifecycleScope.launch {
            try {
                if (areAnimatorsEnabled()) {
                    val iconAnimationDuration = provider.iconAnimationDurationMillis
                    val iconAnimationEnd = provider.iconAnimationStartMillis + iconAnimationDuration
                    val iconAnimationTimeRemaining = iconAnimationEnd - System.currentTimeMillis()
                    delay(iconAnimationTimeRemaining.coerceIn(0L, iconAnimationDuration).milliseconds)
                }
                val interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
                runCatching { provider.iconView }.getOrNull()?.animate()
                    ?.alpha(0f)
                    ?.scaleX(SPLASH_EXIT_ICON_SCALE)
                    ?.scaleY(SPLASH_EXIT_ICON_SCALE)
                    ?.setDuration(SPLASH_EXIT_DURATION_MILLIS)
                    ?.setInterpolator(interpolator)
                    ?.start()
                provider.view.animate()
                    .alpha(0f)
                    .setDuration(SPLASH_EXIT_DURATION_MILLIS)
                    .setInterpolator(interpolator)
                    .withEndAction { provider.remove() }
                    .start()
            } finally {
                if (!isActive) {
                    provider.remove()
                }
            }
        }
    }

    private fun areAnimatorsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }

    private companion object {
        const val SPLASH_EXIT_DURATION_MILLIS = 300L
        const val SPLASH_EXIT_ICON_SCALE = 1.15f
    }
}
