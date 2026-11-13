package co.mipyme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun NavigationRoot(startRoute: Route) {
    val backStack = rememberNavBackStack(startRoute)

    LaunchedEffect(startRoute) {
        if (backStack.firstOrNull() != startRoute) {
            backStack.clear()
            backStack.add(startRoute)
        }
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = koinEntryProvider<NavKey>(),
    )
}
