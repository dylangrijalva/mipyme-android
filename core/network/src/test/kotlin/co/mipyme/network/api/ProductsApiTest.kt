package co.mipyme.network.api

import co.mipyme.contract.Decimal
import co.mipyme.contract.ObjectId
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.Product
import co.mipyme.contract.ProductImage
import co.mipyme.contract.ProductType
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateProductRequest
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

class ProductsApiTest {
    private val example = OpenApiSpec.example("Product")

    @Test
    fun `create posts snake case json with the idempotency key`() {
        runTest {
            val request = CreateProductRequest(
                name = "East Blue Tangerines",
                barcode = "7501234567890",
                price = PositiveDecimal("12.50"),
                unitCost = Decimal("8.00"),
                collectionId = ObjectId("col_fruit"),
                imageUrl = ProductImage("https://img.example.com/tangerines.png"),
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.products.create(request, idempotencyKey = "product-straw-hats")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/products", sent.url.encodedPath)
                assertEquals("product-straw-hats", sent.headers["Idempotency-Key"])
                assertEquals(
                    jsonOf(
                        "name" to JsonPrimitive("East Blue Tangerines"),
                        "barcode" to JsonPrimitive("7501234567890"),
                        "price" to JsonPrimitive("12.50"),
                        "unit_cost" to JsonPrimitive("8.00"),
                        "collection_id" to JsonPrimitive("col_fruit"),
                        "image_url" to JsonPrimitive("https://img.example.com/tangerines.png"),
                    ),
                    sent.jsonBody(),
                )
            }
        }
    }

    @Test
    fun `create sends a non default product type`() {
        runTest {
            val request = CreateProductRequest(
                name = "Haircut",
                price = PositiveDecimal("5.00"),
                productType = ProductType.SERVICE,
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.products.create(request)

                assertEquals(JsonPrimitive("service"), recording.request.jsonBody()["product_type"])
                assertNull(recording.request.headers["Idempotency-Key"])
            }
        }
    }

    @Test
    fun `list sends the default limit without a cursor`() {
        runTest {
            recordingApi(page(example)).use { recording ->
                recording.api.products.list()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/products", sent.url.encodedPath)
                assertEquals(setOf("limit"), sent.url.parameters.names())
                assertEquals("50", sent.url.parameters["limit"])
            }
        }
    }

    @Test
    fun `list sends the limit and cursor`() {
        runTest {
            recordingApi(page(example)).use { recording ->
                recording.api.products.list(PaginationRequest(limit = 20, cursor = "opaque-cursor"))

                assertEquals("20", recording.request.url.parameters["limit"])
                assertEquals("opaque-cursor", recording.request.url.parameters["cursor"])
            }
        }
    }

    @Test
    fun `list decodes the paginated response`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.products.list().getOrNull()

                val product = MipymeJson.decodeFromJsonElement(Product.serializer(), example)
                assertEquals(PaginationResponse(listOf(product), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the product by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.products.find("prod_tangerines").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Product.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/products/prod_tangerines", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `find encodes slashes in the id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.products.find("../sales")

                assertEquals("/v1/products/..%2Fsales", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `update omits absent fields and sends cleared and changed fields`() {
        runTest {
            val request = UpdateProductRequest(
                name = OptionalField.Present("Mikan"),
                barcode = OptionalField.Present(null),
                collectionId = OptionalField.Present(ObjectId("col_fruit")),
            )
            recordingApi(example.toString()).use { recording ->
                recording.api.products.update("prod_tangerines", request)

                assertEquals(HttpMethod.Patch, recording.request.method)
                assertEquals("/v1/products/prod_tangerines", recording.request.url.encodedPath)
                assertEquals(
                    jsonOf(
                        "name" to JsonPrimitive("Mikan"),
                        "barcode" to JsonNull,
                        "collection_id" to JsonPrimitive("col_fruit"),
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
                recording.api.products.update("prod_tangerines", UpdateProductRequest())

                assertEquals(jsonOf(), recording.request.jsonBody())
            }
        }
    }
}
