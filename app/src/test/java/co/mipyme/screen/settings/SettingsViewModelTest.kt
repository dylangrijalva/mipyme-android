package co.mipyme.screen.settings

import arrow.core.left
import arrow.core.right
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.Location
import co.mipyme.contract.SyncEmployee
import co.mipyme.data.employee.CurrentEmployeeRepository
import co.mipyme.data.location.LocationSelectionRepository
import co.mipyme.data.theme.ThemeMode
import co.mipyme.data.theme.ThemeRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.FakePreferencesDataStore
import co.mipyme.testing.location
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dataScope = CoroutineScope(dispatcher + SupervisorJob())

    private val owner = syncEmployee("emp_1", emptyList(), isOwner = true)
    private val cashier = syncEmployee("emp_1", emptyList())
    private val singleLocationCashier = syncEmployee("emp_1", listOf("loc_1"))
    private val multiLocationCashier = syncEmployee("emp_1", listOf("loc_1", "loc_2"))
    private val downtown = location("loc_1", name = "Downtown")
    private val airport = location("loc_2", name = "Airport")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        dataScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `theme click shows the sheet and dismissing hides it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertFalse(viewModel.state.isThemeSheetVisible)

        viewModel.onThemeClick()
        assertTrue(viewModel.state.isThemeSheetVisible)

        viewModel.onThemeSheetDismissed()
        assertFalse(viewModel.state.isThemeSheetVisible)
    }

    @Test
    fun `selecting a different theme persists it and hides the sheet`() = runTest(dispatcher) {
        val themeRepository = ThemeRepository(FakePreferencesDataStore())
        val viewModel = viewModel(themeRepository = themeRepository)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.state.themeMode)
        viewModel.onThemeClick()

        viewModel.onThemeSelected(ThemeMode.DARK)
        advanceUntilIdle()

        assertFalse(viewModel.state.isThemeSheetVisible)
        assertEquals(ThemeMode.DARK, themeRepository.themeModeFlow.first())
        assertEquals(ThemeMode.DARK, viewModel.state.themeMode)
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `selecting the current theme hides the sheet and keeps the stored mode`() = runTest(dispatcher) {
        val themeRepository = ThemeRepository(FakePreferencesDataStore())
        themeRepository.setThemeMode(ThemeMode.DARK)
        val viewModel = viewModel(themeRepository = themeRepository)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.state.themeMode)
        viewModel.onThemeClick()

        viewModel.onThemeSelected(ThemeMode.DARK)
        advanceUntilIdle()

        assertFalse(viewModel.state.isThemeSheetVisible)
        assertEquals(ThemeMode.DARK, themeRepository.themeModeFlow.first())
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `a failed theme change shows the error toast and keeps the theme`() = runTest(dispatcher) {
        val themeRepository = ThemeRepository(FakePreferencesDataStore(failWrites = true))
        val viewModel = viewModel(themeRepository = themeRepository)
        advanceUntilIdle()

        viewModel.onThemeSelected(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(R.string.settings_theme_error, viewModel.state.pendingToastRes)
        assertEquals(ThemeMode.SYSTEM, viewModel.state.themeMode)
    }

    @Test
    fun `theme mode mirrors repository changes`() = runTest(dispatcher) {
        val themeRepository = ThemeRepository(FakePreferencesDataStore())
        val viewModel = viewModel(themeRepository = themeRepository)
        advanceUntilIdle()

        themeRepository.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.state.themeMode)
    }

    @Test
    fun `sign out click shows the confirmation`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onSignOutClick()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `dismissing hides the confirmation without signing out`() = runTest(dispatcher) {
        val authService = FakeAuthService(signOutResult = Unit.right())
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, authService.signOutCalls)
    }

    @Test
    fun `confirming signs out once and hides the confirmation`() = runTest(dispatcher) {
        val authService = FakeAuthService(signOutResult = Unit.right())
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        assertNull(viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `failed sign out shows the mapped error toast`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = FakeAuthService(signOutResult = AuthError.Network.left()))
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSigningOut)
        assertFalse(viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `double tap while signing out calls sign out once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(signOutGate = gate)
        val viewModel = viewModel(authService = authService)
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, authService.signOutCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `signing out is true while the call is suspended and false afterwards`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(authService = FakeAuthService(signOutGate = gate))
        viewModel.onSignOutClick()

        viewModel.onSignOutConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.state.isSigningOut)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.isSigningOut)
    }

    @Test
    fun `dismissing while signing out keeps the confirmation visible`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(authService = FakeAuthService(signOutGate = gate))
        viewModel.onSignOutClick()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        viewModel.onSignOutDismissed()

        assertTrue(viewModel.state.isSignOutConfirmationVisible)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `owner items remain after the session is no longer authenticated`() = runTest(dispatcher) {
        val authService = authService()
        val viewModel = viewModel(employees = listOf(owner), authService = authService)
        advanceUntilIdle()

        authService.authState.value = AuthState.SessionExpired
        advanceUntilIdle()

        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = viewModel(authService = FakeAuthService(signOutResult = AuthError.Network.left()))

        viewModel.onSignOutClick()
        viewModel.onSignOutConfirmed()
        advanceUntilIdle()
        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `visible items default to profile only`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)
    }

    @Test
    fun `an owner sees every item`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner))

        advanceUntilIdle()

        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    @Test
    fun `a non owner only sees profile`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(cashier))

        advanceUntilIdle()

        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)
    }

    @Test
    fun `visible items follow employee changes`() = runTest(dispatcher) {
        val cache = FakeMipymeCache(employees = listOf(owner))
        val viewModel = viewModel(cache = cache)
        advanceUntilIdle()

        cache.employees = listOf(cashier)
        advanceUntilIdle()
        assertEquals(listOf(SettingsItem.PROFILE), viewModel.state.visibleItems)

        cache.employees = listOf(owner)
        advanceUntilIdle()
        assertEquals(SettingsItem.entries, viewModel.state.visibleItems)
    }

    @Test
    fun `an owner with one location can switch location`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(owner), locations = listOf(downtown))

        advanceUntilIdle()

        assertTrue(viewModel.state.canSwitchLocation)
    }

    @Test
    fun `a non owner with one location cannot switch location`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(singleLocationCashier), locations = listOf(downtown, airport))

        advanceUntilIdle()

        assertFalse(viewModel.state.canSwitchLocation)
    }

    @Test
    fun `a non owner with several locations can switch location`() = runTest(dispatcher) {
        val viewModel = viewModel(employees = listOf(multiLocationCashier), locations = listOf(downtown, airport))

        advanceUntilIdle()

        assertTrue(viewModel.state.canSwitchLocation)
    }

    @Test
    fun `switching location clears the selected location`() = runTest(dispatcher) {
        val authService = authService()
        val cache = FakeMipymeCache(locations = listOf(downtown, airport), employees = listOf(owner))
        val repository = locationRepository(authService, cache)
        val viewModel = viewModel(cache = cache, authService = authService, locationRepository = repository)
        advanceUntilIdle()
        assertTrue(repository.select("loc_2").isRight())
        advanceUntilIdle()
        assertEquals(airport, repository.selectedLocationFlow.first())

        viewModel.onSwitchLocationClick()
        advanceUntilIdle()

        assertNull(repository.selectedLocationFlow.first())
        assertFalse(viewModel.state.isSwitchingLocation)
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `a failed switch shows the error toast`() = runTest(dispatcher) {
        val authService = authService()
        val cache = FakeMipymeCache(locations = listOf(downtown, airport), employees = listOf(owner))
        val repository = locationRepository(authService, cache, FakePreferencesDataStore(failWrites = true))
        val viewModel = viewModel(cache = cache, authService = authService, locationRepository = repository)
        advanceUntilIdle()

        viewModel.onSwitchLocationClick()
        advanceUntilIdle()

        assertEquals(R.string.settings_switch_location_error, viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isSwitchingLocation)
    }

    private fun viewModel(
        employees: List<SyncEmployee> = emptyList(),
        locations: List<Location> = emptyList(),
        cache: FakeMipymeCache = FakeMipymeCache(locations = locations, employees = employees),
        authService: FakeAuthService = authService(),
        locationRepository: LocationSelectionRepository = locationRepository(authService, cache),
        themeRepository: ThemeRepository = ThemeRepository(FakePreferencesDataStore()),
    ): SettingsViewModel {
        return SettingsViewModel(
            authService,
            CurrentEmployeeRepository(authService, cache, dataScope),
            locationRepository,
            themeRepository,
        )
    }

    private fun locationRepository(
        authService: FakeAuthService,
        cache: FakeMipymeCache,
        dataStore: FakePreferencesDataStore = FakePreferencesDataStore(),
    ): LocationSelectionRepository {
        return LocationSelectionRepository(
            dataStore = dataStore,
            authService = authService,
            cache = cache,
            scope = dataScope,
        )
    }

    private fun authService(): FakeAuthService {
        return FakeAuthService(
            AuthState.Authenticated(
                IdentityAccount(
                    id = "user_1",
                    name = "Ada Lovelace",
                    email = null,
                    pictureUrl = "https://img.example.com/ada.png",
                    publicMetadata = IdentityPublicMetadata(employeeId = "emp_1"),
                ),
            ),
        )
    }
}
