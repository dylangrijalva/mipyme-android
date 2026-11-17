package co.mipyme.screen.ask_notification_permissions

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.data.notification.AndroidNotificationPermissionChecker
import co.mipyme.data.notification.NotificationPermissionChecker
import co.mipyme.data.notification.NotificationPermissionRepository
import co.mipyme.extension.notificationPermissionDataStore
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.askNotificationPermissionsScreenModule(): Module {
    single<NotificationPermissionChecker> { AndroidNotificationPermissionChecker(androidContext()) }
    single {
        NotificationPermissionRepository(
            dataStore = get<Context>().notificationPermissionDataStore,
            permissionChecker = get(),
            authService = get(),
            scope = get<CoroutineScope>(),
            ioDispatcher = Dispatchers.IO,
        )
    }
    viewModelOf(::AskNotificationPermissionsViewModel)
    navigation<Route.AskNotificationPermissions>(
        metadata = NavigationTransitions.forward,
    ) { AskNotificationPermissionsScreen() }
    return this
}

@Composable
internal fun AskNotificationPermissionsScreen(
    viewModel: AskNotificationPermissionsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionResult()
    }

    BackHandler(onBack = viewModel::onSkip)

    AskNotificationPermissionsContent(
        state = state,
        onTurnOn = {
            if (viewModel.onRequestPermission()) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSkip = viewModel::onSkip,
    )
}

@Composable
private fun AskNotificationPermissionsContent(
    state: AskNotificationPermissionsScreenUiState,
    onTurnOn: () -> Unit,
    onSkip: () -> Unit,
) {
    val spacing = Moshi.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.surface)
            .safeDrawingPadding()
            .padding(horizontal = spacing.pageGutterNarrow),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fluent_alert_24_regular),
                contentDescription = null,
                modifier = Modifier.size(spacing.space12),
                tint = Moshi.colors.foreground.accent,
            )
            Spacer(Modifier.height(spacing.space6))
            MoshiText(
                text = stringResource(R.string.ask_notification_permissions_title),
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Heading24,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = stringResource(R.string.ask_notification_permissions_body),
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Copy16,
                textAlign = TextAlign.Center,
                color = Moshi.colors.foreground.secondary,
            )
        }

        Spacer(Modifier.height(spacing.space4))
        MoshiButton(
            onClick = onTurnOn,
            modifier = Modifier.fillMaxWidth(),
            variant = MoshiButtonVariant.Primary,
            enabled = !state.isResponding,
        ) {
            MoshiText(text = stringResource(R.string.ask_notification_permissions_turn_on))
        }
        Spacer(Modifier.height(spacing.space3))
        MoshiButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            variant = MoshiButtonVariant.Secondary,
            enabled = !state.isResponding,
        ) {
            MoshiText(text = stringResource(R.string.ask_notification_permissions_skip))
        }
        Spacer(Modifier.height(spacing.space4))
    }
}

@ScreenPreview
@Composable
fun AskNotificationPermissionsScreenPreview() {
    MoshiTheme {
        AskNotificationPermissionsContent(
            state = AskNotificationPermissionsScreenUiState(),
            onTurnOn = {},
            onSkip = {},
        )
    }
}
