package co.mipyme.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Welcome : Route

    @Serializable
    data object SignInWithEmail : Route

    @Serializable
    data class VerifyOtpCode(val email: String) : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object InitialSync : Route

    @Serializable
    data object AskNotificationPermissions : Route

    @Serializable
    data object SelectLocation : Route

    @Serializable
    data object NoLocationAccess : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object GeneralSettings : Route

    @Serializable
    data object ProfileSettings : Route

    @Serializable
    data object BusinessInformationSettings : Route

    @Serializable
    data object PrintersAndDevicesSettings : Route

    @Serializable
    data object TicketsSettings : Route
}
