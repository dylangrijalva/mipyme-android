package co.mipyme.network

import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.Decimal
import co.mipyme.contract.ErrorDetail
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.ObjectId
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaymentMethod
import co.mipyme.contract.PositiveDecimal
import co.mipyme.contract.request.AuthorizeRealtimeChannelRequest
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.CreateCustomerRequest
import co.mipyme.contract.request.CreateLocationRequest
import co.mipyme.contract.request.CreateProductRequest
import co.mipyme.contract.request.CreatePurchaseLineItem
import co.mipyme.contract.request.CreatePurchaseRequest
import co.mipyme.contract.request.CreateSaleLineItem
import co.mipyme.contract.request.CreateSalePayment
import co.mipyme.contract.request.CreateSaleRequest
import co.mipyme.contract.request.CreateSupplierRequest
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.contract.request.OnboardLocationRequest
import co.mipyme.contract.request.SyncRequest
import co.mipyme.contract.request.UpdateCollectionRequest
import co.mipyme.contract.request.UpdateCustomerRequest
import co.mipyme.contract.request.UpdateLocationRequest
import co.mipyme.contract.request.UpdateProductRequest
import co.mipyme.contract.request.UpdateSupplierRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import java.io.IOException
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException

class MipymeApiTest {
    private data class Operation(
        val method: String,
        val template: String,
        val call: suspend (MipymeApi) -> Any,
    )

    private val operations = listOf(
        Operation("get", "/me") { it.accounts.currentUser() },
        Operation("post", "/v1/onboarding") {
            it.accounts.onboard(
                OnboardAccountRequest("Luffy", CountryIsoCode.NI, OnboardLocationRequest("Straw Hats")),
                "key",
            )
        },
        Operation("get", "/v1/locations") { it.locations.list() },
        Operation("post", "/v1/locations") {
            it.locations.create(CreateLocationRequest("Water 7", BusinessType.OTHER), "key")
        },
        Operation("get", "/v1/locations/{id}") { it.locations.find("resource-id") },
        Operation("patch", "/v1/locations/{id}") { it.locations.update("resource-id", UpdateLocationRequest()) },
        Operation("get", "/v1/products") { it.products.list() },
        Operation("post", "/v1/products") {
            it.products.create(CreateProductRequest("Tangerines", price = PositiveDecimal("1.00")), "key")
        },
        Operation("get", "/v1/products/{id}") { it.products.find("resource-id") },
        Operation("patch", "/v1/products/{id}") { it.products.update("resource-id", UpdateProductRequest()) },
        Operation("get", "/v1/collections") { it.collections.list() },
        Operation("post", "/v1/collections") { it.collections.create(CreateCollectionRequest("Fruit"), "key") },
        Operation("get", "/v1/collections/{id}") { it.collections.find("resource-id") },
        Operation("patch", "/v1/collections/{id}") {
            it.collections.update("resource-id", UpdateCollectionRequest(OptionalField.Present("Fruit")))
        },
        Operation("get", "/v1/suppliers") { it.suppliers.list() },
        Operation("post", "/v1/suppliers") { it.suppliers.create(CreateSupplierRequest("Red Hair"), "key") },
        Operation("get", "/v1/suppliers/{id}") { it.suppliers.find("resource-id") },
        Operation("patch", "/v1/suppliers/{id}") { it.suppliers.update("resource-id", UpdateSupplierRequest()) },
        Operation("get", "/v1/customers") { it.customers.list() },
        Operation("post", "/v1/customers") { it.customers.create(CreateCustomerRequest("Law"), "key") },
        Operation("get", "/v1/customers/{id}") { it.customers.find("resource-id") },
        Operation("patch", "/v1/customers/{id}") { it.customers.update("resource-id", UpdateCustomerRequest()) },
        Operation("get", "/v1/purchases") { it.purchases.list() },
        Operation("post", "/v1/purchases") {
            it.purchases.create(
                CreatePurchaseRequest(
                    supplierId = ObjectId("supp_1"),
                    locationId = ObjectId("loc_1"),
                    lineItems = listOf(CreatePurchaseLineItem(ObjectId("var_1"), 1, PositiveDecimal("1.00"))),
                ),
                "key",
            )
        },
        Operation("get", "/v1/purchases/{id}") { it.purchases.find("resource-id") },
        Operation("post", "/v1/purchases/{id}/void") { it.purchases.void("resource-id", idempotencyKey = "key") },
        Operation("get", "/v1/sales") { it.sales.list() },
        Operation("post", "/v1/sales") {
            it.sales.create(
                CreateSaleRequest(
                    locationId = ObjectId("loc_1"),
                    businessDate = LocalDateTime.of(2026, 10, 22, 10, 0),
                    lineItems = listOf(CreateSaleLineItem(ObjectId("var_1"), Decimal("1.00"), 1)),
                    payments = listOf(CreateSalePayment(PaymentMethod.CASH, PositiveDecimal("1.00"))),
                ),
                "key",
            )
        },
        Operation("get", "/v1/sales/{id}") { it.sales.find("resource-id") },
        Operation("post", "/v1/sales/{id}/void") { it.sales.void("resource-id", idempotencyKey = "key") },
        Operation("post", "/v1/sync") { it.sync.readResources(SyncRequest("*")) },
        Operation("post", "/v1/realtime/subscriptions") {
            it.realtime.authorizeChannel(AuthorizeRealtimeChannelRequest("123.456", "private-merch_1"))
        },
        Operation("get", "/_health") { it.isHealthy() },
    )

