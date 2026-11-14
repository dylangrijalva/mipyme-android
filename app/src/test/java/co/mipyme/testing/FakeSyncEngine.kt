package co.mipyme.testing

import arrow.core.Either
import arrow.core.right
import co.mipyme.sync.SyncEngine
import co.mipyme.sync.SyncError
import co.mipyme.sync.SyncStatus
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeSyncEngine(
    completedMerchants: Set<String> = emptySet(),
    var result: Either<SyncError, Unit> = Unit.right(),
    var gate: CompletableDeferred<Unit>? = null,
) : SyncEngine {
    val completed = MutableStateFlow(completedMerchants)
    var synchronizeCalls = 0
        private set

    override val syncStatusFlow: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun lastSuccessfulSyncAtFlow(): Flow<LocalDateTime?> = MutableStateFlow(null)

    override fun initialSyncCompletedFlow(merchantId: String): Flow<Boolean> {
        return completed.map { merchantId in it }
    }

    override suspend fun synchronize(): Either<SyncError, Unit> {
        synchronizeCalls++
        gate?.await()
        return result
    }
}
