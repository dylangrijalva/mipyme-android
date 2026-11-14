package co.mipyme

import co.mipyme.navigation.Route

sealed interface LaunchDestination {
    data object Idle : LaunchDestination

    data object Welcome : LaunchDestination

    data object Onboarding : LaunchDestination

    data object AskNotificationPermissions : LaunchDestination

    data object Home : LaunchDestination
}

fun LaunchDestination.toRoute(): Route? {
    return when (this) {
        LaunchDestination.Idle -> null
        LaunchDestination.Welcome -> Route.Welcome
        LaunchDestination.Onboarding -> Route.Onboarding
        LaunchDestination.AskNotificationPermissions -> Route.AskNotificationPermissions
        LaunchDestination.Home -> Route.Home
    }
}
