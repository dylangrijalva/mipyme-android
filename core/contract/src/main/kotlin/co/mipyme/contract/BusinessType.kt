package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BusinessType {
    @SerialName("convenience_store")
    CONVENIENCE_STORE,

    @SerialName("restaurant")
    RESTAURANT,

    @SerialName("coffee_shop")
    COFFEE_SHOP,

    @SerialName("clothing_store")
    CLOTHING_STORE,

    @SerialName("electronics_store")
    ELECTRONICS_STORE,

    @SerialName("grocery_store")
    GROCERY_STORE,

    @SerialName("pharmacy")
    PHARMACY,

    @SerialName("hardware_store")
    HARDWARE_STORE,

    @SerialName("beauty_salon")
    BEAUTY_SALON,

    @SerialName("bar")
    BAR,

    @SerialName("autoparts_store")
    AUTOPARTS_STORE,

    @SerialName("sports_store")
    SPORTS_STORE,

    @SerialName("bookstore")
    BOOKSTORE,

    @SerialName("jewelry_store")
    JEWELRY_STORE,

    @SerialName("shoe_store")
    SHOE_STORE,

    @SerialName("stationery_store")
    STATIONERY_STORE,

    @SerialName("bakery")
    BAKERY,

    @SerialName("other")
    OTHER,
}
