package co.mipyme.screen.select_location

import co.mipyme.contract.Location
import java.util.Locale

data class SelectLocationItem(
    val id: String,
    val name: String,
    val address: String?,
) {
    val initials: String
        get() {
            return name.trim().split(Regex("\\s+"))
                .filter(String::isNotBlank)
                .take(2)
                .joinToString("") { word -> word.substring(0, word.offsetByCodePoints(0, 1)) }
                .uppercase(Locale.ROOT)
        }
}

fun Location.toSelectLocationItem(): SelectLocationItem {
    return SelectLocationItem(id = id, name = name, address = address)
}
