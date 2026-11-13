package co.mipyme.network.api

import co.mipyme.contract.Collection
import co.mipyme.contract.OptionalField
import co.mipyme.contract.PaginationResponse
import co.mipyme.contract.request.CreateCollectionRequest
import co.mipyme.contract.request.PaginationRequest
import co.mipyme.contract.request.UpdateCollectionRequest
import co.mipyme.network.OpenApiSpec
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.jsonBody
import co.mipyme.network.jsonOf
import co.mipyme.network.page
import co.mipyme.network.recordingApi
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive

class CollectionsApiTest {
    private val example = OpenApiSpec.example("Collection")

    @Test
    fun `create posts the name with the idempotency key`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.collections.create(CreateCollectionRequest("East Blue Provisions"), "collection-key")

                val sent = recording.request
                assertEquals(HttpMethod.Post, sent.method)
                assertEquals("/v1/collections", sent.url.encodedPath)
                assertEquals("collection-key", sent.headers["Idempotency-Key"])
                assertEquals(jsonOf("name" to JsonPrimitive("East Blue Provisions")), sent.jsonBody())
            }
        }
    }

    @Test
    fun `list sends the limit and cursor and decodes the page`() {
        runTest {
            recordingApi(page(example, nextCursor = "next-page")).use { recording ->
                val result = recording.api.collections.list(PaginationRequest(limit = 10, cursor = "opaque-cursor")).getOrNull()

                val sent = recording.request
                assertEquals(HttpMethod.Get, sent.method)
                assertEquals("/v1/collections", sent.url.encodedPath)
                assertEquals("10", sent.url.parameters["limit"])
                assertEquals("opaque-cursor", sent.url.parameters["cursor"])
                val collection = MipymeJson.decodeFromJsonElement(Collection.serializer(), example)
                assertEquals(PaginationResponse(listOf(collection), "next-page", true), result)
            }
        }
    }

    @Test
    fun `find gets the collection by id`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                val result = recording.api.collections.find("col_fruit").getOrNull()

                assertEquals(MipymeJson.decodeFromJsonElement(Collection.serializer(), example), result)
                assertEquals(HttpMethod.Get, recording.request.method)
                assertEquals("/v1/collections/col_fruit", recording.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `update patches only the supplied name`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.collections.update("col_fruit", UpdateCollectionRequest(OptionalField.Present("Fruit")))

                assertEquals(HttpMethod.Patch, recording.request.method)
                assertEquals("/v1/collections/col_fruit", recording.request.url.encodedPath)
                assertEquals(jsonOf("name" to JsonPrimitive("Fruit")), recording.request.jsonBody())
            }
        }
    }

    @Test
    fun `update with no changes sends an empty object`() {
        runTest {
            recordingApi(example.toString()).use { recording ->
                recording.api.collections.update("col_fruit", UpdateCollectionRequest())

                assertEquals(jsonOf(), recording.request.jsonBody())
            }
        }
    }
}
