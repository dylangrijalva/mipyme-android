package co.mipyme.screen.product_editor

import android.net.Uri
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.ProductType
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

internal data class ProductEditorState(
    val productType: ProductType = ProductType.ITEM,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val canManageCatalog: Boolean = false,
    val isOffline: Boolean = false,
    val name: String = "",
    val barcode: String = "",
    val hasDefaultVariant: Boolean = true,
    val price: String = "",
    val unitCost: String = "",
    val requiresUnitCost: Boolean = productType == ProductType.ITEM,
    val unitCostReadOnly: Boolean = false,
    val selectedCollection: ProductEditorCollection? = null,
    val collections: List<ProductEditorCollection> = emptyList(),
    val photoUri: Uri? = null,
    val photoUrl: String? = null,
    val locations: List<ProductEditorLocation> = emptyList(),
    val country: CountryIsoCode? = null,
    val pricingDraft: ProductPricingDraft? = null,
    val collectionQuery: String = "",
    val isCreatingCollection: Boolean = false,
    val isSaving: Boolean = false,
    val error: ProductEditorError? = null,
    val collectionError: Boolean = false,
    val savedProductId: String? = null,
    val collectionSelectionApplied: Boolean = false,
    val isDirty: Boolean = false,
    val hasSaveChanges: Boolean = false,
) {
    val isService: Boolean get() = productType == ProductType.SERVICE
    val showsBarcode: Boolean get() = !isService && hasDefaultVariant

    val canEdit: Boolean
        get() = canManageCatalog && !isLoading && !isOffline && error != ProductEditorError.LOAD &&
            !isSaving && !isCreatingCollection && savedProductId == null

    val canSave: Boolean
        get() = canEdit && (!isEditing || hasSaveChanges) &&
            name.trim().length in 1..MAX_CATALOG_TEXT_LENGTH &&
            (!showsBarcode || barcode.trim().length <= MAX_CATALOG_TEXT_LENGTH) &&
            pricing.canApply

    val canRetry: Boolean
        get() = when (error) {
            ProductEditorError.LOAD -> !isLoading
            null, ProductEditorError.PHOTO, ProductEditorError.SCAN -> false
            else -> canSave
        }

    val priceError: ProductEditorFieldError? get() = pricing.priceError

    val isPricingDirty: Boolean
        get() = pricingDraft?.let {
            it.price.normalizedDecimal() != price.normalizedDecimal() ||
                it.unitCost.normalizedDecimal() != unitCost.normalizedDecimal()
        } ?: false

    val visibleCollections: List<ProductEditorCollection>
        get() = collections.filter { it.name.contains(collectionQuery.trim(), ignoreCase = true) }

    val canCreateCollection: Boolean
        get() {
            val name = collectionQuery.trim()
            return canEdit && name.length in 1..MAX_CATALOG_TEXT_LENGTH &&
                collections.none { it.name.equals(name, ignoreCase = true) }
        }

    internal val pricing: ProductPricingDraft
        get() = ProductPricingDraft(
            price = price,
            unitCost = unitCost,
            requiresUnitCost = requiresUnitCost,
            unitCostReadOnly = unitCostReadOnly,
        )
}

internal data class ProductEditorCollection(val id: String, val name: String) : Serializable

internal enum class ProductStockStatus { NOT_SET, OUT_OF_STOCK, LOW_STOCK, IN_STOCK }

