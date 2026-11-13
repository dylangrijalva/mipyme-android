package co.mipyme.auth.internal

import co.mipyme.contract.IdentityAccount

// An account without a merchant may have been onboarded elsewhere. When the latest user could not
// be fetched and the session's cached user was used instead, a restored session re-reads the user
// once to pick up the latest public metadata. A user that was just fetched is already current.
internal fun shouldRefreshAfterRestore(account: IdentityAccount, isFromServer: Boolean): Boolean {
    return !isFromServer && account.publicMetadata.merchantId == null
}
