package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseOrder(
    val id: String,
    val transactionNumber: Double,
    val status: String,
    val locationId: String,
    val supplier: SupplierSummary,
    val lineItems: List<PurchaseOrderLineItem>,
    val notes: String?,
    val createdBy: EmployeeSummary?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)

@Serializable
data class PurchaseOrderLineItem(
    val productId: String,
    val productName: String,
    val unitCost: Decimal,
    val price: Decimal?,
    val variants: List<PurchaseVariant>,
)
