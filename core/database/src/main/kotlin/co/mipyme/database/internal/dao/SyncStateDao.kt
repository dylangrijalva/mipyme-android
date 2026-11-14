package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SyncStateDao {
    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT * FROM sync_state LIMIT 1")
    suspend fun find(): SyncStateEntity?

    @Query("SELECT * FROM sync_state LIMIT 1")
    fun observe(): Flow<SyncStateEntity?>

    @Query("DELETE FROM sync_state")
    suspend fun deleteAll()
}
