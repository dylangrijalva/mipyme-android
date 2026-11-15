package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import co.mipyme.contract.serializer.PosPermissionListSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SyncEmployee(
    val id: String,
    val name: String,
    val isOwner: Boolean,
    val status: EmployeeStatus,
    @Serializable(with = PosPermissionListSerializer::class)
    val permissions: List<PosPermission>,
    val account: EmployeeAccount?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val maxDiscountRate: Decimal,
    val locationIds: List<String>,
)
