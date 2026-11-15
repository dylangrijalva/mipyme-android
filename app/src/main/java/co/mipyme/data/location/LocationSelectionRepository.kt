package co.mipyme.data.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.contract.Location
import co.mipyme.database.MipymeCache
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

private val SELECTED_EMPLOYEE_ID_KEY = stringPreferencesKey("selected_employee_id")
private val SELECTED_LOCATION_ID_KEY = stringPreferencesKey("selected_location_id")

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionRepository(
    private val dataStore: DataStore<Preferences>,
    private val authService: AuthService,
    private val cache: MipymeCache,
    scope: CoroutineScope,
) {
    private val storedSelection: Flow<StoredSelection?> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.w(error, "Could not read the selected location")
            emit(emptyPreferences())
        }
        .map { preferences ->
            val userId = preferences[SELECTED_EMPLOYEE_ID_KEY]
            val locationId = preferences[SELECTED_LOCATION_ID_KEY]
            if (userId != null && locationId != null) StoredSelection(userId, locationId) else null
        }
        .distinctUntilChanged()

    private val accessFlow: Flow<LocationAccess?> = authService.authStateFlow
        .map { it.locationUserId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                cache.observeLocationAccess(userId).map { access ->
                    access?.let {
                        LocationAccess(
                            userId = userId,
                            merchantId = it.merchantId,
                            isOwner = it.isOwner,
                            locations = it.locations,
                        )
                    }
                }
            }
        }
        .distinctUntilChanged()

    private val resolutionFlow = combine(storedSelection, accessFlow) { stored, access ->
        resolve(stored, access)
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val locationSelectionFlow: Flow<LocationSelectionState> = resolutionFlow
        .filterNotNull()
        .map { it.state }
        .distinctUntilChanged()

    val accessibleLocationsFlow: Flow<List<Location>> = locationSelectionFlow
        .map { it.accessibleLocations }
        .distinctUntilChanged()

    val selectedLocationFlow: Flow<Location?> = locationSelectionFlow
        .map { it.selectedLocation }
        .distinctUntilChanged()

    @Volatile
    private var attemptedAutoSelection: StoredSelection? = null

    init {
        scope.launch {
            authService.authStateFlow.collect { authState ->
                if (authState != AuthState.Initializing && authState !is AuthState.Authenticated) {
                    attemptedAutoSelection = null
                    clearSelection()
                }
            }
        }
        scope.launch {
            resolutionFlow.filterNotNull().collect { resolution -> reconcile(resolution) }
        }
    }

    suspend fun select(locationId: String): Either<LocationSelectionError, Unit> {
        val state = locationSelectionFlow.first()
        val userId = state.userId
        if (userId == null || state.accessibleLocations.none { it.id == locationId }) {
            return LocationSelectionError.NotAccessible.left()
        }
        return persist(StoredSelection(userId, locationId))
    }

    private fun resolve(stored: StoredSelection?, access: LocationAccess?): Resolution {
        if (access == null) {
            return Resolution(state = LocationSelectionState())
        }
        val storedLocation = stored
            ?.takeIf { it.userId == access.userId }
            ?.let { selection -> access.locations.firstOrNull { it.id == selection.locationId } }
        val autoLocation = if (storedLocation == null && !access.isOwner) {
            access.locations.singleOrNull()
        } else {
            null
        }
        return Resolution(
            state = LocationSelectionState(
                userId = access.userId,
                merchantId = access.merchantId,
                accessibleLocations = access.locations,
                selectedLocation = storedLocation ?: autoLocation,
            ),
            staleSelection = stored.takeIf { storedLocation == null },
            autoSelection = autoLocation?.let { StoredSelection(access.userId, it.id) },
        )
    }

    private suspend fun reconcile(resolution: Resolution) {
        val auto = resolution.autoSelection
        if (auto != null) {
            if (auto != attemptedAutoSelection) {
                attemptedAutoSelection = auto
                persist(auto)
            }
            return
        }
        attemptedAutoSelection = null
        resolution.staleSelection?.let { clearSelectionIfStill(it) }
    }

    private suspend fun persist(selection: StoredSelection): Either<LocationSelectionError, Unit> {
        var written = false
        return Either.catch {
            dataStore.edit { preferences ->
                if (authService.authStateFlow.value.locationUserId == selection.userId) {
                    preferences[SELECTED_EMPLOYEE_ID_KEY] = selection.userId
                    preferences[SELECTED_LOCATION_ID_KEY] = selection.locationId
                    written = true
                }
            }
        }
            .onLeft { error -> Timber.w(error, "Could not persist the selected location") }
            .mapLeft<LocationSelectionError> { LocationSelectionError.PersistFailed }
            .flatMap { if (written) Either.Right(Unit) else Either.Left(LocationSelectionError.NotAccessible) }
    }

    private suspend fun clearSelectionIfStill(stale: StoredSelection) {
        Either.catch {
            dataStore.edit { preferences ->
                if (preferences[SELECTED_EMPLOYEE_ID_KEY] == stale.userId &&
                    preferences[SELECTED_LOCATION_ID_KEY] == stale.locationId
                ) {
                    preferences.remove(SELECTED_EMPLOYEE_ID_KEY)
                    preferences.remove(SELECTED_LOCATION_ID_KEY)
                }
            }
        }.onLeft { error -> Timber.w(error, "Could not clear the selected location") }
    }

    suspend fun clearSelection(): Either<LocationSelectionError, Unit> {
        return Either.catch {
            dataStore.edit { preferences ->
                preferences.remove(SELECTED_EMPLOYEE_ID_KEY)
                preferences.remove(SELECTED_LOCATION_ID_KEY)
            }
        }
            .onLeft { error -> Timber.w(error, "Could not clear the selected location") }
            .mapLeft<LocationSelectionError> { LocationSelectionError.PersistFailed }
            .map { }
    }

    private data class StoredSelection(val userId: String, val locationId: String)

    private data class LocationAccess(
        val userId: String,
        val merchantId: String,
        val isOwner: Boolean,
        val locations: List<Location>,
    )

    private data class Resolution(
        val state: LocationSelectionState,
        val staleSelection: StoredSelection? = null,
        val autoSelection: StoredSelection? = null,
    )
}
