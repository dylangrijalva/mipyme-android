package co.mipyme.auth.internal

// The cooldown protects one address from repeated sends; a corrected email shouldn't have to wait.
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
