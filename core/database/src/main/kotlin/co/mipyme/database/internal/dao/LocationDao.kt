package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LocationDao {
    @Upsert
    suspend fun upsertAll(items: List<LocationEntity>)

    @Query("SELECT * FROM locations ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("DELETE FROM locations")
    suspend fun deleteAll()
}
