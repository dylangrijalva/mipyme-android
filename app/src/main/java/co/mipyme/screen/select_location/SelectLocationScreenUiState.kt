package co.mipyme.screen.select_location

import androidx.annotation.StringRes

data class SelectLocationScreenUiState(
    val locations: List<SelectLocationItem> = emptyList(),
    val isSelectionComplete: Boolean = false,
    val isSelecting: Boolean = false,
    val isSigningOut: Boolean = false,
    val isSignOutConfirmationVisible: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
