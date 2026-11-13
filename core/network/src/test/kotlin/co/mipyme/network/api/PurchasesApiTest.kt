package co.mipyme.network.api

import co.mipyme.contract.ObjectId
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.Purchase
import co.mipyme.contract.PurchaseStatus
import co.mipyme.contract.request.CreatePurchaseLineItem
import co.mipyme.contract.request.CreatePurchaseRequest
import co.mipyme.contract.request.PurchasePaginationRequest
import co.mipyme.contract.request.VoidPurchaseRequest
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import co.mipyme.network.page
import co.mipyme.network.recordingApi
import io.ktor.http.HttpMethod
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class PurchasesApiTest {
    private val example = OpenApiSpec.example("Purchase")

    private val request = CreatePurchaseRequest(
        supplierId = ObjectId("supp_redhair"),
        locationId = ObjectId("loc_water7"),
        notes = "Provisions",
        lineItems = listOf(CreatePurchaseLineItem(ObjectId("var_tangerines"), 20, PositiveDecimal("80.00"))),
    )

    @Test
    fun `create posts snake case json with the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.purchases.create(request, idempotencyKey = "purchase-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/purchases", sent.url.encodedPath)
                assertEquals("purchase-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf(
                        "supplier_id" to JsonPrimitive("supp_redhair"),
                        "location_id" to JsonPrimitive("loc_water7"),
                        "notes" to JsonPrimitive("Provisions"),
                        "line_items" to JsonArray(
                            listOf(
                                jsonOf(
                                    "variant_id" to JsonPrimitive("var_tangerines"),
                                    "quantity" to JsonPrimitive(20),
                                    "unit_cost" to JsonPrimitive("80.00"),
                                ),
                            ),
                        ),
                    ),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `list sends only the default limit without filters`() {
        runTest {
            recordingApi(page(example)).use { recording ->
                recording.api.purchases.list()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/purchases", sent.url.encodedPath)
                assertEquals(setOf("limit"), sent.url.parameters.names())
                assertEquals("50", sent.url.parameters["limit"])
            }
        }
    }

    @Test
    fun `list maps every filter to a snake case query parameter`() {
        runTest {
            val filters = PurchasePaginationRequest(
                limit = 20,
                cursor = "opaque-cursor",
                locationId = ObjectId("loc_water7"),
                supplierId = ObjectId("supp_redhair"),
                status = PurchaseStatus.VOIDED,
                from = LocalDateTime.of(2026, 10, 1, 0, 0),
                to = LocalDateTime.of(2026, 10, 22, 23, 59, 59),
            )
            recordingApi(page(example)).use { recording ->
                recording.api.purchases.list(filters)

                val parameters = recording.request.url.parameters
                assertEquals(
                    setOf("limit", "cursor", "location_id", "supplier_id", "status", "from", "to"),
                    parameters.names(),
                )
                assertEquals("20", parameters["limit"])
                assertEquals("opaque-cursor", parameters["cursor"])
                assertEquals("loc_water7", parameters["location_id"])
                assertEquals("supp_redhair", parameters["supplier_id"])
                assertEquals("voided", parameters["status"])
                assertEquals("2026-10-01T00:00:00Z", parameters["from"])
                assertEquals("2026-10-22T23:59:59Z", parameters["to"])
            }
        }
    }

    @Test
    fun `list decodes the paginated response`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.purchases.list().getOrNull()

                val purchase = MipymeJson.decodeFromJsonElement(Purchase.serializer(), example)
                assertEquals(PaginationResponse(listOf(purchase), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the purchase by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.purchases.find("purchase_1").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Purchase.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/purchases/purchase_1", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `void posts the reason with the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.purchases.void("purchase_1", VoidPurchaseRequest("Wrong supplier"), "void-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/purchases/purchase_1/void", sent.url.encodedPath)
                assertEquals("void-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(jsonOf("reason" to JsonPrimitive("Wrong supplier")), sent.jsonBody())
            }
        }
    }

    @Test
    fun `void without a reason sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.purchases.void("purchase_1")

                assertEquals(jsonOf(), recording.request.jsonBody())
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }
}
