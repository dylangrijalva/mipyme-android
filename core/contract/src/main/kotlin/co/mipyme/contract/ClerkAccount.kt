package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class ClerkAccount(
    val id: String,
    val name: String,
    val email: String?,
    val pictureUrl: String,
    val publicMetadata: IdentityPublicMetadata,
)
