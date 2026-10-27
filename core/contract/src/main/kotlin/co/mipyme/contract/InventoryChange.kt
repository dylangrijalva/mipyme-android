package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InventoryChange(
    val id: String,
    val variantId: String,
    val locationId: String,
    val referenceId: String? = null,
    val reason: InventoryMovementReason? = null,
    val fromState: InventoryState? = null,
    val toState: InventoryState? = null,
    val quantityDelta: Double? = null,
    val unitCost: Decimal? = null,
    val createdBy: EmployeeSummary,
    @Serializable(with = LocalDateTimeSerializer::class)
    val occurredAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val quantity: Double? = null,
    val type: InventoryChangeType,
)
