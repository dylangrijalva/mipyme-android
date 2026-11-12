package co.mipyme.auth.internal

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NullableAccountDataStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private val scopes = mutableListOf<CoroutineScope>()

    private val account = IdentityAccount(
        id = "user_1",
        name = "Ana Pérez",
        email = "ana@example.com",
        pictureUrl = "",
        publicMetadata = IdentityPublicMetadata(),
    )

    @Before
    fun setUp() {
        file = File(folder.root, "identity_account.json")
    }

    @After
    fun tearDown() = runBlocking {
        stopRunningStores()
    }

    @Test
    fun `account written by one instance is read by a fresh instance`() = runBlocking {
        newStore().updateData { account }

        assertEquals(account, newStore().data.first())
    }

    @Test
    fun `cleared account is read as null by a fresh instance after a restart`() = runBlocking {
        newStore().updateData { account }
        newStore().updateData { null }

        assertNull(newStore().data.first())
    }

    @Test
    fun `clearing an account on a fresh instance succeeds after a previous clear`() = runBlocking {
        newStore().updateData { account }
        newStore().updateData { null }

        val result = newStore().updateData { null }

        assertNull(result)
    }

    @Test
    fun `saving an account after a previous clear succeeds on a fresh instance`() = runBlocking {
        newStore().updateData { account }
        newStore().updateData { null }

        newStore().updateData { account }

        assertEquals(account, newStore().data.first())
    }

    @Test
    fun `an existing empty file reads as null`() = runBlocking {
        file.writeBytes(ByteArray(0))

        assertNull(newStore().data.first())
    }

    // Simulates a process restart: the previous store is stopped before a new one opens the file.
    private suspend fun newStore(): DataStore<IdentityAccount?> {
        stopRunningStores()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val delegate = DataStoreFactory.create(
            serializer = IdentityAccountSerializer,
            scope = scope,
            produceFile = { file },
        )
        return NullableAccountDataStore(delegate)
    }

    private suspend fun stopRunningStores() {
        scopes.forEach { scope -> scope.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }
}
