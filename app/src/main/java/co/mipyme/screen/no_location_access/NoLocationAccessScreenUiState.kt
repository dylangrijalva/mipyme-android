package co.mipyme.screen.no_location_access

import androidx.annotation.StringRes

data class NoLocationAccessScreenUiState(
    val isRefreshing: Boolean = false,
    val isSigningOut: Boolean = false,
    val isSignOutConfirmationVisible: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
