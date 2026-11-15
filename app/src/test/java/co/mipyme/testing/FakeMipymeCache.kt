package co.mipyme.testing

import co.mipyme.contract.Collection
import co.mipyme.contract.Customer
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.Location
import co.mipyme.contract.Merchant
import co.mipyme.contract.Product
import co.mipyme.contract.Supplier
import co.mipyme.contract.SyncEmployee
import co.mipyme.database.EmployeeLocationAccess
import co.mipyme.database.MipymeCache
import co.mipyme.database.ProductVariantMatch
import co.mipyme.database.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMipymeCache(
    locations: List<Location> = emptyList(),
    employees: List<SyncEmployee> = emptyList(),
    merchantId: String? = "merch_1",
) : MipymeCache {
    private val snapshot = MutableStateFlow(Snapshot(locations, employees, merchantId))

    var locations: List<Location>
        get() = snapshot.value.locations
        set(value) {
            snapshot.update { it.copy(locations = value) }
        }

    var employees: List<SyncEmployee>
        get() = snapshot.value.employees
        set(value) {
            snapshot.update { it.copy(employees = value) }
        }

    var merchantId: String?
        get() = snapshot.value.merchantId
        set(value) {
            snapshot.update { it.copy(merchantId = value) }
        }

    override fun observeLocations(): Flow<List<Location>> = snapshot.map { it.locations }

    override fun observeLocationAccess(employeeId: String): Flow<EmployeeLocationAccess?> {
        return snapshot.map { current ->
            val merchantId = current.merchantId ?: return@map null
            val employee = current.employees.firstOrNull { it.id == employeeId && (it.isOwner || it.status == EmployeeStatus.ENABLED) }
            when {
                employee == null -> EmployeeLocationAccess(merchantId, false, emptyList())
                employee.isOwner -> EmployeeLocationAccess(merchantId, true, current.locations)
                else -> EmployeeLocationAccess(
                    merchantId,
                    false,
                    current.locations.filter { it.id in employee.locationIds },
                )
            }
        }
    }

    override fun observeMerchant(): Flow<Merchant?> = emptyFlow()

    override fun observeProducts(): Flow<List<Product>> = emptyFlow()

    override suspend fun findVariantByBarcode(barcode: String): ProductVariantMatch? = null

    override fun observeEmployee(employeeId: String): Flow<Employee?> {
        return snapshot.map { current -> current.employees.firstOrNull { it.id == employeeId }?.toEmployee() }
    }

    override fun observeCustomers(): Flow<List<Customer>> = emptyFlow()

    override fun observeSuppliers(): Flow<List<Supplier>> = emptyFlow()

    override fun observeCollections(): Flow<List<Collection>> = emptyFlow()

    override fun observeSyncState(): Flow<SyncState?> = emptyFlow()

    private fun SyncEmployee.toEmployee(): Employee {
        return Employee(
            id = id,
            name = name,
            isOwner = isOwner,
            status = status,
            permissions = permissions,
            account = account,
            createdAt = createdAt,
            updatedAt = updatedAt,
            maxDiscountRate = maxDiscountRate,
        )
    }

    private data class Snapshot(
        val locations: List<Location>,
        val employees: List<SyncEmployee>,
        val merchantId: String?,
    )
}
