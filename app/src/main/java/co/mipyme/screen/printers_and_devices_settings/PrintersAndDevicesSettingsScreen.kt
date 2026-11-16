package co.mipyme.screen.printers_and_devices_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.printersAndDevicesSettingsScreenModule(): Module {
    viewModelOf(::PrintersAndDevicesSettingsViewModel)
    navigation<Route.PrintersAndDevicesSettings>(
        metadata = NavigationTransitions.forwardWithBackwardPop,
    ) { PrintersAndDevicesSettingsScreen() }
    return this
}

@Composable
internal fun PrintersAndDevicesSettingsScreen(viewModel: PrintersAndDevicesSettingsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val backStack = LocalNavBackStack.current

    PrintersAndDevicesSettingsContent(
        state = state,
        onBack = {
            if (backStack.lastOrNull() == Route.PrintersAndDevicesSettings) {
                backStack.removeLastOrNull()
            }
        },
    )
}

@Composable
private fun PrintersAndDevicesSettingsContent(
    state: PrintersAndDevicesSettingsScreenUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.settings_printers_and_devices),
            navigation = MoshiToolbarNavigation.Back(onClick = onBack),
        )
    }
}

@ScreenPreview
@Composable
fun PrintersAndDevicesSettingsScreenPreview() {
    MoshiTheme {
        PrintersAndDevicesSettingsContent(state = PrintersAndDevicesSettingsScreenUiState, onBack = {})
    }
}
