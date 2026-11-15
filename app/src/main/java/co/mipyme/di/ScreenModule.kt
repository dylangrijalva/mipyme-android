package co.mipyme.di

import co.mipyme.mainScreenModule
import co.mipyme.screen.ask_notification_permissions.askNotificationPermissionsScreenModule
import co.mipyme.screen.business_information_settings.businessInformationSettingsScreenModule
import co.mipyme.screen.checkout.checkoutScreenModule
import co.mipyme.screen.customers.customersScreenModule
import co.mipyme.screen.expenses.expensesScreenModule
import co.mipyme.screen.general_settings.generalSettingsScreenModule
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.initial_sync.initialSyncScreenModule
import co.mipyme.screen.inventory.inventoryScreenModule
import co.mipyme.screen.no_location_access.noLocationAccessScreenModule
import co.mipyme.screen.onboarding.onboardingScreenModule
import co.mipyme.screen.printers_and_devices_settings.printersAndDevicesSettingsScreenModule
import co.mipyme.screen.profile_settings.profileSettingsScreenModule
import co.mipyme.screen.reports.reportsScreenModule
import co.mipyme.screen.select_location.selectLocationScreenModule
import co.mipyme.screen.settings.settingsScreenModule
import co.mipyme.screen.sign_in_with_email.signInWithEmailScreenModule
import co.mipyme.screen.staff.staffScreenModule
import co.mipyme.screen.suppliers.suppliersScreenModule
import co.mipyme.screen.tickets_settings.ticketsSettingsScreenModule
import co.mipyme.screen.transactions.transactionsScreenModule
import co.mipyme.screen.verify_otp_code.verifyOtpCodeScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import org.koin.core.module.Module
import org.koin.dsl.module

fun screenModule(): Module {
    return module {
        mainScreenModule()
        welcomeScreenModule()
        signInWithEmailScreenModule()
        verifyOtpCodeScreenModule()
        onboardingScreenModule()
        initialSyncScreenModule()
        askNotificationPermissionsScreenModule()
        selectLocationScreenModule()
        noLocationAccessScreenModule()
        homeScreenModule()
        checkoutScreenModule()
        inventoryScreenModule()
        suppliersScreenModule()
        customersScreenModule()
        expensesScreenModule()
        transactionsScreenModule()
        reportsScreenModule()
        staffScreenModule()
        settingsScreenModule()
        generalSettingsScreenModule()
        profileSettingsScreenModule()
        businessInformationSettingsScreenModule()
        printersAndDevicesSettingsScreenModule()
        ticketsSettingsScreenModule()
    }
}
