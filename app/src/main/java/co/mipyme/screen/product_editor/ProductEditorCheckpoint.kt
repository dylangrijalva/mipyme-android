package co.mipyme.screen.product_editor

import android.net.Uri
import co.mipyme.data.product_editor.ProductPhotoUploadCheckpoint
import java.io.Serializable

internal data class ProductEditorCheckpoint(
    val name: String,
    val barcode: String,
    val price: String,
    val unitCost: String,
    val collection: ProductEditorCollection?,
    val photoUri: String?,
    val photoUrl: String?,
    val pricingDraft: ProductPricingDraft?,
    val inventory: Map<String, StockDraft>,
    val originalFields: ProductEditorFields,
    val savedProduct: SavedProduct?,
    val pendingProduct: ProductCreation?,
    val pendingUpdate: ProductUpdate?,
    val collectionQuery: String,
    val failedCollectionName: String?,
    val pendingCollection: CollectionCreation?,
    val collectionSelectionApplied: Boolean,
    val error: ProductEditorError?,
    val completedProductId: String?,
    val photoMayHaveBeenSaved: Boolean,
    val clearPhoto: Boolean,
    val photoUpload: ProductPhotoUploadCheckpoint?,
) : Serializable {
    fun restore(state: ProductEditorState): ProductEditorState {
        return state.copy(
            name = name,
            barcode = barcode,
            price = price,
            unitCost = unitCost,
            selectedCollection = collection,
            photoUri = photoUri?.let(Uri::parse),
            photoUrl = photoUrl,
            pricingDraft = pricingDraft,
            collectionQuery = collectionQuery,
            collectionError = failedCollectionName != null,
            collectionSelectionApplied = collectionSelectionApplied,
            error = error,
            savedProductId = completedProductId,
        )
    }
}

internal data class StockDraft(
    val quantity: String?,
    val lowStock: String?,
    val optimalStock: String?,
) : Serializable {
    fun merge(location: ProductEditorLocation): ProductEditorLocation {
        return location.copy(
            quantity = quantity?.takeUnless { location.quantityReadOnly } ?: location.quantity,
            lowStock = lowStock ?: location.lowStock,
            optimalStock = optimalStock ?: location.optimalStock,
        )
    }
}

// Compare retries to the last confirmed draft so unrelated server changes stay untouched.
internal data class SavedProduct(val id: String, val fields: ProductEditorFields) : Serializable
internal data class ProductCreation(val fields: ProductEditorFields, val key: String) : Serializable
internal data class ProductUpdate(val fields: ProductEditorFields, val clearPhoto: Boolean) : Serializable
internal data class CollectionCreation(val name: String, val key: String) : Serializable

internal fun String.checkpointText(): String {
    return take(MAX_CHECKPOINT_TEXT_LENGTH)
}

internal fun String.checkpointDecimal(): String {
    if (length <= MAX_CHECKPOINT_TEXT_LENGTH) return this
    return normalizedDecimal().checkpointText()
}

private const val MAX_CHECKPOINT_TEXT_LENGTH = 256
