package co.mipyme.contract.request

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductType
import kotlinx.serialization.Serializable

@Serializable
data class CreateProductRequest(
    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val price: PositiveDecimal,
    val unitCost: Decimal? = null,
    val collectionId: ObjectId? = null,
    val productType: ProductType = ProductType.ITEM,
    val imageUrl: ProductImage? = null,
)
