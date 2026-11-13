package co.mipyme.auth.internal

import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreRefreshTest {

    private fun accountWith(merchantId: String?): IdentityAccount {
        return IdentityAccount(
            id = "user_1",
            name = "Ana Pérez",
            email = "ana@example.com",
            pictureUrl = "https://img.example.com/ana.png",
            publicMetadata = IdentityPublicMetadata(merchantId = merchantId),
        )
    }

    @Test
    fun `cached account without a merchant is refreshed when the fetch failed`() {
        assertTrue(shouldRefreshAfterRestore(accountWith(merchantId = null), isFromServer = false))
    }

    @Test
    fun `account without a merchant is not refreshed when it was just fetched`() {
        assertFalse(shouldRefreshAfterRestore(accountWith(merchantId = null), isFromServer = true))
    }

    @Test
    fun `account with a merchant is not refreshed even when the fetch failed`() {
        assertFalse(shouldRefreshAfterRestore(accountWith(merchantId = "merch_1"), isFromServer = false))
    }

    @Test
    fun `account with a merchant is not refreshed when it was just fetched`() {
        assertFalse(shouldRefreshAfterRestore(accountWith(merchantId = "merch_1"), isFromServer = true))
    }
}
