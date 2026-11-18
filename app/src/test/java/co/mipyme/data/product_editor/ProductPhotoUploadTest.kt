package co.mipyme.data.product_editor

import arrow.core.right
import co.mipyme.network.NetworkError
import co.mipyme.testing.FakeMipymeApi
import co.mipyme.testing.FakeResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPhotoUploadTest {
    private val webp = byteArrayOf(0x52, 0x49, 0x46, 0x46, 4, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)
    private val selection = ProductPhotoSelection(
        productId = "prod_tangerines",
        uri = "content://co.mipyme.dev.product-photo/product-photos/tangerines.webp",
        accountId = "nami",
        merchantId = "straw_hat_pirates",
    )
    private val product = """
        {
          "id":"prod_tangerines","name":"East Blue Tangerines","slug":"east-blue-tangerines",
          "unit_cost":"5","price":"10","product_type":"item","status":"active",
          "image_url":"https://storage.example.com/verified/tangerines.webp","collection":null,
          "option_groups":[],"variants":[],"created_at":"2026-11-17T10:00:00.000Z",
          "updated_at":"2026-11-17T10:00:00.000Z","archived_at":null
        }
    """.trimIndent()

    @Test
    fun `retry after a verification connection failure verifies the same upload without rereading the URI`() = runTest {
        val fake = api()
        fake.api.use { api ->
            val upload = ProductPhotoUpload(api.products)
            val first = upload.finish(selection, { null }) { webp.right() }
            assertTrue(first.leftOrNull() is NetworkError.NetworkUnavailable)

            val retry = upload.finish(selection, { null }) { error("The photo had already been uploaded") }

            assertEquals(selection.productId, retry.getOrNull()?.id)
            assertEquals(listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Post, HttpMethod.Post), fake.requests.map { it.method })
            assertEquals(fake.requests[2].url, fake.requests[3].url)
        }
    }

    @Test
    fun `a recreated editor resumes uploaded bytes from the checkpoint taken before verification`() = runTest {
        lateinit var initial: ProductPhotoUpload
        var beforeVerify: ProductPhotoUploadCheckpoint? = null
        val fake = api(onVerify = { beforeVerify = initial.observeCheckpoint().first() })
        fake.api.use { api ->
            initial = ProductPhotoUpload(api.products)
            assertTrue(initial.finish(selection, { null }) { webp.right() }.isLeft())
            val checkpoint = requireNotNull(beforeVerify)
            assertEquals("img_tangerines_1", checkpoint.imageId)
            assertEquals(checkpoint, initial.observeCheckpoint().first())
            val savedState = ByteArrayOutputStream()
            ObjectOutputStream(savedState).use { it.writeObject(checkpoint) }
            val restored = ObjectInputStream(ByteArrayInputStream(savedState.toByteArray())).use {
                it.readObject() as ProductPhotoUploadCheckpoint
            }

            val recreated = ProductPhotoUpload(api.products)
            recreated.restore(restored)
            val result = recreated.finish(selection, { null }) { error("The photo had already been uploaded") }

            assertTrue(result.isRight())
            assertNull(recreated.observeCheckpoint().first())
            assertEquals(1, fake.requests.count { it.method == HttpMethod.Put })
            assertEquals(fake.requests[2].url, fake.requests[3].url)
        }
    }

    @Test
    fun `retry authorizes a new upload when the stored upload expired`() = runTest {
        val fake = api(expireFirstUpload = true)
        fake.api.use { api ->
            val upload = ProductPhotoUpload(api.products)
            val first = upload.finish(selection, { null }) { webp.right() }
            assertTrue(first.isLeft())

            val retry = upload.finish(selection, { null }) { webp.right() }

            assertEquals(selection.productId, retry.getOrNull()?.id)
            val verifies = fake.requests.filter { it.url.encodedPath.endsWith("/verify") }
            assertEquals(
                listOf("img_tangerines_1", "img_tangerines_1", "img_tangerines_2"),
                verifies.map { it.url.encodedPath.split('/')[5] },
            )
            assertEquals(2, fake.requests.count { it.method == HttpMethod.Put })
        }
    }

    @Test
    fun `discarding a selection invalidates its verify retry even when the next URI matches`() = runTest {
        val fake = api()
        fake.api.use { api ->
            val upload = ProductPhotoUpload(api.products)
            assertTrue(upload.finish(selection, { null }) { webp.right() }.isLeft())
            upload.discard()
            assertNull(upload.observeCheckpoint().first())

            assertTrue(upload.finish(selection, { null }) { webp.right() }.isRight())

            assertEquals(2, fake.requests.count { it.method == HttpMethod.Put })
            val paths = fake.requests.map { it.url.encodedPath }
            assertEquals(1, paths.count { it.endsWith("/img_tangerines_1/verify") })
            assertEquals(1, paths.count { it.endsWith("/img_tangerines_2/verify") })
        }
    }

    @Test
    fun `a recreated editor cannot verify another account or merchant's saved upload`() = runTest {
        val fake = api()
        fake.api.use { api ->
            val initial = ProductPhotoUpload(api.products)
            assertTrue(initial.finish(selection, { null }) { webp.right() }.isLeft())
            val checkpoint = requireNotNull(initial.observeCheckpoint().first())

            val differentAccount = ProductPhotoUpload(api.products)
            differentAccount.restore(checkpoint)
            val differentMerchant = ProductPhotoUpload(api.products)
            differentMerchant.restore(checkpoint)
            assertTrue(differentAccount.finish(selection.copy(accountId = "usopp"), { null }) { webp.right() }.isRight())
            assertTrue(
                differentMerchant.finish(selection.copy(merchantId = "heart_pirates"), { null }) { webp.right() }.isRight(),
            )

            assertEquals(3, fake.requests.count { it.method == HttpMethod.Put })
            assertEquals(1, fake.requests.count { it.url.encodedPath.endsWith("/img_tangerines_1/verify") })
        }
    }

    private fun api(expireFirstUpload: Boolean = false, onVerify: suspend () -> Unit = {}): FakeMipymeApi {
        var verifyCount = 0
        var authorizeCount = 0
        return FakeMipymeApi { request ->
            when {
                request.method == HttpMethod.Put -> FakeResponse(HttpStatusCode.OK, "")
                request.url.encodedPath.endsWith("/verify") -> {
                    onVerify()
                    when (++verifyCount) {
                        1 -> throw IOException("Connection reset while verifying")
                        2 -> if (expireFirstUpload) {
                            FakeResponse(
                                HttpStatusCode.Conflict,
                                """{"code":"conflict_error","message":"Upload expired","status":409,"details":[],"request_id":"req_1"}""",
                            )
                        } else {
                            FakeResponse(HttpStatusCode.OK, product)
                        }
                        else -> FakeResponse(HttpStatusCode.OK, product)
                    }
                }

                else -> FakeResponse(
                    HttpStatusCode.Created,
                    """
                    {
                      "id":"img_tangerines_${++authorizeCount}",
                      "upload_url":"https://storage.example.com/tangerines.webp",
                      "expires_at":"2026-11-17T10:10:00.000Z",
                      "headers":{"content-type":"image/webp","content-length":"${webp.size}"}
                    }
                    """.trimIndent(),
                )
            }
        }
    }
}
