package co.mipyme.screen.settings

import androidx.annotation.StringRes
import co.mipyme.data.theme.ThemeMode

data class SettingsScreenUiState(
    val visibleItems: List<SettingsItem> = listOf(SettingsItem.PROFILE),
    val canSwitchLocation: Boolean = false,
    val isSwitchingLocation: Boolean = false,
    val isSignOutConfirmationVisible: Boolean = false,
    val isSigningOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isThemeSheetVisible: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
