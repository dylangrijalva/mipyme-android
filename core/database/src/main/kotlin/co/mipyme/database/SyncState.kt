package co.mipyme.database

import java.time.LocalDateTime

data class SyncState(
    val syncToken: String,
    val merchantId: String?,
    val lastSuccessfulSyncAt: LocalDateTime?,
)
