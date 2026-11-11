package co.mipyme.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import co.mipyme.screen.HomeScreen
import co.mipyme.screen.WelcomeScreen

@Composable
fun NavigationRoot() {
    val backStack = rememberNavBackStack(Route.Welcome)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Route.Welcome> { WelcomeScreen() }
            entry<Route.Home> { HomeScreen() }
        }
    )
}
