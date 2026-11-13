package co.mipyme.network.api

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.PaymentMethod
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.Sale
import co.mipyme.contract.SaleStatus
import co.mipyme.contract.Source
import co.mipyme.contract.request.CreateSaleLineItem
import co.mipyme.contract.request.CreateSalePayment
import co.mipyme.contract.request.CreateSaleRequest
import co.mipyme.contract.request.SalePaginationRequest
import co.mipyme.contract.request.VoidSaleRequest
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SalesApiTest {
    private val example = OpenApiSpec.example("Sale")

    private val request = CreateSaleRequest(
        source = Source.POS_ANDROID,
        locationId = ObjectId("loc_water7"),
        businessDate = LocalDateTime.of(2026, 10, 22, 10, 0),
        customerId = ObjectId("cust_nami"),
        lineItems = listOf(CreateSaleLineItem(ObjectId("var_tangerines"), Decimal("125.50"), 2)),
        payments = listOf(CreateSalePayment(PaymentMethod.CASH, PositiveDecimal("251.00"))),
    )

    @Test
    fun `create posts snake case json with decimal strings and the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.sales.create(request, idempotencyKey = "sale-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/sales", sent.url.encodedPath)
                assertEquals("sale-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf(
                        "source" to JsonPrimitive("pos.android"),
                        "location_id" to JsonPrimitive("loc_water7"),
                        "business_date" to JsonPrimitive("2026-10-22T10:00:00Z"),
                        "customer_id" to JsonPrimitive("cust_nami"),
                        "line_items" to JsonArray(
                            listOf(
                                jsonOf(
                                    "variant_id" to JsonPrimitive("var_tangerines"),
                                    "price" to JsonPrimitive("125.50"),
                                    "quantity" to JsonPrimitive(2),
                                ),
                            ),
                        ),
                        "payments" to JsonArray(
                            listOf(
                                jsonOf(
                                    "method" to JsonPrimitive("cash"),
                                    "amount" to JsonPrimitive("251.00"),
                                ),
                            ),
                        ),
                    ),
                    sent.jsonBody(),
                )
                assertTrue(sent.jsonBody().getValue("line_items").jsonArray.single().jsonObject.getValue("price").jsonPrimitive.isString)
            }
        }
    }

    @Test
    fun `create omits the default source and unset optional fields`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.sales.create(request.copy(source = Source.POS_WEB, customerId = null))

                val body = recording.request.jsonBody()
                assertFalse("source" in body)
                assertFalse("customer_id" in body)
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `list sends only the default limit without filters`() {
        runTest {
            recordingApi(page(example)).use { recording ->
                recording.api.sales.list()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/sales", sent.url.encodedPath)
                assertEquals(setOf("limit"), sent.url.parameters.names())
                assertEquals("50", sent.url.parameters["limit"])
            }
        }
    }

    @Test
    fun `list maps every filter to a snake case query parameter`() {
        runTest {
            val filters = SalePaginationRequest(
                limit = 20,
                cursor = "opaque-cursor",
                locationId = ObjectId("loc_water7"),
                customerId = ObjectId("cust_nami"),
                status = SaleStatus.PENDING,
                from = LocalDateTime.of(2026, 10, 1, 0, 0),
                to = LocalDateTime.of(2026, 10, 22, 23, 59, 59),
            )
            recordingApi(page(example)).use { recording ->
                recording.api.sales.list(filters)

                val parameters = recording.request.url.parameters
                assertEquals(
                    setOf("limit", "cursor", "location_id", "customer_id", "status", "from", "to"),
                    parameters.names(),
                )
                assertEquals("20", parameters["limit"])
                assertEquals("opaque-cursor", parameters["cursor"])
                assertEquals("loc_water7", parameters["location_id"])
                assertEquals("cust_nami", parameters["customer_id"])
                assertEquals("pending", parameters["status"])
                assertEquals("2026-10-01T00:00:00Z", parameters["from"])
                assertEquals("2026-10-22T23:59:59Z", parameters["to"])
            }
        }
    }

    @Test
    fun `list decodes the paginated response`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.sales.list().getOrNull()

                val sale = MipymeJson.decodeFromJsonElement(Sale.serializer(), example)
                assertEquals(PaginationResponse(listOf(sale), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the sale by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.sales.find("sale_1").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Sale.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/sales/sale_1", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `void posts the reason with the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.sales.void("sale_1", VoidSaleRequest("Damaged package"), "void-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/sales/sale_1/void", sent.url.encodedPath)
                assertEquals("void-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(jsonOf("reason" to JsonPrimitive("Damaged package")), sent.jsonBody())
            }
        }
    }

    @Test
    fun `void without a reason sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.sales.void("sale_1")

                assertEquals(jsonOf(), recording.request.jsonBody())
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }
}
