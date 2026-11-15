package co.mipyme.screen.home

import co.mipyme.contract.Decimal
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.PosPermission
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDestinationTest {

    private val settingsOnly = listOf(HomeDestination.SETTINGS)

    @Test
    fun `no employee only shows settings`() {
        assertEquals(settingsOnly, (null as Employee?).visibleHomeDestinations())
    }

    @Test
    fun `a non owner that is not enabled only shows settings whatever their permissions`() {
        listOf(EmployeeStatus.INVITED, EmployeeStatus.DISABLED).forEach { status ->
            val employee = employee(status = status, permissions = PosPermission.entries)

            assertEquals("status=$status", settingsOnly, employee.visibleHomeDestinations())
        }
    }

    @Test
    fun `an owner sees every destination in drawer order whatever their status`() {
        EmployeeStatus.entries.forEach { status ->
            val owner = employee(isOwner = true, status = status)

            assertEquals("status=$status", HomeDestination.entries, owner.visibleHomeDestinations())
        }
    }

    @Test
    fun `an enabled employee without permissions only shows settings`() {
        assertEquals(settingsOnly, employee().visibleHomeDestinations())
    }

    @Test
    fun `the checkout permission shows checkout and customers`() {
        assertEquals(
            listOf(HomeDestination.CHECKOUT, HomeDestination.CUSTOMERS, HomeDestination.SETTINGS),
            employeeWith(PosPermission.CHECKOUT).visibleHomeDestinations(),
        )
    }

    @Test
    fun `catalog inventory and inventory report permissions each show inventory`() {
        listOf(
            PosPermission.MANAGE_CATALOG,
            PosPermission.MANAGE_INVENTORY,
            PosPermission.VIEW_INVENTORY_REPORTS,
        ).forEach { permission ->
            assertEquals(
                "permission=$permission",
                true,
                HomeDestination.INVENTORY in employeeWith(permission).visibleHomeDestinations(),
            )
        }
    }

    @Test
    fun `the supplier permission shows suppliers`() {
        assertEquals(
            listOf(HomeDestination.SUPPLIERS, HomeDestination.SETTINGS),
            employeeWith(PosPermission.MANAGE_SUPPLIERS).visibleHomeDestinations(),
        )
    }

    @Test
    fun `the transactions permission shows transactions`() {
        assertEquals(
            listOf(HomeDestination.TRANSACTIONS, HomeDestination.SETTINGS),
            employeeWith(PosPermission.VIEW_TRANSACTIONS).visibleHomeDestinations(),
        )
    }

    @Test
    fun `sales and inventory report permissions each show reports`() {
        assertEquals(
            listOf(HomeDestination.REPORTS, HomeDestination.SETTINGS),
            employeeWith(PosPermission.VIEW_SALES_REPORTS).visibleHomeDestinations(),
        )
        assertEquals(
            listOf(HomeDestination.INVENTORY, HomeDestination.REPORTS, HomeDestination.SETTINGS),
            employeeWith(PosPermission.VIEW_INVENTORY_REPORTS).visibleHomeDestinations(),
        )
    }

    @Test
    fun `expenses and staff are never shown to a non owner`() {
        val destinations = employeeWith(*PosPermission.entries.toTypedArray()).visibleHomeDestinations()

        assertEquals(false, HomeDestination.EXPENSES in destinations)
        assertEquals(false, HomeDestination.STAFF in destinations)
    }

    @Test
    fun `permissions without a destination add nothing`() {
        val permissions = listOf(PosPermission.APPLY_DISCOUNTS, PosPermission.VOID_TRANSACTION, PosPermission.REFUND)

        assertEquals(settingsOnly, employee(permissions = permissions).visibleHomeDestinations())
    }

    @Test
    fun `visible destinations keep drawer order`() {
        val destinations = employeeWith(*PosPermission.entries.toTypedArray()).visibleHomeDestinations()

        assertEquals(destinations.sortedBy { it.ordinal }, destinations)
        assertEquals(HomeDestination.CHECKOUT, destinations.first())
    }

    private fun employeeWith(vararg permissions: PosPermission): Employee {
        return employee(permissions = permissions.toList())
    }

    private fun employee(
        isOwner: Boolean = false,
        status: EmployeeStatus = EmployeeStatus.ENABLED,
        permissions: List<PosPermission> = emptyList(),
    ): Employee {
        val time = LocalDateTime.of(2026, 11, 14, 10, 30)
        return Employee(
            id = "emp_1",
            name = "Employee",
            isOwner = isOwner,
            status = status,
            permissions = permissions,
            account = null,
            createdAt = time,
            updatedAt = time,
            maxDiscountRate = Decimal("0.00"),
        )
    }
}
