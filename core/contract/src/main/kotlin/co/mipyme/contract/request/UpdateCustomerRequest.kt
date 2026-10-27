package co.mipyme.contract.request

import co.mipyme.contract.OptionalField
import co.mipyme.contract.serializer.OptionalFieldSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCustomerRequest(
    @Serializable(with = OptionalFieldSerializer::class)
    val name: OptionalField<String> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val email: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val phone: OptionalField<String?> = OptionalField.Absent,
)
