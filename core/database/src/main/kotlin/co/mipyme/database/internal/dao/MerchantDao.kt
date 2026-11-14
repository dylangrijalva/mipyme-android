package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.database.internal.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MerchantDao {
    @Upsert
    suspend fun upsert(merchant: MerchantEntity)

    @Query("SELECT * FROM merchant LIMIT 1")
    fun observe(): Flow<MerchantEntity?>

    @Query("DELETE FROM merchant")
    suspend fun deleteAll()
}
