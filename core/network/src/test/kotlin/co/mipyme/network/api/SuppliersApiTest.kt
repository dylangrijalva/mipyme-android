package co.mipyme.network.api

import co.mipyme.contract.Supplier
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateSupplierRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateSupplierRequest
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

class SuppliersApiTest {
    private val example = OpenApiSpec.example("Supplier")

    @Test
    fun `create posts snake case json with the idempotency key`() {
        runTest {
            val request = CreateSupplierRequest(name = "Red Hair Pirates", email = "contact@redhairpirates.example.com")
            recordingApi(example.toString()).use { recording ->
                recording.api.suppliers.create(request, idempotencyKey = "suppliers-key")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/suppliers", sent.url.encodedPath)
                assertEquals("suppliers-key", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf("name" to JsonPrimitive("Red Hair Pirates"), "email" to JsonPrimitive("contact@redhairpirates.example.com")),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `create omits unset optional fields and the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.suppliers.create(CreateSupplierRequest(name = "Red Hair Pirates"))

                assertEquals(jsonOf("name" to JsonPrimitive("Red Hair Pirates")), recording.request.jsonBody())
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `list sends the limit and cursor and decodes the page`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.suppliers.list(PaginationRequest(limit = 10, cursor = "opaque-cursor")).getOrNull()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/suppliers", sent.url.encodedPath)
                assertEquals("10", sent.url.parameters["limit"])
                assertEquals("opaque-cursor", sent.url.parameters["cursor"])
                val item = MipymeJson.decodeFromJsonElement(Supplier.serializer(), example)
                assertEquals(PaginationResponse(listOf(item), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the supplier by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.suppliers.find("supp_redhair").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Supplier.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/suppliers/supp_redhair", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `update omits absent fields and sends cleared and changed fields`() {
        runTest {
            val request = UpdateSupplierRequest(
                name = OptionalField.Present("Red Hair"),
                email = OptionalField.Present(null),
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.suppliers.update("supp_redhair", request)

                assertEquals(HttpMethod.Patch, recording.request.method)
                assertEquals("/v1/suppliers/supp_redhair", recording.request.url.encodedPath)
                assertEquals(
                    jsonOf("name" to JsonPrimitive("Red Hair"), "email" to JsonNull),
                    recording.request.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `update with no changes sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.suppliers.update("supp_redhair", UpdateSupplierRequest())

                assertEquals(jsonOf(), recording.request.jsonBody())
            }
        }
    }
}
