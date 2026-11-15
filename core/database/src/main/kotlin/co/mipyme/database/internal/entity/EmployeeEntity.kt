package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.Decimal
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.SyncEmployee
import java.time.LocalDateTime

@Entity(tableName = "employees")
internal data class EmployeeEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "is_owner") val isOwner: Boolean,
    val status: EmployeeStatus,
    val permissions: String,
    @ColumnInfo(name = "account_id") val accountId: String?,
    @ColumnInfo(name = "account_email") val accountEmail: String?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
    @ColumnInfo(name = "max_discount_rate") val maxDiscountRate: String,
)

private const val PERMISSION_SEPARATOR = ","

internal fun SyncEmployee.toEntity(): EmployeeEntity {
    return EmployeeEntity(
        id = id,
        name = name,
        isOwner = isOwner,
        status = status,
        permissions = permissions.joinToString(PERMISSION_SEPARATOR) { it.name },
        accountId = account?.id,
        accountEmail = account?.email,
        createdAt = createdAt,
        updatedAt = updatedAt,
        maxDiscountRate = maxDiscountRate.value,
    )
}
