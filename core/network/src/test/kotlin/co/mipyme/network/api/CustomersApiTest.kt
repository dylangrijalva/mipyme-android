package co.mipyme.network.api

import co.mipyme.contract.Customer
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateCustomerRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateCustomerRequest
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

class CustomersApiTest {
    private val example = OpenApiSpec.example("Customer")

    @Test
    fun `create posts snake case json with the idempotency key`() {
        runTest {
            val request = CreateCustomerRequest(name = "Trafalgar Law", email = "law@heartpirates.example.com")
            recordingApi(example.toString()).use { recording ->
                recording.api.customers.create(request, idempotencyKey = "customers-key")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/customers", sent.url.encodedPath)
                assertEquals("customers-key", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf("name" to JsonPrimitive("Trafalgar Law"), "email" to JsonPrimitive("law@heartpirates.example.com")),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `create omits unset optional fields and the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.customers.create(CreateCustomerRequest(name = "Trafalgar Law"))

                assertEquals(jsonOf("name" to JsonPrimitive("Trafalgar Law")), recording.request.jsonBody())
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `list sends the limit and cursor and decodes the page`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.customers.list(PaginationRequest(limit = 10, cursor = "opaque-cursor")).getOrNull()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/customers", sent.url.encodedPath)
                assertEquals("10", sent.url.parameters["limit"])
                assertEquals("opaque-cursor", sent.url.parameters["cursor"])
                val item = MipymeJson.decodeFromJsonElement(Customer.serializer(), example)
                assertEquals(PaginationResponse(listOf(item), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the customer by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.customers.find("cust_law").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Customer.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/customers/cust_law", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `update omits absent fields and sends cleared and changed fields`() {
        runTest {
            val request = UpdateCustomerRequest(
                name = OptionalField.Present("Law"),
                email = OptionalField.Present(null),
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.customers.update("cust_law", request)

                assertEquals(HttpMethod.Patch, recording.request.method)
                assertEquals("/v1/customers/cust_law", recording.request.url.encodedPath)
                assertEquals(
                    jsonOf("name" to JsonPrimitive("Law"), "email" to JsonNull),
                    recording.request.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `update with no changes sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.customers.update("cust_law", UpdateCustomerRequest())

                assertEquals(jsonOf(), recording.request.jsonBody())
            }
        }
    }
}
