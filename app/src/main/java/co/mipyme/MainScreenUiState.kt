package co.mipyme

import co.mipyme.data.theme.ThemeMode

data class MainScreenUiState(
    val launchDestination: LaunchDestination = LaunchDestination.Idle,
    val themeMode: ThemeMode? = null,
)
