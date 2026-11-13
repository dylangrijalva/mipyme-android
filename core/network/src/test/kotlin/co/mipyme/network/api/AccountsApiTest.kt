package co.mipyme.network.api

import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.JsonWebTokenPayload
import co.mipyme.contract.Merchant
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.contract.request.OnboardLocationRequest
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.TEST_TOKEN
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import co.mipyme.network.recordingApi
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AccountsApiTest {
    private val onboardRequest = OnboardAccountRequest(
        name = "Monkey D. Luffy",
        country = CountryIsoCode.NI,
        location = OnboardLocationRequest(name = "Straw Hat Pirates", businessType = BusinessType.GROCERY_STORE),
    )

    @Test
    fun `current user gets the unversioned me path with the bearer token`() {
        runTest {
            val example = OpenApiSpec.example("JsonWebTokenPayload")
            recordingApi(example.toString()).use { recording ->
                val claims = recording.api.accounts.currentUser().getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(JsonWebTokenPayload.serializer(), example), claims)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/me", recording.request.url.encodedPath)
                assertEquals("Bearer $TEST_TOKEN", recording.request.headers[HttpHeaders.Authorization])
            }
        }
    }

    @Test
    fun `onboard posts snake case json with the idempotency key`() {
        runTest {
            recordingApi(OpenApiSpec.example("Merchant").toString()).use { recording ->
                recording.api.accounts.onboard(onboardRequest, idempotencyKey = "onboard-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/onboarding", sent.url.encodedPath)
                assertEquals("onboard-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf(
                        "name" to JsonPrimitive("Monkey D. Luffy"),
                        "country" to JsonPrimitive("ni"),
                        "location" to jsonOf(
                            "name" to JsonPrimitive("Straw Hat Pirates"),
                            "business_type" to JsonPrimitive("grocery_store"),
                        ),
                    ),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `onboard omits the idempotency key header when none is given`() {
        runTest {
            recordingApi(OpenApiSpec.example("Merchant").toString()).use { recording ->
                recording.api.accounts.onboard(onboardRequest)

                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `onboard omits a default business type`() {
        runTest {
            val request = onboardRequest.copy(location = OnboardLocationRequest(name = "Straw Hat Pirates"))
            recordingApi(OpenApiSpec.example("Merchant").toString()).use { recording ->
                recording.api.accounts.onboard(request)

                assertEquals(setOf("name"), recording.request.jsonBody().getValue("location").jsonObject.keys)
            }
        }
    }

    @Test
    fun `onboard decodes the merchant response`() {
        runTest {
            val example = OpenApiSpec.example("Merchant")
            recordingApi(example.toString()).use { recording ->
                val merchant = recording.api.accounts.onboard(onboardRequest).getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Merchant.serializer(), example), merchant)
                assertEquals(example.jsonObject.getValue("id").jsonPrimitive.content, merchant?.id)
            }
        }
    }
}
