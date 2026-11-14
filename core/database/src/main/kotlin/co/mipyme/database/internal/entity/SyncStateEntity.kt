package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.database.SyncState
import co.mipyme.database.internal.SINGLE_ROW_KEY
import java.time.LocalDateTime

@Entity(tableName = "sync_state")
internal data class SyncStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "row_key")
    val rowKey: Int = SINGLE_ROW_KEY,
    @ColumnInfo(name = "sync_token") val syncToken: String,
    @ColumnInfo(name = "merchant_id") val merchantId: String?,
    @ColumnInfo(name = "last_successful_sync_at") val lastSuccessfulSyncAt: LocalDateTime?,
)

internal fun SyncStateEntity.toContract(): SyncState {
    return SyncState(
        syncToken = syncToken,
        merchantId = merchantId,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
    )
}

