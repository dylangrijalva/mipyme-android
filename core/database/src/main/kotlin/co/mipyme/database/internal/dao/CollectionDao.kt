package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CollectionDao {
    @Upsert
    suspend fun upsertAll(items: List<CollectionEntity>)

    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}
