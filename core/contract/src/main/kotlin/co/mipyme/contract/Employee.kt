package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: String,
    val name: String,
    val isOwner: Boolean,
    val status: EmployeeStatus,
    val permissions: List<PosPermission>,
    val account: EmployeeAccount?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val maxDiscountRate: Decimal,
)

@Serializable
data class EmployeeAccount(
    val id: String,
    val email: String,
)

@Serializable
data class EmployeeSummary(
    val id: String,
    val name: String,
)
