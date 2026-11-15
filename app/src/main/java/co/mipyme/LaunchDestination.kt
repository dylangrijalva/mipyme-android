package co.mipyme

import co.mipyme.navigation.Route

sealed interface LaunchDestination {
    data object Idle : LaunchDestination

    data object Welcome : LaunchDestination

    data object Onboarding : LaunchDestination

    data object InitialSync : LaunchDestination

    data object AskNotificationPermissions : LaunchDestination

    data object SelectLocation : LaunchDestination

    data object NoLocationAccess : LaunchDestination

    data object Home : LaunchDestination
}

fun LaunchDestination.toRoute(): Route? {
    return when (this) {
        LaunchDestination.Idle -> null
        LaunchDestination.Welcome -> Route.Welcome
        LaunchDestination.Onboarding -> Route.Onboarding
        LaunchDestination.InitialSync -> Route.InitialSync
        LaunchDestination.AskNotificationPermissions -> Route.AskNotificationPermissions
        LaunchDestination.SelectLocation -> Route.SelectLocation
        LaunchDestination.NoLocationAccess -> Route.NoLocationAccess
        LaunchDestination.Home -> Route.Home
    }
}
