package co.mipyme.data.onboarding

import arrow.core.left
import co.mipyme.auth.AuthError
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.ErrorDetail
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.contract.request.OnboardLocationRequest
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeApi
import co.mipyme.testing.FakeResponse
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryTest {

    private val request = OnboardAccountRequest(
        name = "Monkey D. Luffy",
        country = CountryIsoCode.NI,
        location = OnboardLocationRequest(name = "Straw Hat Pirates", businessType = BusinessType.GROCERY_STORE),
    )

    private val merchantJson = """
        {"id":"merch_1","name":"Straw Hat Pirates","country":"ni",
         "created_at":"2026-10-22T10:00:00Z","updated_at":"2026-10-22T10:00:00Z"}
    """.trimIndent()

    private fun errorJson(code: String, status: Int, details: String = "[]"): String {
        return """{"code":"$code","message":"Nope","details":$details,"request_id":"req_1","status":$status}"""
    }

    @Test
    fun `success posts to the onboarding endpoint with the idempotency key and refreshes the account`() = runTest {
        val authService = FakeAuthService()
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.OK, merchantJson) }
        val repository = OnboardingRepository(fake.api, authService)

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals("merch_1", result.getOrNull()?.id)
        val sent = fake.requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/v1/onboarding", sent.url.encodedPath)
        assertEquals("key-1", sent.headers["Idempotency-Key"])
        assertEquals(1, authService.refreshCalls)
    }

    @Test
    fun `account refresh failure after the merchant is created returns AccountRefreshFailed`() = runTest {
        val authService = FakeAuthService(refreshResult = AuthError.Network.left())
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.OK, merchantJson) }
        val repository = OnboardingRepository(fake.api, authService)

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(OnboardingError.AccountRefreshFailed, result.leftOrNull())
        assertEquals(1, authService.refreshCalls)
    }

    @Test
    fun `validation error returns its field details and skips the account refresh`() = runTest {
        val details = """[{"code":"too_small","message":"Too short","field":"name"}]"""
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.BadRequest, errorJson("validation_error", 400, details)) }
        val authService = FakeAuthService()
        val repository = OnboardingRepository(fake.api, authService)

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(
            OnboardingError.Validation(listOf(ErrorDetail("too_small", "Too short", "name"))),
            result.leftOrNull(),
        )
        assertEquals(0, authService.refreshCalls)
    }

    @Test
    fun `conflict error returns EmployeeConflict`() = runTest {
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.Conflict, errorJson("conflict_error", 409)) }
        val repository = OnboardingRepository(fake.api, FakeAuthService())

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(OnboardingError.EmployeeConflict, result.leftOrNull())
    }

    @Test
    fun `idempotency key mismatch returns IdempotencyKeyMismatch`() = runTest {
        val body = errorJson("idempotency_key_mismatch", 409)
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.Conflict, body) }
        val repository = OnboardingRepository(fake.api, FakeAuthService())

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(OnboardingError.IdempotencyKeyMismatch, result.leftOrNull())
    }

    @Test
    fun `request still in progress or idempotency unavailable returns IdempotencyPending`() = runTest {
        val codes = listOf("idempotency_request_in_progress", "idempotency_unavailable")

        codes.forEach { code ->
            val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.Conflict, errorJson(code, 409)) }
            val repository = OnboardingRepository(fake.api, FakeAuthService())

            val result = repository.onboard(request, idempotencyKey = "key-1")

            assertEquals("code=$code", OnboardingError.IdempotencyPending, result.leftOrNull())
        }
    }

    @Test
    fun `unrecognized server error such as rate limiting returns Unknown`() = runTest {
        val fake = FakeMipymeApi {
            FakeResponse(HttpStatusCode.TooManyRequests, errorJson("rate_limit_exceeded", 429))
        }
        val repository = OnboardingRepository(fake.api, FakeAuthService())

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(OnboardingError.Unknown, result.leftOrNull())
    }

    @Test
    fun `malformed success response returns Unknown and skips the account refresh`() = runTest {
        val fake = FakeMipymeApi { FakeResponse(HttpStatusCode.OK, "{}") }
        val authService = FakeAuthService()
        val repository = OnboardingRepository(fake.api, authService)

        val result = repository.onboard(request, idempotencyKey = "key-1")

        assertEquals(OnboardingError.Unknown, result.leftOrNull())
        assertEquals(0, authService.refreshCalls)
    }

    @Test
    fun `offline device or connection timeout returns Network`() = runTest {
        val failures = listOf<Throwable>(IOException("offline"), ConnectTimeoutException("connect timed out"))

        failures.forEach { failure ->
            val fake = FakeMipymeApi { throw failure }
            val repository = OnboardingRepository(fake.api, FakeAuthService())

            val result = repository.onboard(request, idempotencyKey = "key-1")

            assertEquals("failure=$failure", OnboardingError.Network, result.leftOrNull())
        }
    }
}
