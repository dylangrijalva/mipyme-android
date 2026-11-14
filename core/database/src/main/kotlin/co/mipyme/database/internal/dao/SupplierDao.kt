package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SupplierDao {
    @Upsert
    suspend fun upsertAll(items: List<SupplierEntity>)

    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}
