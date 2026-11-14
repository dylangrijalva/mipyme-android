package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.Merchant
import co.mipyme.database.internal.SINGLE_ROW_KEY
import java.time.LocalDateTime

@Entity(tableName = "merchant")
internal data class MerchantEntity(
    @PrimaryKey
    @ColumnInfo(name = "row_key")
    val rowKey: Int = SINGLE_ROW_KEY,
    val id: String,
    val name: String,
    val country: CountryIsoCode,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

internal fun Merchant.toEntity(): MerchantEntity {
    return MerchantEntity(
        id = id,
        name = name,
        country = country,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun MerchantEntity.toContract(): Merchant {
    return Merchant(
        id = id,
        name = name,
        country = country,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

