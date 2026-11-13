package co.mipyme.screen.onboarding

import androidx.lifecycle.SavedStateHandle
import arrow.core.left
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.data.onboarding.OnboardingRepository
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeApi
import co.mipyme.testing.FakeResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val defaultLocale = Locale.getDefault()

    private val merchantJson = """
        {"id":"merch_1","name":"Straw Hat Pirates","country":"ni",
         "created_at":"2026-10-22T10:00:00Z","updated_at":"2026-10-22T10:00:00Z"}
    """.trimIndent()

    private val account = IdentityAccount(
        id = "user_1",
        name = "Monkey D. Luffy",
        email = "luffy@example.com",
        pictureUrl = "",
        publicMetadata = IdentityPublicMetadata(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
    }

    private class Harness(
        val viewModel: OnboardingViewModel,
        val authService: FakeAuthService,
        val fakeApi: FakeMipymeApi,
    )

    private fun harness(
        authService: FakeAuthService = FakeAuthService(AuthState.Authenticated(account)),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        respond: suspend () -> FakeResponse = { FakeResponse(HttpStatusCode.OK, merchantJson) },
    ): Harness {
        val fakeApi = FakeMipymeApi { respond() }
        val viewModel = OnboardingViewModel(
            savedStateHandle = savedStateHandle,
            repository = OnboardingRepository(fakeApi.api, authService),
            authService = authService,
        )
        return Harness(viewModel, authService, fakeApi)
    }

    private fun OnboardingViewModel.completeAllSteps() {
        onContinue()
        onBusinessNameChange("Straw Hat Pirates")
        onContinue()
        onBusinessTypeSelected(BusinessType.GROCERY_STORE)
        onContinue()
        onCountrySelected(CountryIsoCode.MX)
    }

    @Test
    fun `starts on the name step prefilled with the signed in account name`() = runTest(dispatcher) {
        val viewModel = harness().viewModel

        assertEquals("Monkey D. Luffy", viewModel.state.name)
        assertEquals(OnboardingStep.NAME, viewModel.state.step)
    }

    @Test
    fun `name starts empty when no account is signed in`() = runTest(dispatcher) {
        val viewModel = harness(FakeAuthService(AuthState.Unauthenticated)).viewModel

        assertEquals("", viewModel.state.name)
    }

    @Test
    fun `business type starts as Convenience Store`() = runTest(dispatcher) {
        assertEquals(BusinessType.CONVENIENCE_STORE, harness().viewModel.state.businessType)
    }

    @Test
    fun `country starts as the device country when it is supported`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("es-GT"))

        assertEquals(CountryIsoCode.GT, harness().viewModel.state.country)
    }

    @Test
    fun `country starts as Mexico when the device country is Mexico`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("es-MX"))

        assertEquals(CountryIsoCode.MX, harness().viewModel.state.country)
    }

    @Test
    fun `country starts as Nicaragua when the device country is supported but hidden`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("es-AR"))

        assertEquals(CountryIsoCode.NI, harness().viewModel.state.country)
    }

    @Test
    fun `country starts as Nicaragua when the device country is not supported`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("en-US"))

        assertEquals(CountryIsoCode.NI, harness().viewModel.state.country)
    }

    @Test
    fun `country starts as Nicaragua instead of the ZZ placeholder region`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("es-ZZ"))

        assertEquals(CountryIsoCode.NI, harness().viewModel.state.country)
    }

    @Test
    fun `continue stays on the name step until a non-blank name is entered`() = runTest(dispatcher) {
        val viewModel = harness(FakeAuthService(AuthState.Unauthenticated)).viewModel

        viewModel.onContinue()
        assertEquals(OnboardingStep.NAME, viewModel.state.step)

        viewModel.onNameChange("   ")
        viewModel.onContinue()
        assertEquals(OnboardingStep.NAME, viewModel.state.step)

        viewModel.onNameChange("Luffy")
        viewModel.onContinue()
        assertEquals(OnboardingStep.BUSINESS_NAME, viewModel.state.step)
    }

    @Test
    fun `finish button stays disabled on the country step while business creation is turned off`() = runTest(dispatcher) {
        val viewModel = harness().viewModel

        assertEquals(true, viewModel.state.isContinueEnabled)

        viewModel.completeAllSteps()

        assertEquals(OnboardingStep.COUNTRY, viewModel.state.step)
        assertEquals(false, viewModel.state.isContinueEnabled)
    }

    @Test
    fun `name is valid up to 64 characters after trimming and flagged as too long beyond that`() = runTest(dispatcher) {
        val viewModel = harness().viewModel

        viewModel.onNameChange(" " + "a".repeat(64) + " ")
        assertTrue(viewModel.state.isStepValid)
        assertFalse(viewModel.state.isNameTooLong)

        viewModel.onNameChange("a".repeat(65))
        assertFalse(viewModel.state.isStepValid)
        assertTrue(viewModel.state.isNameTooLong)
    }

    @Test
    fun `continue stays on the business name step while the business name is blank or too long`() = runTest(dispatcher) {
        val viewModel = harness().viewModel
        viewModel.onContinue()

        viewModel.onContinue()
        assertEquals(OnboardingStep.BUSINESS_NAME, viewModel.state.step)

        viewModel.onBusinessNameChange("a".repeat(65))
        viewModel.onContinue()
        assertEquals(OnboardingStep.BUSINESS_NAME, viewModel.state.step)
        assertTrue(viewModel.state.isBusinessNameTooLong)

        viewModel.onBusinessNameChange("Straw Hat Pirates")
        viewModel.onContinue()
        assertEquals(OnboardingStep.BUSINESS_TYPE, viewModel.state.step)
    }

    @Test
    fun `back returns to the previous step and keeps what was typed`() = runTest(dispatcher) {
        val viewModel = harness().viewModel
        viewModel.onContinue()
        viewModel.onBusinessNameChange("Straw Hat Pirates")
        viewModel.onContinue()

        viewModel.onBack()

        assertEquals(OnboardingStep.BUSINESS_NAME, viewModel.state.step)
        assertEquals("Straw Hat Pirates", viewModel.state.businessName)
    }

    @Test
    fun `back on the name step signs out without confirmation when nothing was typed`() = runTest(dispatcher) {
        val harness = harness()

        harness.viewModel.onBack()
        advanceUntilIdle()

        assertEquals(1, harness.authService.signOutCalls)
        assertFalse(harness.viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `sign out asks for confirmation when the name differs from the prefilled one`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.onNameChange("Luffy")

        harness.viewModel.onSignOutClick()
        advanceUntilIdle()

        assertTrue(harness.viewModel.state.isSignOutConfirmationVisible)
        assertEquals(0, harness.authService.signOutCalls)
    }

    @Test
    fun `sign out asks for confirmation when a business name was typed`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.onContinue()
        harness.viewModel.onBusinessNameChange("Straw Hat Pirates")
        harness.viewModel.onBack()

        harness.viewModel.onSignOutClick()

        assertTrue(harness.viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `confirming sign out signs out and hides the dialog`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.onNameChange("Luffy")
        harness.viewModel.onSignOutClick()

        harness.viewModel.onSignOutConfirmed()
        advanceUntilIdle()

        assertEquals(1, harness.authService.signOutCalls)
        assertFalse(harness.viewModel.state.isSignOutConfirmationVisible)
        assertFalse(harness.viewModel.state.isSigningOut)
    }

    @Test
    fun `dismissing sign out hides the dialog and stays signed in`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.onNameChange("Luffy")
        harness.viewModel.onSignOutClick()

        harness.viewModel.onSignOutDismissed()
        advanceUntilIdle()

        assertEquals(0, harness.authService.signOutCalls)
        assertFalse(harness.viewModel.state.isSignOutConfirmationVisible)
    }

    @Test
    fun `sign out failure shows the auth error toast and ends the signing out state`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            initialState = AuthState.Authenticated(account),
            signOutResult = AuthError.Network.left(),
        )
        val harness = harness(authService)

        harness.viewModel.onSignOutClick()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), harness.viewModel.state.pendingToastRes)
        assertFalse(harness.viewModel.state.isSigningOut)
    }

    @Test
    fun `typing and navigation are ignored while sign out is in progress`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(AuthState.Authenticated(account), signOutGate = gate)
        val harness = harness(authService)
        harness.viewModel.onSignOutClick()
        advanceUntilIdle()
        assertTrue(harness.viewModel.state.isSigningOut)

        harness.viewModel.onNameChange("Zoro")
        harness.viewModel.onContinue()
        harness.viewModel.onSignOutClick()
        advanceUntilIdle()

        assertEquals("Monkey D. Luffy", harness.viewModel.state.name)
        assertEquals(OnboardingStep.NAME, harness.viewModel.state.step)
        assertEquals(1, authService.signOutCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `finish submits the trimmed answers to the onboarding endpoint with an idempotency key`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.onNameChange("  Monkey D. Luffy ")
        harness.viewModel.completeAllSteps()
        harness.viewModel.onBusinessNameChange("  Straw Hat Pirates ")
        harness.viewModel.onContinue()

        advanceUntilIdle()

        val sent = harness.fakeApi.requests.single()
        assertEquals("/v1/onboarding", sent.url.encodedPath)
        assertTrue(sent.headers["Idempotency-Key"].orEmpty().isNotBlank())
        val body = (sent.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertEquals(
            """{"name":"Monkey D. Luffy","country":"mx","location":{"name":"Straw Hat Pirates","business_type":"grocery_store"}}""",
            body,
        )
    }

    @Test
    fun `successful onboarding refreshes the account without showing a toast`() = runTest(dispatcher) {
        val harness = harness()
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(1, harness.authService.refreshCalls)
        assertNull(harness.viewModel.state.pendingToastRes)
        assertFalse(harness.viewModel.state.isSubmitting)
    }

    @Test
    fun `retrying after the view model is recreated reuses the saved idempotency key`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle()
        val offline = harness(savedStateHandle = savedStateHandle, respond = { throw IOException("offline") })
        offline.viewModel.completeAllSteps()
        offline.viewModel.onContinue()
        advanceUntilIdle()

        val restored = harness(savedStateHandle = savedStateHandle)
        restored.viewModel.completeAllSteps()
        restored.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(
            offline.fakeApi.requests.single().headers["Idempotency-Key"],
            restored.fakeApi.requests.single().headers["Idempotency-Key"],
        )
    }

    @Test
    fun `changing an answer after a network failure sends a new idempotency key`() = runTest(dispatcher) {
        var offline = true
        val harness = harness(respond = {
            if (offline) throw IOException("offline") else FakeResponse(HttpStatusCode.OK, merchantJson)
        })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        offline = false
        harness.viewModel.onCountrySelected(CountryIsoCode.CO)
        harness.viewModel.onContinue()
        advanceUntilIdle()

        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun `retrying after a server error sends a new idempotency key`() = runTest(dispatcher) {
        val body = """{"code":"internal_server_error","message":"Boom","details":[],"request_id":"req_1","status":500}"""
        val harness = harness(respond = { FakeResponse(HttpStatusCode.InternalServerError, body) })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun `retrying while the server is still processing reuses the key and shows the network toast`() = runTest(dispatcher) {
        val body = """{"code":"idempotency_request_in_progress","message":"Wait","details":[],"request_id":"req_1","status":409}"""
        val harness = harness(respond = { FakeResponse(HttpStatusCode.Conflict, body) })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        assertEquals(R.string.onboarding_error_network, harness.viewModel.state.pendingToastRes)
    }

    @Test
    fun `idempotency key mismatch retries once with a new key and succeeds without a toast`() = runTest(dispatcher) {
        val mismatch = """{"code":"idempotency_key_mismatch","message":"Used","details":[],"request_id":"req_1","status":409}"""
        var calls = 0
        val harness = harness(respond = {
            if (++calls == 1) FakeResponse(HttpStatusCode.Conflict, mismatch) else FakeResponse(HttpStatusCode.OK, merchantJson)
        })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertNotEquals(keys[0], keys[1])
        assertNull(harness.viewModel.state.pendingToastRes)
        assertEquals(1, harness.authService.refreshCalls)
    }

    @Test
    fun `idempotency key mismatch on the retry stops retrying and shows the generic toast`() = runTest(dispatcher) {
        val mismatch = """{"code":"idempotency_key_mismatch","message":"Used","details":[],"request_id":"req_1","status":409}"""
        val harness = harness(respond = { FakeResponse(HttpStatusCode.Conflict, mismatch) })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(2, harness.fakeApi.requests.size)
        assertEquals(R.string.onboarding_error_unknown, harness.viewModel.state.pendingToastRes)
        assertFalse(harness.viewModel.state.isSubmitting)
    }

    @Test
    fun `account refresh failure shows the network toast and the retry reuses the same key`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            initialState = AuthState.Authenticated(account),
            refreshResult = AuthError.Network.left(),
        )
        val harness = harness(authService)
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(R.string.onboarding_error_network, harness.viewModel.state.pendingToastRes)
        assertEquals(OnboardingStep.COUNTRY, harness.viewModel.state.step)
        assertFalse(harness.viewModel.state.isSubmitting)

        harness.viewModel.onToastShown()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        assertEquals(2, authService.refreshCalls)
    }

    @Test
    fun `signing in as a different account resets the form and discards the idempotency key`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val harness = harness(authService, respond = { throw IOException("offline") })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onBusinessNameChange("Straw Hat Pirates")
        harness.viewModel.onContinue()
        advanceUntilIdle()

        authService.authState.value = AuthState.Unauthenticated
        authService.authState.value = AuthState.Authenticated(account.copy(id = "user_2"))
        advanceUntilIdle()

        assertEquals(
            OnboardingScreenUiState(name = "Monkey D. Luffy", country = harness.viewModel.state.country),
            harness.viewModel.state,
        )
        // The second account submits exactly the same values, so only a cleared key can differ.
        harness.viewModel.completeAllSteps()
        harness.viewModel.onBusinessNameChange("Straw Hat Pirates")
        harness.viewModel.onContinue()
        advanceUntilIdle()
        val keys = harness.fakeApi.requests.map { it.headers["Idempotency-Key"] }
        assertEquals(2, keys.size)
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun `signing out resets the form before any account signs in`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val harness = harness(authService)
        harness.viewModel.onContinue()
        harness.viewModel.onBusinessNameChange("Straw Hat Pirates")

        authService.authState.value = AuthState.Unauthenticated
        advanceUntilIdle()

        assertEquals("", harness.viewModel.state.name)
        assertEquals("", harness.viewModel.state.businessName)
        assertEquals(OnboardingStep.NAME, harness.viewModel.state.step)
    }

    @Test
    fun `refreshing the same account keeps the form progress`() = runTest(dispatcher) {
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val harness = harness(authService)
        harness.viewModel.onContinue()
        harness.viewModel.onBusinessNameChange("Straw Hat Pirates")

        authService.authState.value = AuthState.Authenticated(account.copy(name = "Luffy"))
        advanceUntilIdle()

        assertEquals(OnboardingStep.BUSINESS_NAME, harness.viewModel.state.step)
        assertEquals("Straw Hat Pirates", harness.viewModel.state.businessName)
        assertEquals("Monkey D. Luffy", harness.viewModel.state.name)
    }

    @Test
    fun `switching accounts cancels the in flight submission without showing its result`() = runTest(dispatcher) {
        val gate = CompletableDeferred<FakeResponse>()
        val authService = FakeAuthService(AuthState.Authenticated(account))
        val harness = harness(authService, respond = { gate.await() })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onContinue()
        advanceUntilIdle()
        assertTrue(harness.viewModel.state.isSubmitting)

        authService.authState.value = AuthState.Authenticated(account.copy(id = "user_2"))
        advanceUntilIdle()
        gate.complete(FakeResponse(HttpStatusCode.OK, "{}"))
        advanceUntilIdle()

        assertFalse(harness.viewModel.state.isSubmitting)
        assertNull(harness.viewModel.state.pendingToastRes)
    }

    @Test
    fun `while submitting the form ignores input and sends no second request`() = runTest(dispatcher) {
        val gate = CompletableDeferred<FakeResponse>()
        val harness = harness(respond = { gate.await() })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()
        harness.viewModel.onContinue()
        harness.viewModel.onCountrySelected(CountryIsoCode.CO)
        harness.viewModel.onBack()
        advanceUntilIdle()

        assertTrue(harness.viewModel.state.isSubmitting)
        assertEquals(CountryIsoCode.MX, harness.viewModel.state.country)
        assertEquals(OnboardingStep.COUNTRY, harness.viewModel.state.step)
        assertEquals(1, harness.fakeApi.requests.size)
        gate.complete(FakeResponse(HttpStatusCode.OK, merchantJson))
        advanceUntilIdle()
        assertFalse(harness.viewModel.state.isSubmitting)
    }

    @Test
    fun `network failure shows the network toast and the retry reuses the same key`() = runTest(dispatcher) {
        var offline = true
        val harness = harness(respond = {
            if (offline) throw IOException("offline") else FakeResponse(HttpStatusCode.OK, merchantJson)
        })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(R.string.onboarding_error_network, harness.viewModel.state.pendingToastRes)
        assertFalse(harness.viewModel.state.isSubmitting)

        offline = false
        harness.viewModel.onToastShown()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertNull(harness.viewModel.state.pendingToastRes)
        assertEquals(2, harness.fakeApi.requests.size)
        assertEquals(
            harness.fakeApi.requests[0].headers["Idempotency-Key"],
            harness.fakeApi.requests[1].headers["Idempotency-Key"],
        )
    }

    @Test
    fun `validation error returns to the step of the invalid field and shows the validation toast`() = runTest(dispatcher) {
        val details = """[{"code":"too_small","message":"Too short","field":"location.name"}]"""
        val body = """{"code":"validation_error","message":"Bad","details":$details,"request_id":"req_1","status":400}"""
        val harness = harness(respond = { FakeResponse(HttpStatusCode.BadRequest, body) })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(OnboardingStep.BUSINESS_NAME, harness.viewModel.state.step)
        assertEquals(R.string.onboarding_error_validation, harness.viewModel.state.pendingToastRes)
    }

    @Test
    fun `employee conflict shows its toast and stays on the country step`() = runTest(dispatcher) {
        val body = """{"code":"conflict_error","message":"Nope","details":[],"request_id":"req_1","status":409}"""
        val harness = harness(respond = { FakeResponse(HttpStatusCode.Conflict, body) })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(R.string.onboarding_error_employee_conflict, harness.viewModel.state.pendingToastRes)
        assertEquals(OnboardingStep.COUNTRY, harness.viewModel.state.step)
    }

    @Test
    fun `malformed response shows the generic error toast`() = runTest(dispatcher) {
        val harness = harness(respond = { FakeResponse(HttpStatusCode.OK, "{}") })
        harness.viewModel.completeAllSteps()

        harness.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(R.string.onboarding_error_unknown, harness.viewModel.state.pendingToastRes)
    }

    @Test
    fun `marking the toast as shown clears it`() = runTest(dispatcher) {
        val harness = harness(respond = { FakeResponse(HttpStatusCode.OK, "{}") })
        harness.viewModel.completeAllSteps()
        harness.viewModel.onContinue()
        advanceUntilIdle()

        harness.viewModel.onToastShown()

        assertNull(harness.viewModel.state.pendingToastRes)
    }
}
