package co.mipyme.screen.initial_sync

import androidx.annotation.StringRes

data class InitialSyncScreenUiState(
    val isSyncing: Boolean = true,
    @param:StringRes val errorRes: Int? = null,
    val isSigningOut: Boolean = false,
    val isSignOutConfirmationVisible: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
)
