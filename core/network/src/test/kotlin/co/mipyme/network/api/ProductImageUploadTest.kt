package co.mipyme.network.api

import co.mipyme.contract.Product
import co.mipyme.network.AuthTokenProvider
import co.mipyme.network.BASE_URL
import co.mipyme.network.MipymeApi
import co.mipyme.network.NetworkError
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.TEST_TOKEN
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive

class ProductImageUploadTest {
    private val webp = byteArrayOf(0x52, 0x49, 0x46, 0x46, 4, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)
    private val signedUrl = "https://storage.example.com/tangerines.webp?credential=straw-hats%2Fs3&signature=redacted"
    private val authorization = """
        {
          "id": "img_tangerines",
          "upload_url": "$signedUrl",
          "expires_at": "2026-09-23T09:10:00.000Z",
          "headers": {"content-type": "image/webp", "content-length": "${webp.size}"}
        }
    """.trimIndent()
    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `upload sends exact WebP bytes without bearer and verification returns the product`() = runTest {
        val product = OpenApiSpec.example("Product")
        val requests = mutableListOf<HttpRequestData>()
        val messages = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += request
            when (requests.size) {
                1 -> respond(authorization, HttpStatusCode.Created, json)
                2 -> respond("", HttpStatusCode.OK)
                else -> respond(product.toString(), HttpStatusCode.OK, json)
            }
        }
        MipymeApi(BASE_URL, AuthTokenProvider { TEST_TOKEN }, logger(messages), engine).use { api ->
            val imageId = requireNotNull(api.products.storeImage("prod_tangerines", webp).getOrNull())
            val result = api.products.verifyImageUpload("prod_tangerines", imageId)
            assertEquals(MipymeJson.decodeFromJsonElement(Product.serializer(), product), result.getOrNull())
        }

        val (authorize, put, verify) = requests
        assertEquals("Bearer $TEST_TOKEN", authorize.headers[HttpHeaders.Authorization])
        assertEquals(jsonOf("byte_size" to JsonPrimitive(webp.size)), authorize.jsonBody())
        assertEquals(HttpMethod.Put, put.method)
        assertEquals(signedUrl, put.url.toString())
        assertNull(put.headers[HttpHeaders.Authorization])
        val body = assertIs<OutgoingContent.ByteArrayContent>(put.body)
        assertEquals("image/webp", body.contentType.toString())
        assertEquals(webp.size.toLong(), body.contentLength)
        assertContentEquals(webp, body.bytes())
        assertEquals("Bearer $TEST_TOKEN", verify.headers[HttpHeaders.Authorization])
        assertEquals("/v1/products/prod_tangerines/image-uploads/img_tangerines/verify", verify.url.encodedPath)
        assertTrue(messages.any { it.contains("/v1/products/prod_tangerines/image-uploads") })
        assertFalse(messages.any { it.contains(signedUrl) || it.contains("storage.example.com") })
    }

    @Test
    fun `retrying failed verification reuses the uploaded image`() = runTest {
        val product = OpenApiSpec.example("Product")
        val requests = mutableListOf<HttpRequestData>()
        var verifyAttempts = 0
        val engine = MockEngine { request ->
            requests += request
            when {
                request.method == HttpMethod.Put -> respond("", HttpStatusCode.OK)
                request.url.encodedPath.endsWith("/verify") -> {
                    if (++verifyAttempts == 1) throw IOException("Connection reset while verifying")
                    respond(product.toString(), HttpStatusCode.OK, json)
                }
                else -> respond(authorization, HttpStatusCode.Created, json)
            }
        }
        MipymeApi(BASE_URL, AuthTokenProvider { TEST_TOKEN }, engine = engine).use { api ->
            val imageId = requireNotNull(api.products.storeImage("prod_tangerines", webp).getOrNull())
            val first = api.products.verifyImageUpload("prod_tangerines", imageId)
            assertIs<NetworkError.NetworkUnavailable>(first.leftOrNull())
            val retry = api.products.verifyImageUpload("prod_tangerines", imageId)
            assertEquals(MipymeJson.decodeFromJsonElement(Product.serializer(), product), retry.getOrNull())
        }

        assertEquals(listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Post, HttpMethod.Post), requests.map { it.method })
        assertEquals(requests[2].url, requests[3].url)
    }

    @Test
    fun `a failed direct upload skips verification and redacts its URL from the error`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            if (requests.size == 1) {
                respond(authorization, HttpStatusCode.Created, json)
            } else {
                throw IOException("Connection reset while uploading to $signedUrl")
            }
        }
        MipymeApi(BASE_URL, AuthTokenProvider { TEST_TOKEN }, engine = engine).use { api ->
            val result = api.products.storeImage("prod_tangerines", webp)
            val error = assertIs<NetworkError.NetworkUnavailable>(result.leftOrNull())
            assertFalse(error.cause.stackTraceToString().contains("storage.example.com"))
            assertEquals(listOf(HttpMethod.Post, HttpMethod.Put), requests.map { it.method })
        }
    }

    private fun logger(messages: MutableList<String>): Logger {
        return object : Logger {
            override fun log(message: String) {
                messages += message
            }
        }
    }
}
