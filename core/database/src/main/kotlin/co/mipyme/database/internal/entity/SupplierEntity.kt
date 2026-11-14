package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.Supplier
import java.time.LocalDateTime

@Entity(tableName = "suppliers")
internal data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

internal fun Supplier.toEntity(): SupplierEntity {
    return SupplierEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun SupplierEntity.toContract(): Supplier {
    return Supplier(
        id = id,
        name = name,
        email = email,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

