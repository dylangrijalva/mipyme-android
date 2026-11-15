package co.mipyme.contract.serializer

import co.mipyme.contract.PosPermission
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PosPermissionListSerializer : KSerializer<List<PosPermission>> {
    private val namesSerializer = ListSerializer(String.serializer())
    private val permissionDescriptor = PosPermission.serializer().descriptor
    private val permissionsByName: Map<String, PosPermission> = PosPermission.entries.associateBy { permission ->
        permissionDescriptor.getElementName(permission.ordinal)
    }

    override val descriptor: SerialDescriptor = namesSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<PosPermission> {
        return decoder.decodeSerializableValue(namesSerializer).mapNotNull { permissionsByName[it] }
    }

    override fun serialize(encoder: Encoder, value: List<PosPermission>) {
        val names = value.map { permissionDescriptor.getElementName(it.ordinal) }
        encoder.encodeSerializableValue(namesSerializer, names)
    }
}
