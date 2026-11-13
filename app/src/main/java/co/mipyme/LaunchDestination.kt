package co.mipyme

sealed interface LaunchDestination {
    data object Idle : LaunchDestination

    data object Welcome : LaunchDestination

    data object Onboarding : LaunchDestination

    data object Home : LaunchDestination
}
