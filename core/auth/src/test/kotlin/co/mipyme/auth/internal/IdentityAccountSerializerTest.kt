package co.mipyme.auth.internal

import co.mipyme.contract.AccountType
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentityAccountSerializerTest {

    private val account = IdentityAccount(
        id = "user_1",
        name = "Ana Pérez",
        email = "ana@example.com",
        pictureUrl = "https://img.example.com/ana.png",
        publicMetadata = IdentityPublicMetadata(
            merchantId = "merchant_9",
            employeeId = "employee_4",
            type = AccountType.OWNER,
        ),
    )

    @Test
    fun written_account_is_read_back_unchanged() = runBlocking {
        val bytes = write(account)

        val restored = read(bytes)

        assertEquals(account, restored)
    }

    @Test
    fun writing_null_produces_empty_file_that_reads_as_null() = runBlocking {
        val bytes = write(null)

        assertEquals(0, bytes.size)
        assertNull(read(bytes))
    }

    @Test
    fun empty_input_reads_as_null() = runBlocking {
        assertNull(read(ByteArray(0)))
    }

    @Test
    fun corrupted_input_reads_as_null() = runBlocking {
        val corrupted = "{ this is not json".encodeToByteArray()

        assertNull(read(corrupted))
    }

    @Test
    fun json_missing_required_fields_reads_as_null() = runBlocking {
        val incomplete = """{"id":"user_1"}""".encodeToByteArray()

        assertNull(read(incomplete))
    }

    @Test
    fun unrecognised_account_type_reads_as_null_instead_of_crashing() = runBlocking {
        val invalidType = """
            {"id":"user_1","name":"Ana","email":null,"pictureUrl":"",
             "publicMetadata":{"type":"superadmin"}}
        """.trimIndent().encodeToByteArray()

        assertNull(read(invalidType))
    }

    @Test
    fun unknown_fields_are_ignored() = runBlocking {
        val withExtra = """
            {"id":"user_1","name":"Ana","email":null,"pictureUrl":"",
             "publicMetadata":{},"futureField":42}
        """.trimIndent().encodeToByteArray()

        val restored = read(withExtra)

        assertEquals("user_1", restored?.id)
    }

    private suspend fun write(value: IdentityAccount?): ByteArray {
        val output = ByteArrayOutputStream()
        IdentityAccountSerializer.writeTo(value, output)
        return output.toByteArray()
    }

    private suspend fun read(bytes: ByteArray): IdentityAccount? {
        return IdentityAccountSerializer.readFrom(ByteArrayInputStream(bytes))
    }
}
