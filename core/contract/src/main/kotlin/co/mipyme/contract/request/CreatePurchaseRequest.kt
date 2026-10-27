package co.mipyme.contract.request

import co.mipyme.contract.ObjectId
import co.mipyme.contract.PositiveDecimal
import kotlinx.serialization.Serializable

@Serializable
data class CreatePurchaseRequest(
    val supplierId: ObjectId,
    val locationId: ObjectId,
    val notes: String? = null,
    val lineItems: List<CreatePurchaseLineItem>,
)

@Serializable
data class CreatePurchaseLineItem(
    val variantId: ObjectId,
    val quantity: Int,
    val unitCost: PositiveDecimal,
)
