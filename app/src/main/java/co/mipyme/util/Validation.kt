package co.mipyme.util

private val EMAIL_ADDRESS_REGEX = Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$")

fun isValidEmail(email: String): Boolean {
    return EMAIL_ADDRESS_REGEX.matches(email.trim())
}
