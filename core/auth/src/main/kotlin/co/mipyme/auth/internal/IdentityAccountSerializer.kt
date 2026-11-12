package co.mipyme.auth.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import co.mipyme.contract.IdentityAccount
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json

private const val IDENTITY_ACCOUNT_FILE_NAME = "identity_account.json"

private val identityAccountJson = Json { ignoreUnknownKeys = true }

internal val Context.identityAccountDataStore: DataStore<IdentityAccount?> by dataStore(
    fileName = IDENTITY_ACCOUNT_FILE_NAME,
    serializer = IdentityAccountSerializer,
)

internal object IdentityAccountSerializer : Serializer<IdentityAccount?> {
    override val defaultValue: IdentityAccount? = null

    override suspend fun readFrom(input: InputStream): IdentityAccount? {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return null
        return try {
            identityAccountJson.decodeFromString(IdentityAccount.serializer(), bytes.decodeToString())
        } catch (exception: IllegalArgumentException) {
            // Covers SerializationException and invalid values rejected by the contract class.
            null
        }
    }

    override suspend fun writeTo(t: IdentityAccount?, output: OutputStream) {
        if (t == null) return
        output.write(identityAccountJson.encodeToString(IdentityAccount.serializer(), t).encodeToByteArray())
    }
}