    @Test
    fun `client covers every operation in the specification`() {
        val covered = operations.map { it.method to it.template }.toSet()

        assertEquals(OpenApiSpec.operations, covered)
    }

    @Test
    fun `every operation sends its documented method path and idempotency key`() {
        runTest {
            for (operation in operations) {
                recordingApi().use { recording ->
                    operation.call(recording.api)

                    val sent = recording.request
                    val label = "${operation.method} ${operation.template}"
                    assertEquals(HttpMethod.parse(operation.method.uppercase()), sent.method, label)
                    assertEquals(operation.template.replace("{id}", "resource-id"), sent.url.encodedPath, label)
                    val documented = (operation.method to operation.template) in OpenApiSpec.idempotentOperations
                    assertEquals(documented, sent.headers["Idempotency-Key"] != null, label)
                }
            }
        }
    }

    @Test
    fun `requests resolve against the base url and accept json`() {
        runTest {
            recordingApi().use { recording ->
                recording.api.accounts.currentUser()

                assertEquals(Url("$BASE_URL/me"), recording.request.url)
                assertEquals("application/json", recording.request.headers[HttpHeaders.Accept])
            }
        }
    }

    @Test
    fun `authenticated requests carry the bearer token`() {
        runTest {
            recordingApi().use { recording ->
                recording.api.accounts.currentUser()

                assertEquals("Bearer $TEST_TOKEN", recording.request.headers[HttpHeaders.Authorization])
            }
        }
    }

    @Test
    fun `requests have no authorization header when there is no token`() {
        runTest {
            recordingApi(tokenProvider = AuthTokenProvider { null }).use { recording ->
                recording.api.accounts.currentUser()

                assertNull(recording.request.headers[HttpHeaders.Authorization])
            }
        }
    }

    @Test
    fun `the token provider is consulted for every request`() {
        runTest {
            var calls = 0
            recordingApi(tokenProvider = AuthTokenProvider { "token-${++calls}" }).use { recording ->
                recording.api.accounts.currentUser()
                recording.api.accounts.currentUser()

                assertEquals(
                    listOf("Bearer token-1", "Bearer token-2"),
                    recording.requests.map { it.headers[HttpHeaders.Authorization] },
                )
            }
        }
    }

    @Test
    fun `health is public and succeeds on no content`() {
        runTest {
            recordingApi(response = "", status = HttpStatusCode.NoContent).use { recording ->
                assertTrue(recording.api.isHealthy())

                assertEquals("/_health", recording.request.url.encodedPath)
                assertNull(recording.request.headers[HttpHeaders.Authorization])
            }
        }
    }

    @Test
    fun `health is false when the server reports an error`() {
        runTest {
            recordingApi(response = "{}", status = HttpStatusCode.ServiceUnavailable).use { recording ->
                assertEquals(false, recording.api.isHealthy())
            }
        }
    }

    @Test
    fun `health is false when the network is unavailable`() {
        runTest {
            failingApi(IOException("offline")).use { api ->
                assertEquals(false, api.isHealthy())
            }
        }
    }

