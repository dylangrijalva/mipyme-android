package co.mipyme.sync.testing

import co.mipyme.contract.SyncResponse
import co.mipyme.database.SyncCacheWriter
import co.mipyme.database.SyncState
import java.time.LocalDateTime

class FakeSyncCacheWriter(var state: SyncState? = null) : SyncCacheWriter {
    class Applied(val response: SyncResponse, val merchantId: String, val syncedAt: LocalDateTime)

    val applied = mutableListOf<Applied>()
    var clearCalls = 0
        private set
    var applyFailure: Exception? = null
    var afterApply: (() -> Unit)? = null

    override suspend fun readSyncState(): SyncState? {
        return state
    }

    override suspend fun apply(
        response: SyncResponse,
        merchantId: String,
        syncedAt: LocalDateTime,
        isCurrent: suspend () -> Boolean,
    ): Boolean {
        applyFailure?.let { throw it }
        if (!isCurrent()) return false
        applied += Applied(response, merchantId, syncedAt)
        state = SyncState(response.syncToken, merchantId, syncedAt)
        afterApply?.invoke()
        return true
    }

    override suspend fun clearCacheUnlessMerchant(merchantId: String) {
        val cachedMerchantId = state?.merchantId
        if (cachedMerchantId != null && cachedMerchantId != merchantId) {
            clearCalls++
            state = null
        }
    }

    override suspend fun clearCache() {
        clearCalls++
        state = null
    }
}
