package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class IdentityAccount(
    val id: String,
    val name: String,
    val email: String?,
    val pictureUrl: String,
    val publicMetadata: IdentityPublicMetadata,
)

@Serializable
data class IdentityPublicMetadata(
    val merchantId: String? = null,
    val employeeId: String? = null,
    val type: AccountType? = null,
)
