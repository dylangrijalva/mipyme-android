package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CustomerDao {
    @Upsert
    suspend fun upsertAll(items: List<CustomerEntity>)

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
