package co.mipyme.auth.internal

import co.mipyme.contract.AccountType
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val KEY_MERCHANT_ID = "merchant_id"
private const val KEY_EMPLOYEE_ID = "employee_id"
private const val KEY_ACCOUNT_TYPE = "account_type"

internal fun identityAccountOf(
    id: String,
    email: String?,
    firstName: String?,
    lastName: String?,
    imageUrl: String,
    publicMetadata: Map<String, JsonElement>?,
): IdentityAccount {
    return IdentityAccount(
        id = id,
        name = listOfNotNull(firstName, lastName).joinToString(separator = " ").trim(),
        email = email,
        pictureUrl = imageUrl,
        publicMetadata = IdentityPublicMetadata(
            merchantId = publicMetadata.stringValue(KEY_MERCHANT_ID),
            employeeId = publicMetadata.stringValue(KEY_EMPLOYEE_ID),
            type = publicMetadata.stringValue(KEY_ACCOUNT_TYPE).toAccountType(),
        ),
    )
}

private fun Map<String, JsonElement>?.stringValue(key: String): String? {
    val primitive = this?.get(key) as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun String?.toAccountType(): AccountType? {
    return when (this) {
        "owner" -> AccountType.OWNER
        "staff" -> AccountType.STAFF
        "internal" -> AccountType.INTERNAL
        else -> null
    }
}
