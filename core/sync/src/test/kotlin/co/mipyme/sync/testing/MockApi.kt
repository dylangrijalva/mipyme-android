package co.mipyme.sync.testing

import co.mipyme.contract.SyncResponse
import co.mipyme.network.AuthTokenProvider
import co.mipyme.network.MipymeApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface MockReply {
    data class Success(val response: SyncResponse) : MockReply

    data class Failure(val status: Int, val code: String) : MockReply

    data object Offline : MockReply

    data object Timeout : MockReply
}

class MockApi(private val reply: suspend (index: Int) -> MockReply) {
    val requestedTokens = mutableListOf<String>()

    val api = MipymeApi(
        baseUrl = "https://api.mipyme.example.com",
        tokenProvider = AuthTokenProvider { "token" },
        engine = MockEngine { request ->
            val index = synchronized(requestedTokens) {
                requestedTokens += request.syncToken()
                requestedTokens.size - 1
            }
            val headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when (val result = reply(index)) {
                is MockReply.Success -> respond(
                    content = TestJson.encodeToString(SyncResponse.serializer(), result.response),
                    status = HttpStatusCode.OK,
                    headers = headers,
                )

                MockReply.Offline -> throw java.io.IOException("offline")

                MockReply.Timeout -> throw HttpRequestTimeoutException("https://api.mipyme.example.com", 1L)

                is MockReply.Failure -> respond(
                    content = """{"code":"${result.code}","message":"m","details":[],"request_id":"r","status":${result.status}}""",
                    status = HttpStatusCode.fromValue(result.status),
                    headers = headers,
                )
            }
        },
    )

    private fun HttpRequestData.syncToken(): String {
        val bytes = (body as OutgoingContent.ByteArrayContent).bytes()
        return TestJson.parseToJsonElement(bytes.decodeToString()).jsonObject.getValue("sync_token").jsonPrimitive.content
    }
}
