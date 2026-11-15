package co.mipyme.screen.select_location

import co.mipyme.contract.Location

data class SelectLocationItem(
    val id: String,
    val name: String,
    val address: String?,
)

fun Location.toSelectLocationItem(): SelectLocationItem {
    return SelectLocationItem(id = id, name = name, address = address)
}
