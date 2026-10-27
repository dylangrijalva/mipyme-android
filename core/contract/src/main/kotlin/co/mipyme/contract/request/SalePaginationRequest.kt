package co.mipyme.contract.request

import co.mipyme.contract.ObjectId
import co.mipyme.contract.SaleStatus
import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SalePaginationRequest(
    val limit: Int = 50,
    val cursor: String? = null,
    val locationId: ObjectId? = null,
    val customerId: ObjectId? = null,
    val status: SaleStatus? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val from: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val to: LocalDateTime? = null,
)
