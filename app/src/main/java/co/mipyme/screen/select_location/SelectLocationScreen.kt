package co.mipyme.screen.select_location

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.extension.locationSelectionDataStore
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiMonogram
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarAction
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.selectLocationScreenModule(): Module {
    single {
        LocationSelectionRepository(
            dataStore = get<Context>().locationSelectionDataStore,
            authService = get(),
            cache = get(),
            scope = get<CoroutineScope>(),
        )
    }
    viewModelOf(::SelectLocationViewModel)
    navigation<Route.SelectLocation>(
        metadata = NavigationTransitions.forwardWithBackwardPop,
    ) { SelectLocationScreen() }
    return this
}

@Composable
internal fun SelectLocationScreen(viewModel: SelectLocationViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backStack = LocalNavBackStack.current
    LaunchedEffect(state.isSelectionComplete) {
        if (state.isSelectionComplete && backStack.lastOrNull() == Route.SelectLocation) {
            backStack.clear()
            backStack.add(Route.Home)
        }
    }
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    SelectLocationContent(
        state = state,
        onLocationClick = viewModel::onLocationClick,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirmed = viewModel::onSignOutConfirmed,
        onSignOutDismissed = viewModel::onSignOutDismissed,
    )
}

@Composable
private fun SelectLocationContent(
    state: SelectLocationScreenUiState,
    onLocationClick: (String) -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.surface),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.select_location_title),
            actions = listOf(
                MoshiToolbarAction(
                    icon = R.drawable.ic_fluent_sign_out_24_regular,
                    label = stringResource(R.string.select_location_sign_out),
                    onClick = onSignOutClick,
                ),
            ),
            compact = true,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            items(items = state.locations, key = { it.id }) { location ->
                LocationRow(
                    location = location,
                    onClick = { onLocationClick(location.id) },
                )
            }
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
private fun LocationRow(
    location: SelectLocationItem,
    onClick: () -> Unit,
) {
    val spacing = Moshi.spacing
    val address = location.address?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.select_location_no_address)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = spacing.space12)
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoshiMonogram(
            name = location.initials,
            colorSeed = location.id,
            modifier = Modifier.size(spacing.space12),
        )
        Spacer(Modifier.width(spacing.space4))
        Column(modifier = Modifier.weight(1f)) {
            MoshiText(
                text = location.name,
                role = MoshiTextRole.Copy16,
            )
            Spacer(Modifier.height(spacing.space1))
            MoshiText(
                text = address,
                role = MoshiTextRole.Copy14,
                color = Moshi.colors.foreground.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(spacing.space4))
        Icon(
            painter = painterResource(R.drawable.ic_fluent_chevron_right_24_regular),
            contentDescription = null,
            modifier = Modifier.size(spacing.space4),
            tint = Moshi.colors.foreground.secondary,
        )
    }
}

@Composable
private fun SignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoshiAlertDialog(
        title = stringResource(R.string.select_location_sign_out_confirm_title),
        message = stringResource(R.string.select_location_sign_out_confirm_message),
        onDismissRequest = onDismiss,
        confirmAction = MoshiModalAction(
            label = stringResource(R.string.select_location_sign_out_confirm_action),
            onClick = onConfirm,
        ),
        confirmActionVariant = MoshiButtonVariant.Destructive,
        cancelAction = MoshiModalAction(
            label = stringResource(R.string.select_location_sign_out_confirm_cancel),
            onClick = onDismiss,
        ),
    )
}

private val PreviewLocations = listOf(
    SelectLocationItem(id = "loc_1", name = "Downtown store", address = "Calle 10 #5-20, Medellín"),
    SelectLocationItem(id = "loc_2", name = "Airport kiosk", address = null),
    SelectLocationItem(
        id = "loc_3",
        name = "Warehouse and distribution center on the north side of town",
        address = "Carrera 50 #100-45, Bello, Antioquia, Colombia",
    ),
)

@ScreenPreview
@Composable
fun SelectLocationScreenPreview() {
    MoshiTheme {
        SelectLocationContent(
            state = SelectLocationScreenUiState(locations = PreviewLocations),
            onLocationClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun SelectLocationScreenSignOutConfirmationPreview() {
    MoshiTheme {
        SelectLocationContent(
            state = SelectLocationScreenUiState(
                locations = PreviewLocations,
                isSignOutConfirmationVisible = true,
            ),
            onLocationClick = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}
