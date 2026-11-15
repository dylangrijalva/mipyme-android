package co.mipyme.screen.settings

import co.mipyme.contract.Decimal
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsItemTest {

    private val profileOnly = listOf(SettingsItem.PROFILE)

    @Test
    fun `no employee only shows profile`() {
        assertEquals(profileOnly, (null as Employee?).visibleSettingsItems())
    }

    @Test
    fun `an owner sees every item in order whatever their status`() {
        EmployeeStatus.entries.forEach { status ->
            val owner = employee(isOwner = true, status = status)

            assertEquals("status=$status", SettingsItem.entries, owner.visibleSettingsItems())
        }
    }

    @Test
    fun `an enabled non owner only shows profile`() {
        assertEquals(profileOnly, employee(status = EmployeeStatus.ENABLED).visibleSettingsItems())
    }

    @Test
    fun `a non owner that is not enabled only shows profile`() {
        listOf(EmployeeStatus.INVITED, EmployeeStatus.DISABLED).forEach { status ->
            assertEquals("status=$status", profileOnly, employee(status = status).visibleSettingsItems())
        }
    }

    @Test
    fun `items are declared in display order`() {
        assertEquals(
            listOf(
                SettingsItem.GENERAL,
                SettingsItem.PROFILE,
                SettingsItem.BUSINESS_INFORMATION,
                SettingsItem.PRINTERS_AND_DEVICES,
                SettingsItem.TICKETS,
            ),
            SettingsItem.entries,
        )
    }

    private fun employee(
        isOwner: Boolean = false,
        status: EmployeeStatus = EmployeeStatus.ENABLED,
    ): Employee {
        val time = LocalDateTime.of(2026, 11, 14, 10, 30)
        return Employee(
            id = "emp_1",
            name = "Employee",
            isOwner = isOwner,
            status = status,
            permissions = emptyList(),
            account = null,
            createdAt = time,
            updatedAt = time,
            maxDiscountRate = Decimal("0.00"),
        )
    }
}
