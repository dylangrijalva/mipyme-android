package co.mipyme.screen.settings

import androidx.annotation.StringRes

data class SettingsScreenUiState(
    val visibleItems: List<SettingsItem> = listOf(SettingsItem.PROFILE),
    val isSignOutConfirmationVisible: Boolean = false,
    val isSigningOut: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
