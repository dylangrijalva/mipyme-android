package co.mipyme.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.File
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal const val BASE_URL = "https://api.mipyme.example.com"
internal const val TEST_TOKEN = "straw-hat-token"

internal val TestJson = Json { ignoreUnknownKeys = true }

// Reads the checked-in OpenAPI snapshot (`core/network/openapi.json`).
internal object OpenApiSpec {
    private val HTTP_METHODS = setOf("get", "post", "put", "patch", "delete")
    private val specification = TestJson.parseToJsonElement(File("openapi.json").readText()).jsonObject
    private val schemas = specification.getValue("components").jsonObject.getValue("schemas").jsonObject

    // Every `(lowercase method, path template)` pair documented by the specification.
    val operations: Set<Pair<String, String>> = specification.getValue("paths").jsonObject
        .flatMap { (path, item) ->
            item.jsonObject.keys
                .filter { it in HTTP_METHODS }
                .map { method -> method to path }
        }
        .toSet()

    // Operations that document the `Idempotency-Key` header.
    val idempotentOperations: Set<Pair<String, String>> = specification.getValue("paths").jsonObject
        .flatMap { (path, item) ->
            item.jsonObject
                .filterKeys { it in HTTP_METHODS }
                .filterValues { operation ->
                    operation.jsonObject["parameters"]?.jsonArray.orEmpty().any { parameter ->
                        parameter.jsonObject["\$ref"]?.jsonPrimitive?.content?.endsWith("/IdempotencyKey") == true
                    }
                }
                .map { (method, _) -> method to path }
        }
        .toSet()

    fun example(schemaName: String): JsonElement {
        return sample(schemas.getValue(schemaName).jsonObject)
    }

    private fun sample(schema: JsonObject): JsonElement {
        schema["examples"]?.jsonArray?.firstOrNull()?.let { return it }
        schema["\$ref"]?.jsonPrimitive?.content?.let { return example(it.substringAfterLast('/')) }
        schema["anyOf"]?.jsonArray?.let { return sample(it.first().jsonObject) }
        schema["enum"]?.jsonArray?.let { return it.first() }
        val type = schema["type"]?.let { if (it is JsonArray) it.first() else it }?.jsonPrimitive?.content
        return when (type) {
            "object" -> buildJsonObject {
                val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
                schema["properties"]?.jsonObject?.forEach { (name, value) ->
                    if (name in required) put(name, sample(value.jsonObject))
                }
            }
            "array" -> JsonArray(emptyList())
            "boolean" -> JsonPrimitive(false)
            "integer", "number" -> JsonPrimitive(1)
            "null" -> JsonNull
            else -> JsonPrimitive("Straw Hat Pirates")
        }
    }
}

internal class RecordingApi(
    val api: MipymeApi,
    val requests: List<HttpRequestData>,
) : AutoCloseable by api {
    val request: HttpRequestData
        get() = requests.single()
}

internal fun recordingApi(
    response: String = "{}",
    status: HttpStatusCode = HttpStatusCode.OK,
    tokenProvider: AuthTokenProvider = AuthTokenProvider { TEST_TOKEN },
    logger: Logger = object : Logger {
        override fun log(message: String) = Unit
    },
): RecordingApi {
    val requests = mutableListOf<HttpRequestData>()
    val engine = MockEngine { request ->
        requests += request
        respond(response, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }
    return RecordingApi(MipymeApi(BASE_URL, tokenProvider, logger, engine), requests)
}

internal fun HttpRequestData.jsonBody(): JsonObject {
    val content = assertIs<OutgoingContent.ByteArrayContent>(body).bytes().decodeToString()
    return TestJson.parseToJsonElement(content).jsonObject
}

internal fun jsonOf(vararg fields: Pair<String, JsonElement>): JsonObject {
    return buildJsonObject { fields.forEach { (name, value) -> put(name, value) } }
}

internal fun page(vararg results: JsonElement, nextCursor: String? = null): String {
    return buildJsonObject {
        put("results", JsonArray(results.toList()))
        put("next_cursor", nextCursor?.let { JsonPrimitive(it) } ?: JsonNull)
        put("has_more", nextCursor != null)
    }.toString()
}
