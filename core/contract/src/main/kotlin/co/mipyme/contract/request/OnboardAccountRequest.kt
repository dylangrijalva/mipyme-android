package co.mipyme.contract.request

import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import kotlinx.serialization.Serializable

@Serializable
data class OnboardAccountRequest(
    val name: String,
    val country: CountryIsoCode,
    val location: OnboardLocationRequest,
)

@Serializable
data class OnboardLocationRequest(
    val name: String,
    val businessType: BusinessType = BusinessType.OTHER,
)
