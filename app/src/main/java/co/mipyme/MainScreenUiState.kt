package co.mipyme

sealed interface LaunchDestination {
    data object Idle : LaunchDestination

    data object Welcome : LaunchDestination

    data object Home : LaunchDestination
}

data class MainScreenUiState(
    val launchDestination: LaunchDestination = LaunchDestination.Idle
)