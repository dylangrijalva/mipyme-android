package co.mipyme.sync.internal

import co.mipyme.contract.MipymeError
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.network.NetworkError
import co.mipyme.sync.SyncError

internal fun NetworkError.toSyncError(): SyncError {
    return when (this) {
        is NetworkError.NetworkUnavailable,
        is NetworkError.Timeout -> SyncError.Network

        is NetworkError.Response -> error.toSyncError()

        is NetworkError.Serialization,
        is NetworkError.Unexpected -> SyncError.Unknown
    }
}

internal fun NetworkError.isSyncTokenInvalid(): Boolean {
    return this is NetworkError.Response && error.code == MipymeErrorCode.SYNC_TOKEN_INVALID
}

private fun MipymeError.toSyncError(): SyncError {
    return when {
        status == 401 -> SyncError.Unauthorized
        status == 403 -> SyncError.Forbidden
        status == 429 ||
            status >= 500 ||
            code == MipymeErrorCode.SYNC_UNAVAILABLE -> SyncError.Server

        else -> SyncError.Unknown
    }
}
