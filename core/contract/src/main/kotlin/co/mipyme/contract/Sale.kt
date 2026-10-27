package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Sale(
    val id: String,
    val source: Source,
    val status: SaleStatus,
    val transactionNumber: Double,
    val discountRate: Decimal?,
    val locationId: String,
    val notes: String?,
    val customer: SaleCustomer?,
    val lineItems: List<SaleLineItem>,
    val payments: List<SalePayment>,
    val createdBy: EmployeeSummary?,
    val voidedBy: EmployeeSummary?,
    val voidReason: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val voidedAt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val businessDate: LocalDateTime,
)

@Serializable
data class SaleLineItem(
    val id: String,
    val name: String,
    val variantId: String,
    val shortageQuantity: Int,
    val price: Decimal,
    val unitCost: Decimal,
    val quantity: Double,
)

@Serializable
data class SaleCustomer(
    val id: String,
    val name: String,
)

@Serializable
data class SalePayment(
    val id: String,
    val method: PaymentMethod,
    val amount: Decimal,
    val exchangeRate: Decimal?,
)
