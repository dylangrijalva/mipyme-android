package co.mipyme.data.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import arrow.core.Either
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private val PROMPT_ANSWERED_KEY = booleanPreferencesKey("notification_prompt_answered")

class NotificationPermissionRepository(
    private val dataStore: DataStore<Preferences>,
    private val permissionChecker: NotificationPermissionChecker,
    private val authService: AuthService,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val sessionAnswered = MutableStateFlow(false)

    private val storedAnswered: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.w(error, "Could not read the notification prompt state")
            emit(emptyPreferences())
        }
        .map { preferences -> preferences[PROMPT_ANSWERED_KEY] ?: false }

    val answeredFlow: Flow<Boolean> = combine(storedAnswered, sessionAnswered) { stored, session ->
        stored || session
    }.distinctUntilChanged()

    init {
        scope.launch {
            authService.authStateFlow.collect { authState ->
                if (authState != AuthState.Initializing && authState !is AuthState.Authenticated) {
                    clearAnswered()
                }
            }
        }
    }

    suspend fun markAnswered() {
        sessionAnswered.update { true }
        Either.catch { dataStore.edit { preferences -> preferences[PROMPT_ANSWERED_KEY] = true } }
            .onLeft { error -> Timber.w(error, "Could not persist the notification prompt answer") }
    }

    suspend fun isPermissionGranted(): Boolean {
        return withContext(ioDispatcher) { permissionChecker.isGranted() }
    }

    private suspend fun clearAnswered() {
        sessionAnswered.update { false }
        Either.catch { dataStore.edit { preferences -> preferences.remove(PROMPT_ANSWERED_KEY) } }
            .onLeft { error -> Timber.w(error, "Could not clear the notification prompt answer") }
    }
}
