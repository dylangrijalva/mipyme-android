package co.mipyme.network.api

import co.mipyme.contract.SyncResponse
import co.mipyme.contract.request.SyncRequest
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import co.mipyme.network.recordingApi
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive

class SyncApiTest {
    @Test
    fun `read resources posts the opaque sync token`() {
        runTest {
            recordingApi(OpenApiSpec.example("SyncResponse").toString()).use { recording ->
                recording.api.sync.readResources(SyncRequest(syncToken = "*"))

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/sync", sent.url.encodedPath)
                assertEquals(jsonOf("sync_token" to JsonPrimitive("*")), sent.jsonBody())
            }
        }
    }

    @Test
    fun `read resources decodes the sync response`() {
        runTest {
            val example = OpenApiSpec.example("SyncResponse")
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.sync.readResources(SyncRequest(syncToken = "*")).getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(SyncResponse.serializer(), example), result)
            }
        }
    }
}
