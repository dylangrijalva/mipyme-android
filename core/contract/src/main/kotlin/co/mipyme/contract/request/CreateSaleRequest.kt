package co.mipyme.contract.request

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.PaymentMethod
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.Source
import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class CreateSaleRequest(
    val source: Source = Source.POS_WEB,
    val locationId: ObjectId,
    @Serializable(with = LocalDateTimeSerializer::class)
    val businessDate: LocalDateTime,
    val customerId: ObjectId? = null,
    val notes: String? = null,
    val discountRate: Decimal? = null,
    val lineItems: List<CreateSaleLineItem>,
    val payments: List<CreateSalePayment>,
)

@Serializable
data class CreateSaleLineItem(
    val variantId: ObjectId,
    val price: Decimal,
    val quantity: Int,
)

@Serializable
data class CreateSalePayment(
    val method: PaymentMethod,
    val amount: PositiveDecimal,
)
