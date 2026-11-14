package co.mipyme.data.notification

fun interface NotificationPermissionChecker {
    fun isGranted(): Boolean
}
