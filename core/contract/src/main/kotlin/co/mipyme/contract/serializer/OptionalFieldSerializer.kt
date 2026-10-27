package co.mipyme.contract.serializer

import co.mipyme.contract.OptionalField
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class OptionalFieldSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<OptionalField<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun deserialize(decoder: Decoder): OptionalField<T> {
        return OptionalField.Present(decoder.decodeSerializableValue(valueSerializer))
    }

    override fun serialize(encoder: Encoder, value: OptionalField<T>) {
        when (value) {
            OptionalField.Absent -> {
                throw SerializationException("An absent optional field must not be encoded")
            }

            is OptionalField.Present -> {
                encoder.encodeSerializableValue(valueSerializer, value.value)
            }
        }
    }
}
