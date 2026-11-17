package co.mipyme.screen.business_information_settings

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

fun Module.businessInformationSettingsScreenModule(): Module {
    viewModelOf(::BusinessInformationSettingsViewModel)
    navigation<Route.BusinessInformationSettings>(
        metadata = NavigationTransitions.forwardWithBackwardPop,
    ) { BusinessInformationSettingsScreen() }
    return this
}

@Composable
internal fun BusinessInformationSettingsScreen(viewModel: BusinessInformationSettingsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val backStack = LocalNavBackStack.current

    BusinessInformationSettingsContent(
        state = state,
        onBack = {
            if (backStack.lastOrNull() == Route.BusinessInformationSettings) {
                backStack.removeLastOrNull()
            }
        },
    )
}

@Composable
private fun BusinessInformationSettingsContent(
    state: BusinessInformationSettingsScreenUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.surface),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.settings_business_information),
            navigation = MoshiToolbarNavigation.Back(onClick = onBack),
        )
    }
}

@ScreenPreview
@Composable
fun BusinessInformationSettingsScreenPreview() {
    MoshiTheme {
        BusinessInformationSettingsContent(state = BusinessInformationSettingsScreenUiState, onBack = {})
    }
}
