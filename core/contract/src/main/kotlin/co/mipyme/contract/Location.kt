package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: String,
    val name: String,
    val businessType: BusinessType,
    val address: String?,
    val phone: String?,
    val taxId: String?,
    val taxRate: Decimal?,
    val nextSaleNumber: Int,
    val nextPurchaseOrderNumber: Int,
    val allowBackorders: Boolean,
    val receiptHeaderText: String?,
    val receiptFooterText: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val exchangeRate: Decimal,
)