    @Test
    fun `error responses keep the code message status request id and details`() {
        runTest {
            val example = OpenApiSpec.example("MipymeError").toString()
            recordingApi(example, HttpStatusCode.BadRequest).use { recording ->
                val error = recording.api.accounts.currentUser().leftOrNull()

                val response = assertIs<NetworkError.Response>(error).error
                assertEquals(MipymeErrorCode.STOCK_INSUFFICIENT, response.code)
                assertEquals("Not enough stock for East Blue Tangerines at Water 7.", response.message)
                assertEquals(400, response.status)
                assertTrue(response.requestId.startsWith("req_"))
                assertEquals(
                    listOf(ErrorDetail("stock_insufficient", "Requested 20 units; 18 are available.", "lineItems.0.quantity")),
                    response.details,
                )
            }
        }
    }

    @Test
    fun `error responses accept a camel case request id`() {
        runTest {
            val body = """{"requestId":"req_abc","status":401,"code":"invalid_oauth_token","message":"Provide a bearer token.","details":[]}"""
            recordingApi(body, HttpStatusCode.Unauthorized).use { recording ->
                val error = recording.api.accounts.currentUser().leftOrNull()

                val response = assertIs<NetworkError.Response>(error).error
                assertEquals("req_abc", response.requestId)
                assertEquals(MipymeErrorCode.INVALID_OAUTH_TOKEN, response.code)
            }
        }
    }

    @Test
    fun `an unreadable error body is a serialization failure`() {
        runTest {
            recordingApi("not json", HttpStatusCode.BadGateway).use { recording ->
                val error = recording.api.accounts.currentUser().leftOrNull()

                assertIs<NetworkError.Serialization>(error)
            }
        }
    }

    @Test
    fun `a malformed success body is a serialization failure`() {
        runTest {
            recordingApi("{}").use { recording ->
                val error = recording.api.accounts.currentUser().leftOrNull()

                assertIs<NetworkError.Serialization>(error)
            }
        }
    }

    @Test
    fun `request timeouts are timeout failures`() {
        runTest {
            val timeouts = listOf(
                HttpRequestTimeoutException("$BASE_URL/me", 30_000L),
                ConnectTimeoutException("connect timed out"),
                SocketTimeoutException("socket timed out"),
            )
            for (timeout in timeouts) {
                failingApi(timeout).use { api ->
                    val error = api.accounts.currentUser().leftOrNull()

                    assertIs<NetworkError.Timeout>(error)
                }
            }
        }
    }

    @Test
    fun `io failures are network unavailable failures`() {
        runTest {
            val cause = IOException("offline")
            failingApi(cause).use { api ->
                val error = api.accounts.currentUser().leftOrNull()

                assertEquals(cause.message, assertIs<NetworkError.NetworkUnavailable>(error).cause.message)
            }
        }
    }

    @Test
    fun `serialization exceptions are serialization failures`() {
        runTest {
            val cause = SerializationException("bad payload")
            failingApi(cause).use { api ->
                val error = api.accounts.currentUser().leftOrNull()

                assertEquals(cause.message, assertIs<NetworkError.Serialization>(error).cause.message)
            }
        }
    }

    @Test
    fun `other exceptions are unexpected failures`() {
        runTest {
            val cause = IllegalStateException("boom")
            failingApi(cause).use { api ->
                val error = api.accounts.currentUser().leftOrNull()

                assertEquals(cause.message, assertIs<NetworkError.Unexpected>(error).cause.message)
            }
        }
    }

    @Test
    fun `cancellation propagates instead of becoming a failure`() {
        runTest {
            failingApi(CancellationException("cancelled")).use { api ->
                assertFailsWith<CancellationException> { api.accounts.currentUser() }
            }
        }
    }

    @Test
    fun `the injected logger receives request logs without the bearer token`() {
        runTest {
            val messages = mutableListOf<String>()
            val logger = object : Logger {
                override fun log(message: String) {
                    messages += message
                }
            }
            recordingApi(OpenApiSpec.example("JsonWebTokenPayload").toString(), logger = logger).use { recording ->
                recording.api.accounts.currentUser()

                assertTrue(messages.any { "/me" in it })
                assertTrue(messages.none { TEST_TOKEN in it })
            }
        }
    }

    private fun failingApi(exception: Throwable): MipymeApi {
        return MipymeApi(
            baseUrl = BASE_URL,
            tokenProvider = AuthTokenProvider { TEST_TOKEN },
            engine = MockEngine { throw exception },
        )
    }
}
