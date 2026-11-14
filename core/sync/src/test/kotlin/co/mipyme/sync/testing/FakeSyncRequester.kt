package co.mipyme.sync.testing

import arrow.core.Either
import arrow.core.right
import co.mipyme.sync.SyncError
import co.mipyme.sync.internal.SyncRequester
import kotlinx.coroutines.CompletableDeferred

class FakeSyncRequester : SyncRequester {
    val results = ArrayDeque<Either<SyncError, Unit>>()
    var calls = 0
        private set
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun requestSync(): Either<SyncError, Unit> {
        calls++
        gate?.await()
        return results.removeFirstOrNull() ?: Unit.right()
    }
}
