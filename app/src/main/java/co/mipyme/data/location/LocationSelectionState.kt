package co.mipyme.data.location

import co.mipyme.contract.Location

data class LocationSelectionState(
    val userId: String? = null,
    val merchantId: String? = null,
    val accessibleLocations: List<Location> = emptyList(),
    val selectedLocation: Location? = null,
)
