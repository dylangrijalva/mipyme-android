package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PosPermission {
    @SerialName("checkout")
    CHECKOUT,

    @SerialName("view_sales_reports")
    VIEW_SALES_REPORTS,

    @SerialName("view_inventory_reports")
    VIEW_INVENTORY_REPORTS,

    @SerialName("view_transactions")
    VIEW_TRANSACTIONS,

    @SerialName("manage_catalog")
    MANAGE_CATALOG,

    @SerialName("manage_inventory")
    MANAGE_INVENTORY,

    @SerialName("manage_suppliers")
    MANAGE_SUPPLIERS,

    @SerialName("apply_discounts")
    APPLY_DISCOUNTS,

    @SerialName("void_transaction")
    VOID_TRANSACTION,

    @SerialName("refund")
    REFUND,
}
