package co.mipyme.network.api

import co.mipyme.contract.RealtimeChannelAuthorization
import co.mipyme.contract.request.AuthorizeRealtimeChannelRequest
import co.mipyme.network.recordingApi
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class RealtimeApiTest {
    private val request = AuthorizeRealtimeChannelRequest(
        socketId = "123.456",
        channelName = "private-merch_STRAWHATPIRATES0000",
    )

    @Test
    fun `authorize channel posts form encoded snake case fields`() {
        runTest {
            recordingApi("""{"auth":"thousand-sunny-signature"}""").use { recording ->
                recording.api.realtime.authorizeChannel(request)

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/realtime/subscriptions", sent.url.encodedPath)
                val form = assertIs<OutgoingContent.ByteArrayContent>(sent.body)
                assertEquals(ContentType.Application.FormUrlEncoded, form.contentType?.withoutParameters())
                assertEquals(
                    "socket_id=123.456&channel_name=private-merch_STRAWHATPIRATES0000",
                    form.bytes().decodeToString(),
                )
            }
        }
    }

    @Test
    fun `authorize channel decodes the authorization`() {
        runTest {
            recordingApi("""{"auth":"thousand-sunny-signature"}""").use { recording ->
                val result = recording.api.realtime.authorizeChannel(request).getOrNull()

                assertEquals(RealtimeChannelAuthorization("thousand-sunny-signature"), result)
            }
        }
    }
}
