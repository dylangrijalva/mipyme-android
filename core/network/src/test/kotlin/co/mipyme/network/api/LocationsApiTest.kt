package co.mipyme.network.api

import co.mipyme.contract.BusinessType
import co.mipyme.contract.Decimal
import co.mipyme.contract.Location
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateLocationRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateLocationRequest
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import co.mipyme.network.page
import co.mipyme.network.recordingApi
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class LocationsApiTest {
    private val example = OpenApiSpec.example("Location")

    @Test
    fun `create posts snake case json with the idempotency key`() {
        runTest {
            val request = CreateLocationRequest(
                name = "Water 7",
                businessType = BusinessType.GROCERY_STORE,
                taxId = "J0310000000000",
                receiptHeaderText = "Thanks for shopping",
                exchangeRate = Decimal("36.60"),
                allowBackorders = true,
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.locations.create(request, idempotencyKey = "location-key")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/locations", sent.url.encodedPath)
                assertEquals("location-key", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf(
                        "name" to JsonPrimitive("Water 7"),
                        "business_type" to JsonPrimitive("grocery_store"),
                        "tax_id" to JsonPrimitive("J0310000000000"),
                        "receipt_header_text" to JsonPrimitive("Thanks for shopping"),
                        "exchange_rate" to JsonPrimitive("36.60"),
                        "allow_backorders" to JsonPrimitive(true),
                    ),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `create omits unset optional fields and the idempotency key`() {
        runTest {
            val request = CreateLocationRequest(name = "Water 7", businessType = BusinessType.BAKERY)
            recordingApi(example.toString()).use { recording ->
                recording.api.locations.create(request)

                assertEquals(
                    jsonOf("name" to JsonPrimitive("Water 7"), "business_type" to JsonPrimitive("bakery")),
                    recording.request.jsonBody(),
                )
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `list sends the limit and cursor and decodes the page`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.locations.list(PaginationRequest(limit = 10, cursor = "opaque-cursor")).getOrNull()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/locations", sent.url.encodedPath)
                assertEquals("10", sent.url.parameters["limit"])
                assertEquals("opaque-cursor", sent.url.parameters["cursor"])
                val location = MipymeJson.decodeFromJsonElement(Location.serializer(), example)
                assertEquals(PaginationResponse(listOf(location), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the location by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.locations.find("loc_water7").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Location.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/locations/loc_water7", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `update omits absent fields and sends cleared and changed fields`() {
        runTest {
            val request = UpdateLocationRequest(
                address = OptionalField.Present(null),
                taxRate = OptionalField.Present(Decimal("0.15")),
                allowBackorders = OptionalField.Present(true),
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.locations.update("loc_water7", request)

                assertEquals(HttpMethod.Patch, recording.request.method)
                assertEquals("/v1/locations/loc_water7", recording.request.url.encodedPath)
                assertEquals(
                    jsonOf(
                        "address" to JsonNull,
                        "tax_rate" to JsonPrimitive("0.15"),
                        "allow_backorders" to JsonPrimitive(true),
                    ),
                    recording.request.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `update with no changes sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.locations.update("loc_water7", UpdateLocationRequest())

                assertEquals(jsonOf(), recording.request.jsonBody())
            }
        }
    }
}
