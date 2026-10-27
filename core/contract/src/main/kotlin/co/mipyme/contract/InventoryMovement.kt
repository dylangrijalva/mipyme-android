package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InventoryMovement(
    val id: String,
    val variantId: String,
    val locationId: String,
    val referenceId: String?,
    val reason: InventoryMovementReason,
    val fromState: InventoryState,
    val toState: InventoryState,
    val quantityDelta: Double,
    val unitCost: Decimal,
    val createdBy: EmployeeSummary,
    @Serializable(with = LocalDateTimeSerializer::class)
    val occurredAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)
