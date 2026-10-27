package co.mipyme.contract.request

import co.mipyme.contract.OptionalField
import co.mipyme.contract.serializer.OptionalFieldSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCollectionRequest(
    @Serializable(with = OptionalFieldSerializer::class)
    val name: OptionalField<String> = OptionalField.Absent,
)
