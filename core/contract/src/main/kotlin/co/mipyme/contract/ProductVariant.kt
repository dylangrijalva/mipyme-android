package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ProductVariant(
    val id: String,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val price: Decimal?,
    val options: List<ProductOption>,
    val inventoryLevels: List<InventoryLevel>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)
