package co.mipyme.sync

sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    data class Failed(val reason: SyncError) : SyncStatus
}
