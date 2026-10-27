package co.mipyme.contract.request

import co.mipyme.contract.BusinessType
import co.mipyme.contract.Decimal
import kotlinx.serialization.Serializable

@Serializable
data class CreateLocationRequest(
    val name: String,
    val businessType: BusinessType,
    val phone: String? = null,
    val address: String? = null,
    val taxId: String? = null,
    val receiptHeaderText: String? = null,
    val receiptFooterText: String? = null,
    val exchangeRate: Decimal? = null,
    val taxRate: Decimal? = null,
    val allowBackorders: Boolean? = null,
)
