package co.mipyme.sync

sealed interface SyncError {
    data object Network : SyncError

    data object Server : SyncError

    data object Unauthorized : SyncError

    data object Forbidden : SyncError

    data object Unknown : SyncError
}
