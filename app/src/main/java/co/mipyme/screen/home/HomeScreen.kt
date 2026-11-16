package co.mipyme.screen.home

import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.checkout.CheckoutScreen
import co.mipyme.screen.customers.CustomersScreen
import co.mipyme.screen.expenses.ExpensesScreen
import co.mipyme.screen.inventory.InventoryScreen
import co.mipyme.screen.reports.ReportsScreen
import co.mipyme.screen.settings.SettingsScreen
import co.mipyme.screen.staff.StaffScreen
import co.mipyme.screen.suppliers.SuppliersScreen
import co.mipyme.screen.transactions.TransactionsScreen
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiModal
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiDivider
import co.mipyme.ui.component.MoshiNavigationDrawer
import co.mipyme.ui.component.MoshiNavigationDrawerItem
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.homeScreenModule(): Module {
    single {
        CurrentEmployeeRepository(
            authService = get(),
            cache = get(),
            scope = get(),
        )
    }
    viewModelOf(::HomeViewModel)
    navigation<Route.Home>(
        metadata = NavigationTransitions.forward,
    ) { HomeScreen() }
    return this
}

@Composable
internal fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = LocalNavBackStack.current
    val drawer = remember(drawerState, scope) {
        HomeDrawer { scope.launch { drawerState.open() } }
    }
    val lostAccessDestination = state.pendingLostAccessDestination
    val toastText = lostAccessDestination?.let {
        stringResource(R.string.home_access_lost, stringResource(it.titleRes()))
    }

    LaunchedEffect(lostAccessDestination) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onLostAccessToastShown()
        }
    }

    CompositionLocalProvider(LocalHomeDrawer provides drawer) {
        HomeContent(
            state = state,
            drawerState = drawerState,
            onDestinationSelected = { destination ->
                viewModel.onDestinationSelected(destination)
                scope.launch { drawerState.close() }
            },
            onHeaderClick = viewModel::onChangeBusinessClick,
            onChangeBusinessDismissed = viewModel::onChangeBusinessDismissed,
            onChangeBusinessConfirmed = {
                viewModel.onChangeBusinessDismissed()
                scope.launch {
                    drawerState.snapTo(DrawerValue.Closed)
                    if (backStack.lastOrNull() == Route.Home) {
                        backStack.add(Route.SelectLocation)
                    }
                }
            },
            onBack = viewModel::onBack,
            destinationContent = { destination -> HomeDestinationScreen(destination) },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeScreenUiState,
    drawerState: DrawerState,
    onDestinationSelected: (HomeDestination) -> Unit,
    onHeaderClick: () -> Unit,
    onChangeBusinessDismissed: () -> Unit,
    onChangeBusinessConfirmed: () -> Unit,
    onBack: () -> Unit,
    destinationContent: @Composable (HomeDestination) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val current = state.backStack.lastOrNull()

    MoshiNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = state.visibleDestinations.isNotEmpty(),
        header = {
            HomeDrawerHeader(
                locationName = state.locationName,
                email = state.email,
                onClick = onHeaderClick,
            )
            if (state.locationName != null || state.email != null) {
                MoshiDivider()
            }
        },
        items = {
            state.visibleDestinations.forEach { destination ->
                MoshiNavigationDrawerItem(
                    label = stringResource(destination.titleRes()),
                    icon = destination.iconRes(),
                    selected = destination == current,
                    onClick = { onDestinationSelected(destination) },
                    selectedIcon = destination.selectedIconRes(),
                )
            }
        },
        footer = { HomeDrawerFooter() },
    ) {
        if (state.backStack.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Moshi.colors.background100),
            )
        } else {
            NavDisplay(
                backStack = state.backStack,
                onBack = {
                    if (drawerState.isOpen || drawerState.targetValue == DrawerValue.Open) {
                        scope.launch { drawerState.close() }
                    } else {
                        onBack()
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                entryProvider = { destination ->
                    NavEntry(destination) { destinationContent(destination) }
                },
            )
        }
    }
    if (state.isChangeBusinessConfirmationVisible) {
        MoshiModal(
            title = stringResource(R.string.home_change_business_title),
            onDismissRequest = onChangeBusinessDismissed,
            primaryAction = MoshiModalAction(
                label = stringResource(R.string.home_change_business_continue),
                onClick = onChangeBusinessConfirmed,
            ),
            secondaryAction = MoshiModalAction(
                label = stringResource(R.string.home_change_business_cancel),
                onClick = onChangeBusinessDismissed,
            ),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_building_retail_24_regular),
                    contentDescription = null,
                    modifier = Modifier.size(Moshi.spacing.space12),
                    tint = Moshi.colors.gray1000,
                )
            },
        )
    }
}

@Composable
private fun HomeDrawerHeader(
    locationName: String?,
    email: String?,
    onClick: () -> Unit,
) {
    val spacing = Moshi.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_change_business_action),
                onClick = onClick,
            )
            .sizeIn(minHeight = spacing.space12)
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.space12)
                .background(Moshi.colors.blue900, RoundedCornerShape(spacing.space3)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fluent_building_retail_24_regular),
                contentDescription = null,
                modifier = Modifier.size(spacing.space6),
                tint = Moshi.colors.background100,
            )
        }
        Spacer(Modifier.width(spacing.space3))
        Column(modifier = Modifier.weight(1f)) {
            MoshiText(
                text = locationName ?: stringResource(R.string.home_change_business_action),
                role = MoshiTextRole.Heading20,
            )
            if (email != null) {
                Spacer(Modifier.height(spacing.space1))
                MoshiText(
                    text = email,
                    role = MoshiTextRole.Copy14,
                    color = Moshi.colors.gray900,
                )
            }
        }
        Spacer(Modifier.width(spacing.space2))
        Icon(
            painter = painterResource(R.drawable.ic_fluent_chevron_up_down_24_regular),
            contentDescription = null,
            modifier = Modifier.size(spacing.space6),
            tint = Moshi.colors.gray1000,
        )
    }
}

@Composable
private fun HomeDrawerFooter() {
    Image(
        painter = painterResource(R.drawable.logo_lockup_horizontal),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier
            .padding(Moshi.spacing.space6)
            .height(Moshi.spacing.space6),
    )
}

@Composable
private fun HomeDestinationScreen(destination: HomeDestination) {
    when (destination) {
        HomeDestination.CHECKOUT -> CheckoutScreen()
        HomeDestination.INVENTORY -> InventoryScreen()
        HomeDestination.SUPPLIERS -> SuppliersScreen()
        HomeDestination.CUSTOMERS -> CustomersScreen()
        HomeDestination.EXPENSES -> ExpensesScreen()
        HomeDestination.TRANSACTIONS -> TransactionsScreen()
        HomeDestination.REPORTS -> ReportsScreen()
        HomeDestination.STAFF -> StaffScreen()
        HomeDestination.SETTINGS -> SettingsScreen()
    }
}

@ScreenPreview
@Composable
fun HomeScreenPreview() {
    MoshiTheme {
        HomeContent(
            state = HomeScreenUiState(
                email = "ada@example.com",
                locationName = "Downtown store",
                visibleDestinations = HomeDestination.entries,
                backStack = listOf(HomeDestination.CHECKOUT),
            ),
            drawerState = rememberDrawerState(DrawerValue.Open),
            onDestinationSelected = {},
            onHeaderClick = {},
            onChangeBusinessDismissed = {},
            onChangeBusinessConfirmed = {},
            onBack = {},
            destinationContent = {},
        )
    }
}
