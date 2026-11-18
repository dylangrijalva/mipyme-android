package co.mipyme.screen.product_editor

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.Product
import co.mipyme.contract.ProductType
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.UpdateProductRequest
import java.io.Serializable

internal data class ProductEditorFields(
    val name: String,
    val barcode: String?,
    val price: String,
    val unitCost: String,
    val collectionId: String?,
    val photoUrl: String?,
) : Serializable {
    fun createRequest(productType: ProductType): CreateProductRequest {
        return CreateProductRequest(
            name = name,
            barcode = barcode,
            price = PositiveDecimal(price),
            unitCost = unitCost.takeIf { it.isNotEmpty() }?.let(::Decimal),
            collectionId = collectionId?.let(::ObjectId),
            productType = productType,
        )
    }

    fun updateRequest(saved: ProductEditorFields, clearPhoto: Boolean): UpdateProductRequest? {
        val request = UpdateProductRequest(
            name = name.whenChanged(saved.name),
            barcode = barcode.whenChanged(saved.barcode),
            price = PositiveDecimal(price).whenChanged(PositiveDecimal(saved.price)),
            unitCost = if (unitCost.isEmpty()) OptionalField.Absent else Decimal(unitCost).whenChanged(Decimal(saved.unitCost)),
            collectionId = collectionId?.let(::ObjectId).whenChanged(saved.collectionId?.let(::ObjectId)),
            imageUrl = if (clearPhoto || (photoUrl == null && saved.photoUrl != null)) {
                OptionalField.Present(null)
            } else {
                OptionalField.Absent
            },
        )
        return request.takeUnless { it == UpdateProductRequest() }
    }
}

internal fun ProductEditorState.savedFields(): ProductEditorFields {
    return ProductEditorFields(
        name = name.trim(),
        barcode = barcode.trim().takeIf { showsBarcode && it.isNotEmpty() },
        price = price.normalizedDecimal(),
        unitCost = if (isService) "" else unitCost.normalizedDecimal(),
        collectionId = selectedCollection?.id,
        photoUrl = photoUrl,
    )
}

internal fun Product.savedFields(): ProductEditorFields {
    val item = productType == ProductType.ITEM
    return ProductEditorFields(
        name = name.trim(),
        barcode = variants.singleOrNull()?.barcode?.trim()?.takeIf { item && it.isNotEmpty() },
        price = price.value.normalizedDecimal(),
        unitCost = if (item) unitCost.value.normalizedDecimal() else "",
        collectionId = collection?.id,
        photoUrl = imageUrl?.value,
    )
}

private fun <T> T.whenChanged(original: T): OptionalField<T> {
    return if (this == original) OptionalField.Absent else OptionalField.Present(this)
}
