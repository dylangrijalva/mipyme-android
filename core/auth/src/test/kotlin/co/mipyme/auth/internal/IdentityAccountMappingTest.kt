package co.mipyme.auth.internal

import co.mipyme.contract.AccountType
import co.mipyme.contract.IdentityPublicMetadata
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentityAccountMappingTest {

    @Test
    fun name_joins_first_and_last_name_with_a_space() {
        val account = identityAccountOf(
            id = "user_1",
            email = "ana@example.com",
            firstName = "Ana",
            lastName = "Pérez",
            imageUrl = "https://img.example.com/ana.png",
            publicMetadata = null,
        )

        assertEquals("Ana Pérez", account.name)
    }

    @Test
    fun name_uses_only_the_present_part_when_one_name_is_missing() {
        val account = identityAccountOf(
            id = "user_1",
            email = null,
            firstName = "Ana",
            lastName = null,
            imageUrl = "",
            publicMetadata = null,
        )

        assertEquals("Ana", account.name)
    }

    @Test
    fun name_is_empty_when_both_names_are_missing() {
        val account = identityAccountOf(
            id = "user_1",
            email = null,
            firstName = null,
            lastName = null,
            imageUrl = "",
            publicMetadata = null,
        )

        assertEquals("", account.name)
    }

    @Test
    fun email_and_picture_are_passed_through() {
        val account = identityAccountOf(
            id = "user_1",
            email = "ana@example.com",
            firstName = null,
            lastName = null,
            imageUrl = "https://img.example.com/ana.png",
            publicMetadata = null,
        )

        assertEquals("ana@example.com", account.email)
        assertEquals("https://img.example.com/ana.png", account.pictureUrl)
    }

    @Test
    fun snake_case_metadata_maps_to_identity_public_metadata() {
        val account = identityAccountOf(
            id = "user_1",
            email = null,
            firstName = null,
            lastName = null,
            imageUrl = "",
            publicMetadata = mapOf(
                "merchant_id" to JsonPrimitive("merchant_9"),
                "employee_id" to JsonPrimitive("employee_4"),
                "account_type" to JsonPrimitive("staff"),
            ),
        )

        assertEquals(
            IdentityPublicMetadata(
                merchantId = "merchant_9",
                employeeId = "employee_4",
                type = AccountType.STAFF,
            ),
            account.publicMetadata,
        )
    }

    @Test
    fun account_type_values_map_to_account_types() {
        assertEquals(AccountType.OWNER, accountTypeFrom("owner"))
        assertEquals(AccountType.STAFF, accountTypeFrom("staff"))
        assertEquals(AccountType.INTERNAL, accountTypeFrom("internal"))
    }

    @Test
    fun unknown_account_type_maps_to_null() {
        assertNull(accountTypeFrom("superuser"))
    }

    @Test
    fun missing_metadata_keys_map_to_null_fields() {
        val account = identityAccountOf(
            id = "user_1",
            email = null,
            firstName = null,
            lastName = null,
            imageUrl = "",
            publicMetadata = emptyMap(),
        )

        assertEquals(IdentityPublicMetadata(), account.publicMetadata)
    }

    @Test
    fun null_and_non_primitive_metadata_values_map_to_null_fields() {
        val nonPrimitive: JsonElement = JsonArray(listOf(JsonPrimitive("merchant_1")))
        val account = identityAccountOf(
            id = "user_1",
            email = null,
            firstName = null,
            lastName = null,
            imageUrl = "",
            publicMetadata = mapOf(
                "merchant_id" to JsonNull,
                "employee_id" to nonPrimitive,
                "account_type" to JsonNull,
            ),
        )

        assertEquals(IdentityPublicMetadata(), account.publicMetadata)
    }

    private fun accountTypeFrom(value: String): AccountType? {
        return identityAccountOf(
            id = "user_1",
            email = null,
            firstName = null,
            lastName = null,
            imageUrl = "",
            publicMetadata = mapOf("account_type" to JsonPrimitive(value)),
        ).publicMetadata.type
    }
}
