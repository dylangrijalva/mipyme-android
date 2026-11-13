package co.mipyme.auth.internal

internal fun isOtpCooldownBlocking(
    secondsRemaining: Int,
    cooldownEmail: String?,
    requestedEmail: String,
): Boolean {
    if (secondsRemaining <= 0) {
        return false
    }
    return cooldownEmail != null && cooldownEmail.equals(requestedEmail.trim(), ignoreCase = true)
}
