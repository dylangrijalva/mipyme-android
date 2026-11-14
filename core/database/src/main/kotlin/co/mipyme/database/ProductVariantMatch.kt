package co.mipyme.database

import co.mipyme.contract.Product
import co.mipyme.contract.ProductVariant

data class ProductVariantMatch(
    val product: Product,
    val variant: ProductVariant,
)
