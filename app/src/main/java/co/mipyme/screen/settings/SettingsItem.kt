package co.mipyme.screen.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import co.mipyme.R
import co.mipyme.contract.Employee
import co.mipyme.navigation.Route

enum class SettingsItem {
    GENERAL,
    PROFILE,
    BUSINESS_INFORMATION,
    PRINTERS_AND_DEVICES,
    TICKETS,
}

@StringRes
fun SettingsItem.titleRes(): Int {
    return when (this) {
        SettingsItem.GENERAL -> R.string.settings_general
        SettingsItem.PROFILE -> R.string.settings_profile
        SettingsItem.BUSINESS_INFORMATION -> R.string.settings_business_information
        SettingsItem.PRINTERS_AND_DEVICES -> R.string.settings_printers_and_devices
        SettingsItem.TICKETS -> R.string.settings_tickets
    }
}

@DrawableRes
fun SettingsItem.iconRes(): Int {
    return when (this) {
        SettingsItem.GENERAL -> R.drawable.ic_fluent_settings_24_regular
        SettingsItem.PROFILE -> R.drawable.ic_fluent_person_24_regular
        SettingsItem.BUSINESS_INFORMATION -> R.drawable.ic_fluent_building_retail_24_regular
        SettingsItem.PRINTERS_AND_DEVICES -> R.drawable.ic_fluent_print_24_regular
        SettingsItem.TICKETS -> R.drawable.ic_fluent_receipt_money_24_regular
    }
}

fun SettingsItem.route(): Route {
    return when (this) {
        SettingsItem.GENERAL -> Route.GeneralSettings
        SettingsItem.PROFILE -> Route.ProfileSettings
        SettingsItem.BUSINESS_INFORMATION -> Route.BusinessInformationSettings
        SettingsItem.PRINTERS_AND_DEVICES -> Route.PrintersAndDevicesSettings
        SettingsItem.TICKETS -> Route.TicketsSettings
    }
}

fun Employee?.visibleSettingsItems(): List<SettingsItem> {
    return SettingsItem.entries.filter { item ->
        item == SettingsItem.PROFILE || this?.isOwner == true
    }
}
