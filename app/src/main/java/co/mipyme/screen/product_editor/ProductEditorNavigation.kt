package co.mipyme.screen.product_editor

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import co.mipyme.navigation.LocalFinishProductEditor
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ProductEditorWorkflow(
    route: Route.ProductEditor,
    viewModel: ProductEditorViewModel = koinViewModel { parametersOf(route) },
) {
    val root = LocalNavBackStack.current
    val finish = LocalFinishProductEditor.current
    val workflow = rememberNavBackStack(Route.ProductDetails)
    NavDisplay(
        backStack = workflow,
        entryProvider = { key ->
            NavEntry(key, metadata = NavigationTransitions.forwardWithBackwardPop) {
                when (key) {
                    Route.ProductDetails -> ProductEditorScreen(
                        viewModel = viewModel,
                        onBack = { if (root.lastOrNull() == route) root.removeLastOrNull() },
                        onPricing = { if (workflow.lastOrNull() == key) workflow.add(Route.ProductPricing) },
                        onSaved = { id -> finish(route, id) },
                        onAdjustStock = { locationId ->
                            if (workflow.lastOrNull() == key) workflow.add(Route.ProductAdjustStock(locationId))
                        },
                    )
                    Route.ProductPricing -> ProductPricingScreen(
                        viewModel = viewModel,
                        onBack = { if (workflow.lastOrNull() == key) workflow.removeLastOrNull() },
                    )
                    is Route.ProductAdjustStock -> ProductAdjustStockScreen(
                        viewModel = viewModel,
                        locationId = key.locationId,
                        onBack = { if (workflow.lastOrNull() == key) workflow.removeLastOrNull() },
                    )
                    else -> error("Unknown product editor destination")
                }
            }
        },
    )
}
