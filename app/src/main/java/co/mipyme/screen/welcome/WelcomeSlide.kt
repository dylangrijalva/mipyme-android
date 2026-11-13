package co.mipyme.screen.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

internal data class WelcomeSlide(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)
