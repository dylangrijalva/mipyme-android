package co.mipyme.screen.profile_settings

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

fun Module.profileSettingsScreenModule(): Module {
    viewModelOf(::ProfileSettingsViewModel)
    navigation<Route.ProfileSettings>(
        metadata = NavigationTransitions.forwardWithBackwardPop,
    ) { ProfileSettingsScreen() }
    return this
}

@Composable
internal fun ProfileSettingsScreen(viewModel: ProfileSettingsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val backStack = LocalNavBackStack.current

    ProfileSettingsContent(
        state = state,
        onBack = {
            if (backStack.lastOrNull() == Route.ProfileSettings) {
                backStack.removeLastOrNull()
            }
        },
    )
}

@Composable
private fun ProfileSettingsContent(
    state: ProfileSettingsScreenUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background100),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.settings_profile),
            navigation = MoshiToolbarNavigation.Back(onClick = onBack),
        )
    }
}

@ScreenPreview
@Composable
fun ProfileSettingsScreenPreview() {
    MoshiTheme {
        ProfileSettingsContent(state = ProfileSettingsScreenUiState, onBack = {})
    }
}
