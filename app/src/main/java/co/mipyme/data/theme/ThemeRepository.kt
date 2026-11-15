package co.mipyme.data.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class ThemeRepository(private val dataStore: DataStore<Preferences>) {
    val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.w(error, "Could not read the theme mode")
            emit(emptyPreferences())
        }
        .map { preferences ->
            ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE_KEY] } ?: ThemeMode.SYSTEM
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode): Either<ThemeError, Unit> {
        return Either.catch {
            dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
        }
            .onLeft { error -> Timber.w(error, "Could not persist the theme mode") }
            .mapLeft<ThemeError> { ThemeError.PersistFailed }
            .map { }
    }
}
