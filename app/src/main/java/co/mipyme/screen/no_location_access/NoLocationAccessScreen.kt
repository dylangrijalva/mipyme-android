package co.mipyme.screen.no_location_access

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiLinearProgressIndicator
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarAction
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.noLocationAccessScreenModule(): Module {
    viewModelOf(::NoLocationAccessViewModel)
    navigation<Route.NoLocationAccess>(
        metadata = NavDisplay.transitionSpec {
            fadeIn() togetherWith fadeOut()
        },
    ) { NoLocationAccessScreen() }
    return this
}

@Composable
internal fun NoLocationAccessScreen(viewModel: NoLocationAccessViewModel = koinViewModel()) {
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

    NoLocationAccessContent(
        state = state,
        onRefresh = viewModel::onRefresh,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirmed = viewModel::onSignOutConfirmed,
        onSignOutDismissed = viewModel::onSignOutDismissed,
    )
}

@Composable
private fun NoLocationAccessContent(
    state: NoLocationAccessScreenUiState,
    onRefresh: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.canvas),
    ) {
        MoshiToolbar(
            actions = listOf(
                MoshiToolbarAction(
                    icon = R.drawable.ic_fluent_arrow_sync_24_regular,
                    label = stringResource(R.string.no_location_access_refresh),
                    onClick = onRefresh,
                ),
                MoshiToolbarAction(
                    icon = R.drawable.ic_fluent_sign_out_24_regular,
                    label = stringResource(R.string.no_location_access_sign_out),
                    onClick = onSignOutClick,
                ),
            ),
            compact = true,
        )
        if (state.isRefreshing) {
            MoshiLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space6),
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.space16)
                    .background(colors.background.status.error, CircleShape)
                    .border(width = 1.dp, color = colors.border.status.error, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_dismiss_24_regular),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.space8),
                    tint = colors.foreground.status.error,
                )
            }
            Spacer(Modifier.height(spacing.space6))
            MoshiText(
                text = stringResource(R.string.no_location_access_title),
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Heading,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = stringResource(R.string.no_location_access_body),
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Body,
                color = colors.foreground.secondary,
            )
        }
    }

    if (state.isSignOutConfirmationVisible) {
        SignOutConfirmationDialog(
            onConfirm = onSignOutConfirmed,
            onDismiss = onSignOutDismissed,
        )
    }
}

@Composable
private fun SignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoshiAlertDialog(
        modifier = Modifier.padding(horizontal = Moshi.spacing.space6),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = stringResource(R.string.no_location_access_sign_out_confirm_title),
        message = stringResource(R.string.no_location_access_sign_out_confirm_message),
        onDismissRequest = onDismiss,
        confirmAction = MoshiModalAction(
            label = stringResource(R.string.no_location_access_sign_out_confirm_action),
            onClick = onConfirm,
        ),
        confirmActionVariant = MoshiButtonVariant.Destructive,
        cancelAction = MoshiModalAction(
            label = stringResource(R.string.no_location_access_sign_out_confirm_cancel),
            onClick = onDismiss,
        ),
    )
}

@ScreenPreview
@Composable
fun NoLocationAccessScreenPreview() {
    MoshiTheme {
        NoLocationAccessContent(
            state = NoLocationAccessScreenUiState(),
            onRefresh = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun NoLocationAccessScreenRefreshingPreview() {
    MoshiTheme {
        NoLocationAccessContent(
            state = NoLocationAccessScreenUiState(isRefreshing = true),
            onRefresh = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun NoLocationAccessScreenSignOutConfirmationPreview() {
    MoshiTheme {
        NoLocationAccessContent(
            state = NoLocationAccessScreenUiState(isSignOutConfirmationVisible = true),
            onRefresh = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}
