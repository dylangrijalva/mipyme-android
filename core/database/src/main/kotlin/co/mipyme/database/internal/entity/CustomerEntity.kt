package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.Customer
import java.time.LocalDateTime

@Entity(tableName = "customers")
internal data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

internal fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun CustomerEntity.toContract(): Customer {
    return Customer(
        id = id,
        name = name,
        email = email,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

