package co.mipyme.auth.internal

import co.mipyme.contract.IdentityAccount

internal fun shouldRefreshAfterRestore(account: IdentityAccount, isFromServer: Boolean): Boolean {
    return !isFromServer && account.publicMetadata.merchantId == null
}
