package co.mipyme.contract

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val slug: String,
    val unitCost: Decimal,
    val price: Decimal,
    val productType: ProductType,
    val status: ProductStatus,
    val imageUrl: ProductImage?,
    val collection: ProductCollection?,
    val optionGroups: List<ProductOptionGroup>,
    val variants: List<ProductVariant>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val archivedAt: LocalDateTime?,
)

@Serializable
data class ProductCollection(
    val id: String,
    val name: String,
)

@Serializable
data class ProductOptionGroup(
    val id: String,
    val name: String,
    val options: List<ProductOption>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)

@Serializable
data class ProductOption(
    val id: String,
    val name: String,
    val position: Double,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
)
