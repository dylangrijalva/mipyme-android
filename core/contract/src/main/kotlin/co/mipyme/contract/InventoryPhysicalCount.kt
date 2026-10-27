package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InventoryPhysicalCount(
    val id: String,
    val variantId: String,
    val locationId: String,
    val quantity: Double,
    val createdBy: EmployeeSummary,
    @Serializable(with = LocalDateTimeSerializer::class)
    val occurredAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)
