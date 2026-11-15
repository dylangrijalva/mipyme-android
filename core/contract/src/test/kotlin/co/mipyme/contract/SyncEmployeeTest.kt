package co.mipyme.contract

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncEmployeeTest {
    private val json = Json

    private fun employeeJson(permissions: String): String {
        return """
            {
              "id": "emp_1",
              "name": "Nami",
              "isOwner": false,
              "status": "enabled",
              "permissions": $permissions,
              "account": null,
              "createdAt": "2026-09-23T09:00:00.000",
              "updatedAt": "2026-09-23T09:00:00.000",
              "maxDiscountRate": "0.00",
              "locationIds": ["loc_1"]
            }
        """.trimIndent()
    }

    @Test
    fun decodesKnownPermissionsByTheirWireNames() {
        val employee = json.decodeFromString<SyncEmployee>(employeeJson("""["checkout", "view_sales_reports"]"""))

        assertEquals(listOf(PosPermission.CHECKOUT, PosPermission.VIEW_SALES_REPORTS), employee.permissions)
    }

    @Test
    fun dropsUnknownPermissions() {
        val employee = json.decodeFromString<SyncEmployee>(employeeJson("""["checkout", "teleport", "refund"]"""))

        assertEquals(listOf(PosPermission.CHECKOUT, PosPermission.REFUND), employee.permissions)
    }

    @Test
    fun encodesPermissionsByTheirWireNames() {
        val employee = json.decodeFromString<SyncEmployee>(employeeJson("""["view_sales_reports"]"""))

        val encoded = json.encodeToString(SyncEmployee.serializer(), employee)

        assertEquals(true, encoded.contains("\"permissions\":[\"view_sales_reports\"]"))
    }
}
