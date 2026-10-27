package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RealtimeResourceType {
    @SerialName("collection")
    COLLECTION,

    @SerialName("customer")
    CUSTOMER,

    @SerialName("employee")
    EMPLOYEE,

    @SerialName("merchant")
    MERCHANT,

    @SerialName("sale")
    SALE,

    @SerialName("product")
    PRODUCT,

    @SerialName("purchase_order")
    PURCHASE_ORDER,

    @SerialName("location")
    LOCATION,

    @SerialName("supplier")
    SUPPLIER,
}
