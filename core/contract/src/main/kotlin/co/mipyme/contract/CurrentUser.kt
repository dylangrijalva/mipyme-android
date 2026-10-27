package co.mipyme.contract

import kotlinx.serialization.Serializable

@Serializable
data class CurrentUser(
    val id: String,
    val email: String,
    val merchantId: String,
    val employeeId: String,
    val accountType: AccountType,
)
