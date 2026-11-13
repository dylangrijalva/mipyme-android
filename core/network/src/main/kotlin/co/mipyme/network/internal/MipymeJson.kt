package co.mipyme.network.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

// The API speaks snake_case; the contracts declare camelCase properties.
@OptIn(ExperimentalSerializationApi::class)
internal val MipymeJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = false
}
