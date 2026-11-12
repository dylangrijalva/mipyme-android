package co.mipyme.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun NavigationRoot(startRoute: Route) {
    val backStack = rememberNavBackStack(startRoute)

    NavDisplay(
        backStack = backStack,
        entryProvider = koinEntryProvider<NavKey>(),
    )
}
