package co.mipyme.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RealtimeChangeEvent(
    val id: String,
    val type: RealtimeResourceType,
    val resourceId: String,
    val snapshot: JsonElement?,
)
