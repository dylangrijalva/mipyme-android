package co.mipyme.contract.serializer

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val value = decoder.decodeString()
        return runCatching {
            Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDateTime()
        }.getOrElse {
            runCatching {
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
            }.getOrElse {
                LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(
            DateTimeFormatter.ISO_INSTANT.format(value.toInstant(ZoneOffset.UTC)),
        )
    }
}
