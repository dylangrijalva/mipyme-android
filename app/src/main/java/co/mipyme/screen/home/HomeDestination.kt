package co.mipyme.screen.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import co.mipyme.R
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.PosPermission
import kotlinx.serialization.Serializable

@Serializable
enum class HomeDestination {
    CHECKOUT,
    INVENTORY,
    SUPPLIERS,
    CUSTOMERS,
    EXPENSES,
    TRANSACTIONS,
    REPORTS,
    STAFF,
    SETTINGS,
}

@StringRes
fun HomeDestination.titleRes(): Int {
    return when (this) {
        HomeDestination.CHECKOUT -> R.string.home_destination_checkout
        HomeDestination.INVENTORY -> R.string.home_destination_inventory
        HomeDestination.SUPPLIERS -> R.string.home_destination_suppliers
        HomeDestination.CUSTOMERS -> R.string.home_destination_customers
        HomeDestination.EXPENSES -> R.string.home_destination_expenses
        HomeDestination.TRANSACTIONS -> R.string.home_destination_transactions
        HomeDestination.REPORTS -> R.string.home_destination_reports
        HomeDestination.STAFF -> R.string.home_destination_staff
        HomeDestination.SETTINGS -> R.string.home_destination_settings
    }
}

@DrawableRes
fun HomeDestination.iconRes(): Int {
    return when (this) {
        HomeDestination.CHECKOUT -> R.drawable.ic_fluent_cart_24_regular
        HomeDestination.INVENTORY -> R.drawable.ic_fluent_box_24_regular
        HomeDestination.SUPPLIERS -> R.drawable.ic_fluent_vehicle_truck_cube_24_regular
        HomeDestination.CUSTOMERS -> R.drawable.ic_fluent_people_24_regular
        HomeDestination.EXPENSES -> R.drawable.ic_fluent_wallet_24_regular
        HomeDestination.TRANSACTIONS -> R.drawable.ic_fluent_receipt_24_regular
        HomeDestination.REPORTS -> R.drawable.ic_fluent_data_bar_vertical_24_regular
        HomeDestination.STAFF -> R.drawable.ic_fluent_people_team_24_regular
        HomeDestination.SETTINGS -> R.drawable.ic_fluent_settings_24_regular
    }
}

@DrawableRes
fun HomeDestination.selectedIconRes(): Int {
    return when (this) {
        HomeDestination.CHECKOUT -> R.drawable.ic_fluent_cart_24_filled
        HomeDestination.INVENTORY -> R.drawable.ic_fluent_box_24_filled
        HomeDestination.SUPPLIERS -> R.drawable.ic_fluent_vehicle_truck_cube_24_filled
        HomeDestination.CUSTOMERS -> R.drawable.ic_fluent_people_24_filled
        HomeDestination.EXPENSES -> R.drawable.ic_fluent_wallet_24_filled
        HomeDestination.TRANSACTIONS -> R.drawable.ic_fluent_receipt_24_filled
        HomeDestination.REPORTS -> R.drawable.ic_fluent_data_bar_vertical_24_filled
        HomeDestination.STAFF -> R.drawable.ic_fluent_people_team_24_filled
        HomeDestination.SETTINGS -> R.drawable.ic_fluent_settings_24_filled
    }
}

fun Employee?.visibleHomeDestinations(): List<HomeDestination> {
    if (this == null || (!isOwner && status != EmployeeStatus.ENABLED)) {
        return listOf(HomeDestination.SETTINGS)
    }
    return HomeDestination.entries.filter { destination ->
        isOwner || when (destination) {
            HomeDestination.CHECKOUT,
            HomeDestination.CUSTOMERS -> PosPermission.CHECKOUT in permissions

            HomeDestination.INVENTORY -> permissions.any {
                it == PosPermission.MANAGE_CATALOG ||
                    it == PosPermission.MANAGE_INVENTORY ||
                    it == PosPermission.VIEW_INVENTORY_REPORTS
            }

            HomeDestination.SUPPLIERS -> PosPermission.MANAGE_SUPPLIERS in permissions
            HomeDestination.TRANSACTIONS -> PosPermission.VIEW_TRANSACTIONS in permissions
            HomeDestination.REPORTS -> permissions.any {
                it == PosPermission.VIEW_SALES_REPORTS || it == PosPermission.VIEW_INVENTORY_REPORTS
            }

            HomeDestination.EXPENSES,
            HomeDestination.STAFF -> false

            HomeDestination.SETTINGS -> true
        }
    }
}
