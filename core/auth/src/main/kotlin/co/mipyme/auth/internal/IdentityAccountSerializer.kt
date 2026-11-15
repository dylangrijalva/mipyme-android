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

private val identityAccountJson = Json { ignoreUnknownKeys = true }

private val Context.storedAccountDataStore: DataStore<StoredAccount> by dataStore(
    fileName = "identity_account.json",
    serializer = IdentityAccountSerializer,
)

internal val Context.identityAccountDataStore: DataStore<IdentityAccount?>
    get() = NullableAccountDataStore(storedAccountDataStore)

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
            defaultValue
        }
    }

    override suspend fun writeTo(t: StoredAccount, output: OutputStream) {
        val account = t.account ?: return
        output.write(identityAccountJson.encodeToString(IdentityAccount.serializer(), account).encodeToByteArray())
    }
}
