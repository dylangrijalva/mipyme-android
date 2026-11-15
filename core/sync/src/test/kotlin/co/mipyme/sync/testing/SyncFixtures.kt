package co.mipyme.sync.testing

import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.SyncResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val TestJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

fun authenticated(merchantId: String? = "merch_1", accountId: String = "user_1"): AuthState.Authenticated {
    return AuthState.Authenticated(
        IdentityAccount(
            id = accountId,
            name = "Monkey D. Luffy",
            email = "luffy@example.com",
            pictureUrl = "",
            publicMetadata = IdentityPublicMetadata(merchantId = merchantId),
        ),
    )
}

fun syncResponse(syncToken: String = "token_1", fullSync: Boolean = true): SyncResponse {
    return SyncResponse(
        merchant = null,
        fullSync = fullSync,
        syncToken = syncToken,
        products = emptyList(),
        collections = emptyList(),
        customers = emptyList(),
        suppliers = emptyList(),
        locations = emptyList(),
        employees = emptyList(),
    )
}
