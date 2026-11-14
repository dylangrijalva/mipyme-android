package co.mipyme.auth

import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthStateTest {

    @Test
    fun `authenticated state exposes the account's merchant`() {
        assertEquals("merch_1", AuthState.Authenticated(accountWith(merchantId = "merch_1")).merchantId)
    }

    @Test
    fun `authenticated state without a merchant has no merchant`() {
        assertNull(AuthState.Authenticated(accountWith(merchantId = null)).merchantId)
    }

    @Test
    fun `non-authenticated states have no merchant`() {
        listOf(
            AuthState.Initializing,
            AuthState.Unauthenticated,
            AuthState.SessionExpired,
            AuthState.Blocked,
        ).forEach { assertNull(it.merchantId) }
    }

    private fun accountWith(merchantId: String?): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Ana Pérez",
            email = "ana@example.com",
            pictureUrl = "https://img.example.com/ana.png",
            publicMetadata = IdentityPublicMetadata(merchantId = merchantId),
        )
    }
}
