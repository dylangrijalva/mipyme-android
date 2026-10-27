package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Purchase(
    val id: String,
    val transactionNumber: Double,
    val status: PurchaseStatus,
    val locationId: String,
    val supplier: SupplierSummary,
    val lineItems: List<PurchaseLineItem>,
    val notes: String?,
    val createdBy: EmployeeSummary?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val voidReason: String?,
    val voidedBy: EmployeeSummary?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val voidedAt: LocalDateTime?,
)

@Serializable
data class PurchaseLineItem(
    val id: String,
    val unitCost: Decimal,
    val price: Decimal?,
    val variants: List<PurchaseVariant>,
)

@Serializable
data class PurchaseVariant(
    val id: String,
    val name: String,
    val quantity: Double,
)

@Serializable
data class SupplierSummary(
    val id: String,
    val name: String,
)
