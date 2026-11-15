package co.mipyme.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.home.LocalHomeDrawer
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.settingsScreenModule(): Module {
    viewModelOf(::SettingsViewModel)
    return this
}

@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    SettingsContent(
        state = state,
        onSignOut = viewModel::onSignOut,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsScreenUiState,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_settings),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            MoshiButton(
                onClick = onSignOut,
                variant = MoshiButtonVariant.Destructive,
                isLoading = state.isSigningOut,
            ) {
                MoshiText(text = stringResource(R.string.settings_sign_out))
            }
        }
    }
}

@ScreenPreview
@Composable
fun SettingsScreenPreview() {
    MoshiTheme {
        SettingsContent(state = SettingsScreenUiState(), onSignOut = {})
    }
}

@ScreenPreview
@Composable
fun SettingsScreenSigningOutPreview() {
    MoshiTheme {
        SettingsContent(state = SettingsScreenUiState(isSigningOut = true), onSignOut = {})
    }
}
