package co.mipyme.testing

import co.mipyme.contract.BusinessType
import co.mipyme.contract.Decimal
import co.mipyme.contract.EmployeeAccount
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.Location
import co.mipyme.contract.PosPermission
import co.mipyme.contract.SyncEmployee
import java.time.LocalDateTime

private val FIXTURE_TIME = LocalDateTime.of(2026, 11, 14, 10, 30)

fun location(id: String, name: String = "Location $id", address: String? = "Main St 1"): Location {
    return Location(
        id = id,
        name = name,
        businessType = BusinessType.BAKERY,
        address = address,
        phone = null,
        taxId = null,
        taxRate = null,
        nextSaleNumber = 1,
        nextPurchaseOrderNumber = 1,
        allowBackorders = false,
        receiptHeaderText = null,
        receiptFooterText = null,
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
        exchangeRate = Decimal("1.00"),
    )
}

fun syncEmployee(
    id: String,
    locationIds: List<String>,
    status: EmployeeStatus = EmployeeStatus.ENABLED,
    isOwner: Boolean = false,
    permissions: List<PosPermission> = emptyList(),
): SyncEmployee {
    return SyncEmployee(
        id = id,
        name = "Employee $id",
        isOwner = isOwner,
        status = status,
        permissions = permissions,
        account = EmployeeAccount(id = "acc_$id", email = "$id@example.com"),
        createdAt = FIXTURE_TIME,
        updatedAt = FIXTURE_TIME,
        maxDiscountRate = Decimal("0.00"),
        locationIds = locationIds,
    )
}
