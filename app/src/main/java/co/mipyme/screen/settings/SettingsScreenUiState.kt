package co.mipyme.screen.settings

import androidx.annotation.StringRes

data class SettingsScreenUiState(
    val isSigningOut: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
