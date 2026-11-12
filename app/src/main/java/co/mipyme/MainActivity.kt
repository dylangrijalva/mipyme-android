package co.mipyme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.navigation.NavigationRoot
import co.mipyme.navigation.Route
import co.mipyme.ui.MoshiTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.uiState.launchDestination == LaunchDestination.Idle
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
}

private fun LaunchDestination.toRoute(): Route? {
    return when (this) {
        LaunchDestination.Idle -> null
        LaunchDestination.Welcome -> Route.Welcome
        LaunchDestination.Home -> Route.Home
    }
}
