package co.mipyme.database

import co.mipyme.contract.SyncResponse
import java.time.LocalDateTime

interface SyncCacheWriter {
    suspend fun readSyncState(): SyncState?

    suspend fun apply(
        response: SyncResponse,
        merchantId: String,
        syncedAt: LocalDateTime,
        isCurrent: suspend () -> Boolean,
    ): Boolean

    suspend fun clearCache()

    suspend fun clearCacheUnlessMerchant(merchantId: String)
}
