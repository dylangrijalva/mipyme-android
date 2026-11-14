package co.mipyme.sync.internal

import arrow.core.Either
import co.mipyme.sync.SyncError

internal interface SyncRequester {
    suspend fun requestSync(): Either<SyncError, Unit>
}
