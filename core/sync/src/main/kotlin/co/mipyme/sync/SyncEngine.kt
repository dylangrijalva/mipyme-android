package co.mipyme.sync

import arrow.core.Either
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncEngine {
    val syncStatusFlow: StateFlow<SyncStatus>

    fun lastSuccessfulSyncAtFlow(): Flow<LocalDateTime?>

    fun initialSyncCompletedFlow(merchantId: String): Flow<Boolean>

    suspend fun synchronize(): Either<SyncError, Unit>
}
