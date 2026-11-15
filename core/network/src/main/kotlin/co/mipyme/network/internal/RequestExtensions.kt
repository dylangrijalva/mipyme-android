package co.mipyme.network.internal

import co.mipyme.contract.serializer.LocalDateTimeSerializer
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.LocalDateTime
import kotlinx.serialization.serializer

internal inline fun <reified T> HttpRequestBuilder.jsonBody(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

internal fun HttpRequestBuilder.idempotencyKey(key: String?) {
    if (key != null) {
        header("Idempotency-Key", key)
    }
}

internal inline fun <reified E : Enum<E>> E.wireName(): String {
    return serializer<E>().descriptor.getElementName(ordinal)
}

internal fun LocalDateTime.toQueryValue(): String {
    return MipymeJson.encodeToString(LocalDateTimeSerializer, this).trim('"')
}

internal fun HttpRequestBuilder.pagination(limit: Int, cursor: String?) {
    parameter("limit", limit)
    cursor?.let { parameter("cursor", it) }
}
