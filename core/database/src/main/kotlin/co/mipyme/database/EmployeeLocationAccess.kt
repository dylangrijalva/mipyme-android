package co.mipyme.database

import co.mipyme.contract.Location

data class EmployeeLocationAccess(
    val merchantId: String,
    val isOwner: Boolean,
    val locations: List<Location>,
)
