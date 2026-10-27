package co.mipyme.contract.request

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.ProductImage
import co.mipyme.contract.serializer.OptionalFieldSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    @Serializable(with = OptionalFieldSerializer::class)
    val name: OptionalField<String> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val barcode: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val sku: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val price: OptionalField<PositiveDecimal> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val unitCost: OptionalField<Decimal> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val collectionId: OptionalField<ObjectId?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val imageUrl: OptionalField<ProductImage?> = OptionalField.Absent,
)
