package co.mipyme.auth.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import co.mipyme.contract.IdentityAccount
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val IDENTITY_ACCOUNT_FILE_NAME = "identity_account.json"

private val identityAccountJson = Json { ignoreUnknownKeys = true }

private val Context.storedAccountDataStore: DataStore<StoredAccount> by dataStore(
    fileName = IDENTITY_ACCOUNT_FILE_NAME,
    serializer = IdentityAccountSerializer,
)

internal val Context.identityAccountDataStore: DataStore<IdentityAccount?>
    get() = NullableAccountDataStore(storedAccountDataStore)

// DataStore reads files through Okio, which fails with a NullPointerException when a serializer
// returns null for an existing file. The "no account" state therefore travels inside a non-null
// wrapper; an empty file on disk still means "no account".
internal data class StoredAccount(val account: IdentityAccount?)

internal class NullableAccountDataStore(
    private val delegate: DataStore<StoredAccount>,
) : DataStore<IdentityAccount?> {
    override val data: Flow<IdentityAccount?> = delegate.data.map { stored -> stored.account }

    override suspend fun updateData(transform: suspend (t: IdentityAccount?) -> IdentityAccount?): IdentityAccount? {
        return delegate.updateData { stored -> StoredAccount(transform(stored.account)) }.account
    }
}

internal object IdentityAccountSerializer : Serializer<StoredAccount> {
    override val defaultValue: StoredAccount = StoredAccount(account = null)

    override suspend fun readFrom(input: InputStream): StoredAccount {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            val account = identityAccountJson.decodeFromString(IdentityAccount.serializer(), bytes.decodeToString())
            StoredAccount(account)
        } catch (exception: IllegalArgumentException) {
            // Covers SerializationException and invalid values rejected by the contract class.
            defaultValue
        }
    }

    override suspend fun writeTo(t: StoredAccount, output: OutputStream) {
        val account = t.account ?: return
        output.write(identityAccountJson.encodeToString(IdentityAccount.serializer(), account).encodeToByteArray())
    }
}
