package co.mipyme.database.internal.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import co.mipyme.database.internal.entity.LocationEntity

internal data class EmployeeLocationRow(
    @ColumnInfo(name = "merchant_id") val merchantId: String?,
    @ColumnInfo(name = "is_owner") val isOwner: Boolean?,
    @Embedded val location: LocationEntity?,
)
