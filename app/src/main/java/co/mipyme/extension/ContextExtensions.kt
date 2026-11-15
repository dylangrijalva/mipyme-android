package co.mipyme.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

private const val NOTIFICATION_PERMISSION_FILE_NAME = "notification_permission"

val Context.notificationPermissionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NOTIFICATION_PERMISSION_FILE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private const val LOCATION_SELECTION_FILE_NAME = "location_selection"

val Context.locationSelectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LOCATION_SELECTION_FILE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
