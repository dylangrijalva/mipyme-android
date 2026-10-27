package co.mipyme.contract.request

import co.mipyme.contract.BusinessType
import co.mipyme.contract.Decimal
import co.mipyme.contract.OptionalField
import co.mipyme.contract.serializer.OptionalFieldSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLocationRequest(
    @Serializable(with = OptionalFieldSerializer::class)
    val name: OptionalField<String> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val businessType: OptionalField<BusinessType> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val phone: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val address: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val taxId: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val receiptHeaderText: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val receiptFooterText: OptionalField<String?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val exchangeRate: OptionalField<Decimal> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val taxRate: OptionalField<Decimal?> = OptionalField.Absent,
    @Serializable(with = OptionalFieldSerializer::class)
    val allowBackorders: OptionalField<Boolean> = OptionalField.Absent,
)
