package co.mipyme.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.BuildConfig
import co.mipyme.R
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.home.LocalHomeDrawer
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiListItem
import co.mipyme.ui.component.MoshiListItemChevron
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
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
    val backStack = LocalNavBackStack.current
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
        versionName = BuildConfig.VERSION_NAME,
        onItemClick = { item ->
            val route = item.route()
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        },
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirmed = viewModel::onSignOutConfirmed,
        onSignOutDismissed = viewModel::onSignOutDismissed,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsScreenUiState,
    versionName: String,
    onItemClick: (SettingsItem) -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_settings),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .navigationBarsPadding(),
            ) {
                state.visibleItems.forEach { item ->
                    MoshiListItem(
                        label = stringResource(item.titleRes()),
                        onClick = { onItemClick(item) },
                        leadingIcon = item.iconRes(),
                        trailing = { MoshiListItemChevron() },
                    )
                }
                MoshiListItem(
                    label = stringResource(R.string.settings_help),
                    onClick = {},
                    leadingIcon = R.drawable.ic_fluent_question_circle_24_regular,
                    trailing = { MoshiListItemChevron() },
                )
                MoshiListItem(
                    label = stringResource(R.string.settings_rate_us),
                    onClick = {},
                    leadingIcon = R.drawable.ic_fluent_star_24_regular,
                )
                MoshiListItem(
                    label = stringResource(R.string.settings_sign_out),
                    onClick = onSignOutClick,
                    leadingIcon = R.drawable.ic_fluent_sign_out_24_regular,
                    destructive = true,
                )
                Spacer(Modifier.height(spacing.space8))
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.pageGutterNarrow)
                        .padding(bottom = spacing.space6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    MoshiText(
                        text = stringResource(R.string.app_name),
                        role = MoshiTextRole.Small,
                        color = Moshi.colors.foreground.secondary,
                        textAlign = TextAlign.Center,
                    )
                    MoshiText(
                        text = stringResource(R.string.settings_app_version, versionName),
                        role = MoshiTextRole.Caption,
                        color = Moshi.colors.foreground.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (state.isSignOutConfirmationVisible) {
        MoshiAlertDialog(
            modifier = Modifier.padding(horizontal = spacing.space6),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = stringResource(R.string.settings_sign_out_confirm_title),
            message = stringResource(R.string.settings_sign_out_confirm_message),
            onDismissRequest = onSignOutDismissed,
            confirmAction = MoshiModalAction(
                label = stringResource(R.string.settings_sign_out_confirm_action),
                onClick = onSignOutConfirmed,
                isLoading = state.isSigningOut,
            ),
            confirmActionVariant = MoshiButtonVariant.Destructive,
            cancelAction = MoshiModalAction(
                label = stringResource(R.string.settings_sign_out_confirm_cancel),
                onClick = onSignOutDismissed,
            ),
        )
    }
}

@ScreenPreview
@Composable
fun SettingsScreenPreview() {
    MoshiTheme {
        SettingsContent(
            state = SettingsScreenUiState(visibleItems = SettingsItem.entries),
            versionName = "1.0",
            onItemClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun SettingsScreenNonOwnerPreview() {
    MoshiTheme {
        SettingsContent(
            state = SettingsScreenUiState(),
            versionName = "1.0",
            onItemClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun SettingsScreenSignOutConfirmationPreview() {
    MoshiTheme {
        SettingsContent(
            state = SettingsScreenUiState(
                visibleItems = SettingsItem.entries,
                isSignOutConfirmationVisible = true,
            ),
            versionName = "1.0",
            onItemClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun SettingsScreenSigningOutPreview() {
    MoshiTheme {
        SettingsContent(
            state = SettingsScreenUiState(
                visibleItems = SettingsItem.entries,
                isSignOutConfirmationVisible = true,
                isSigningOut = true,
            ),
            versionName = "1.0",
            onItemClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}