internal data class ProductEditorLocation(
    val id: String,
    val name: String,
    val quantity: String = "",
    val lowStock: String = "",
    val optimalStock: String = "",
    val quantityReadOnly: Boolean = false,
) {
    val stockStatus: ProductStockStatus
        get() {
            val amount = quantity.trim().replace(',', '.').toBigDecimalOrNull()
                ?: return ProductStockStatus.NOT_SET
            val threshold = lowStock.nonNegativeDecimal()
            return when {
                amount.signum() <= 0 -> ProductStockStatus.OUT_OF_STOCK
                threshold != null && amount <= threshold -> ProductStockStatus.LOW_STOCK
                else -> ProductStockStatus.IN_STOCK
            }
        }

    val quantityError: ProductEditorFieldError?
        get() = if (quantityReadOnly) null else quantity.stockFieldError()

    val lowStockError: ProductEditorFieldError? get() = lowStock.stockFieldError()

    val optimalStockError: ProductEditorFieldError?
        get() {
            val error = optimalStock.stockFieldError()
            val low = lowStock.nonNegativeDecimal()
            val optimal = optimalStock.nonNegativeDecimal()
            if (error == null && low != null && optimal != null && optimal < low) {
                return ProductEditorFieldError.BELOW_LOW_STOCK
            }
            return error
        }

    val isValid: Boolean
        get() = quantityError == null && lowStockError == null && optimalStockError == null
}

internal data class ProductPricingDraft(
    val price: String = "",
    val unitCost: String = "",
    val requiresUnitCost: Boolean = false,
    val unitCostReadOnly: Boolean = false,
) : Serializable {
    private val sellingPrice: BigDecimal? get() = price.moneyAmount()?.takeIf { it.signum() > 0 }
    private val cost: BigDecimal? get() = unitCost.moneyAmount()?.takeIf { !requiresUnitCost || it.signum() > 0 }

    val priceError: ProductEditorFieldError?
        get() {
            val sellingPrice = sellingPrice
            val cost = cost
            return when {
                price.isNotBlank() && sellingPrice == null -> ProductEditorFieldError.INVALID
                sellingPrice != null && cost != null && sellingPrice < cost -> ProductEditorFieldError.BELOW_COST
                else -> null
            }
        }

    val unitCostError: ProductEditorFieldError?
        get() {
            return when {
                unitCost.isBlank() && requiresUnitCost -> ProductEditorFieldError.REQUIRED
                unitCost.isNotBlank() && cost == null -> ProductEditorFieldError.INVALID
                else -> null
            }
        }

    val canApply: Boolean get() = sellingPrice != null && priceError == null && unitCostError == null

    val grossProfit: BigDecimal?
        get() {
            val sellingPrice = sellingPrice ?: return null
            val cost = cost ?: return null
            return sellingPrice.subtract(cost)
        }

    val marginPercent: BigDecimal?
        get() {
            val sellingPrice = sellingPrice ?: return null
            val profit = grossProfit ?: return null
            return profit.movePointRight(2).divide(sellingPrice, 2, RoundingMode.HALF_UP)
        }
}

internal enum class ProductEditorError {
    LOAD,
    SAVE,
    SAVE_OFFLINE,
    SAVE_TIMEOUT,
    SAVE_ACCESS,
    BARCODE_ALREADY_EXISTS,
    PRODUCT_VALIDATION,
    PHOTO_SAVE,
    PHOTO,
    SCAN,
}

internal enum class ProductEditorFieldError { INVALID, REQUIRED, BELOW_COST, BELOW_LOW_STOCK }

internal const val MAX_CATALOG_TEXT_LENGTH = 128

private val decimalInputPattern = Regex("[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+")
private val maxMoneyAmount = BigDecimal("9999999999.99")

private fun String.nonNegativeDecimal(): BigDecimal? {
    val input = trim()
    if (!decimalInputPattern.matches(input)) return null
    return input.replace(',', '.').toBigDecimalOrNull()
}

internal fun String.moneyAmount(): BigDecimal? {
    return nonNegativeDecimal()?.takeIf { it.stripTrailingZeros().scale() <= 2 && it <= maxMoneyAmount }
}

internal fun String.normalizedDecimal(): String {
    return nonNegativeDecimal()?.stripTrailingZeros()?.toPlainString() ?: trim()
}

private fun String.stockFieldError(): ProductEditorFieldError? {
    return ProductEditorFieldError.INVALID.takeIf { isNotBlank() && nonNegativeDecimal() == null }
}
