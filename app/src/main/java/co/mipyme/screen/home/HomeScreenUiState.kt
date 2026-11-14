package co.mipyme.screen.home

import androidx.annotation.StringRes

data class HomeScreenUiState(
    val email: String? = null,
    val isSigningOut: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
