package co.mipyme.sync.internal

import co.mipyme.contract.MipymeError
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.network.NetworkError
import co.mipyme.sync.SyncError

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500

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
        status == HTTP_UNAUTHORIZED -> SyncError.Unauthorized
        status == HTTP_FORBIDDEN -> SyncError.Forbidden
        status == HTTP_TOO_MANY_REQUESTS ||
            status >= HTTP_SERVER_ERROR ||
            code == MipymeErrorCode.SYNC_UNAVAILABLE -> SyncError.Server

        else -> SyncError.Unknown
    }
}
