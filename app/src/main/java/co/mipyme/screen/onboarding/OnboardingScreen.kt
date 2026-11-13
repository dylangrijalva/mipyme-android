package co.mipyme.screen.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.data.onboarding.OnboardingRepository
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiCardSelector
import co.mipyme.ui.component.MoshiModal
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiRadioOption
import co.mipyme.ui.component.MoshiStepProgressIndicator
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextField
import co.mipyme.ui.component.MoshiTextRole
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.onboardingScreenModule(): Module {
    singleOf(::OnboardingRepository)
    viewModelOf(::OnboardingViewModel)
    navigation<Route.Onboarding>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
    ) { OnboardingScreen() }
    return this
}

@Composable
internal fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    // The first step keeps the default back behavior and leaves the app.
    BackHandler(enabled = state.step != OnboardingStep.NAME) {
        viewModel.onBack()
    }

    OnboardingContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onBusinessNameChange = viewModel::onBusinessNameChange,
        onBusinessTypeSelected = viewModel::onBusinessTypeSelected,
        onCountrySelected = viewModel::onCountrySelected,
        onContinue = viewModel::onContinue,
        onBack = viewModel::onBack,
        onSignOutConfirmed = viewModel::onSignOutConfirmed,
        onSignOutDismissed = viewModel::onSignOutDismissed,
    )
}

@Composable
private fun OnboardingContent(
    state: OnboardingScreenUiState,
    onNameChange: (String) -> Unit,
    onBusinessNameChange: (String) -> Unit,
    onBusinessTypeSelected: (BusinessType) -> Unit,
    onCountrySelected: (CountryIsoCode) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas)
            .safeDrawingPadding()
            .padding(horizontal = spacing.pageGutterNarrow),
    ) {
        MoshiButton(
            onClick = onBack,
            variant = MoshiButtonVariant.Text,
            enabled = !state.isBusy,
            isLoading = state.isSigningOut,
        ) {
            val labelRes = if (state.step == OnboardingStep.NAME) {
                R.string.onboarding_sign_out
            } else {
                R.string.onboarding_back
            }
            Icon(
                painter = painterResource(R.drawable.ic_fluent_arrow_left_24_regular),
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(spacing.space6),
            )
        }
        Spacer(Modifier.height(spacing.space4))
        MoshiStepProgressIndicator(
            stepCount = OnboardingStep.entries.size,
            currentStep = state.step.ordinal,
            modifier = Modifier
                .fillMaxWidth(0.36f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(spacing.space6))
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward != isRtl) 1 else -1
                slideInHorizontally(initialOffsetX = { it * direction }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it * direction })
            },
            label = "Onboarding step",
        ) { step ->
            Column(modifier = Modifier.fillMaxSize()) {
                MoshiText(
                    text = stringResource(step.titleRes()),
                    role = MoshiTextRole.Heading,
                )
                Spacer(Modifier.height(spacing.space2))
                MoshiText(
                    text = stringResource(step.subtitleRes()),
                    role = MoshiTextRole.Body,
                    color = Moshi.colors.foreground.secondary,
                )
                Spacer(Modifier.height(spacing.space6))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (step) {
                        OnboardingStep.NAME -> TextStep(
                            value = state.name,
                            onValueChange = onNameChange,
                            labelRes = R.string.onboarding_name_label,
                            isTooLong = state.isNameTooLong,
                            enabled = !state.isBusy,
                            onNext = onContinue,
                        )

                        OnboardingStep.BUSINESS_NAME -> TextStep(
                            value = state.businessName,
                            onValueChange = onBusinessNameChange,
                            labelRes = R.string.onboarding_business_name_label,
                            isTooLong = state.isBusinessNameTooLong,
                            enabled = !state.isBusy,
                            onNext = onContinue,
                        )

                        OnboardingStep.BUSINESS_TYPE -> BusinessTypeStep(
                            selected = state.businessType,
                            onSelected = onBusinessTypeSelected,
                            enabled = !state.isBusy,
                        )

                        OnboardingStep.COUNTRY -> CountryStep(
                            selected = state.country,
                            onSelected = onCountrySelected,
                            enabled = !state.isBusy,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(spacing.space4))
        MoshiButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            variant = MoshiButtonVariant.Primary,
            enabled = state.isContinueEnabled,
            isLoading = state.isSubmitting,
        ) {
            val labelRes = if (state.step.next == null) {
                R.string.onboarding_finish
            } else {
                R.string.onboarding_continue
            }
            MoshiText(text = stringResource(labelRes))
        }
        Spacer(Modifier.height(spacing.space4))
    }

    if (state.isSignOutConfirmationVisible) {
        SignOutConfirmationDialog(
            onConfirm = onSignOutConfirmed,
            onDismiss = onSignOutDismissed,
        )
    }
}

@Composable
private fun TextStep(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    isTooLong: Boolean,
    enabled: Boolean,
    onNext: () -> Unit,
) {
    MoshiTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(labelRes),
        supportingText = if (isTooLong) {
            stringResource(R.string.onboarding_text_too_long, ONBOARDING_NAME_MAX_LENGTH)
        } else {
            null
        },
        isError = isTooLong,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
    )
}

@Composable
private fun BusinessTypeStep(
    selected: BusinessType,
    onSelected: (BusinessType) -> Unit,
    enabled: Boolean,
) {
    EqualHeightCardGrid(
        modifier = Modifier.selectableGroup(),
    ) {
        BusinessType.entries.forEach { type ->
            MoshiCardSelector(
                label = stringResource(type.labelRes()),
                selected = type == selected,
                onClick = { onSelected(type) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun CountryStep(
    selected: CountryIsoCode,
    onSelected: (CountryIsoCode) -> Unit,
    enabled: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]
    val countries = remember(locale) {
        CountryIsoCode.entries
            .filter { it != CountryIsoCode.ZZ }
            .map { country -> country to country.displayName(locale) }
            .sortedWith(compareBy({ (country, _) -> country.listGroup() }, { (_, name) -> name }))
    }

    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
    ) {
        countries.forEach { (country, name) ->
            MoshiRadioOption(
                label = name,
                selected = country == selected,
                onClick = { onSelected(country) },
                enabled = enabled,
                leadingContent = { CountryFlag(country) },
            )
        }
    }
}

@Composable
private fun EqualHeightCardGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = Moshi.spacing
    val columns = 2

    Layout(
        modifier = modifier.fillMaxWidth(),
        content = content,
    ) { measurables, constraints ->
        val horizontalSpacing = spacing.space2.roundToPx()
        val verticalSpacing = spacing.space2.roundToPx()
        val columnWidth = (constraints.maxWidth - horizontalSpacing) / columns
        val cardHeight = maxOf(
            spacing.space16.roundToPx(),
            measurables.maxOfOrNull { it.maxIntrinsicHeight(columnWidth) } ?: 0,
        )
        val cardConstraints = Constraints.fixed(width = columnWidth, height = cardHeight)
        val placeables = measurables.map { it.measure(cardConstraints) }
        val rowCount = (placeables.size + columns - 1) / columns
        val contentHeight = if (rowCount == 0) {
            0
        } else {
            cardHeight * rowCount + verticalSpacing * (rowCount - 1)
        }

        layout(
            width = constraints.maxWidth,
            height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / columns
                val column = index % columns
                placeable.placeRelative(
                    x = column * (columnWidth + horizontalSpacing),
                    y = row * (cardHeight + verticalSpacing),
                )
            }
        }
    }
}

@Composable
private fun CountryFlag(country: CountryIsoCode) {
    val flagRes = country.flagRes() ?: return

    Image(
        painter = painterResource(flagRes),
        // Decorative: the option's label already names the country.
        contentDescription = null,
        modifier = Modifier
            .size(Moshi.spacing.space8)
            .border(width = 1.dp, color = Moshi.colors.border.default, shape = CircleShape),
    )
}

@Composable
private fun SignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoshiModal(
        title = stringResource(R.string.onboarding_sign_out_confirm_title),
        supportingText = stringResource(R.string.onboarding_sign_out_confirm_message),
        onDismissRequest = onDismiss,
        primaryAction = MoshiModalAction(
            label = stringResource(R.string.onboarding_sign_out_confirm_action),
            onClick = onConfirm,
        ),
        primaryActionVariant = MoshiButtonVariant.Destructive,
        secondaryAction = MoshiModalAction(
            label = stringResource(R.string.onboarding_sign_out_confirm_cancel),
            onClick = onDismiss,
        ),
    )
}

@StringRes
private fun OnboardingStep.titleRes(): Int {
    return when (this) {
        OnboardingStep.NAME -> R.string.onboarding_name_title
        OnboardingStep.BUSINESS_NAME -> R.string.onboarding_business_name_title
        OnboardingStep.BUSINESS_TYPE -> R.string.onboarding_business_type_title
        OnboardingStep.COUNTRY -> R.string.onboarding_country_title
    }
}

@StringRes
private fun OnboardingStep.subtitleRes(): Int {
    return when (this) {
        OnboardingStep.NAME -> R.string.onboarding_name_subtitle
        OnboardingStep.BUSINESS_NAME -> R.string.onboarding_business_name_subtitle
        OnboardingStep.BUSINESS_TYPE -> R.string.onboarding_business_type_subtitle
        OnboardingStep.COUNTRY -> R.string.onboarding_country_subtitle
    }
}

@StringRes
private fun BusinessType.labelRes(): Int {
    return when (this) {
        BusinessType.CONVENIENCE_STORE -> R.string.onboarding_business_type_convenience_store
        BusinessType.RESTAURANT -> R.string.onboarding_business_type_restaurant
        BusinessType.COFFEE_SHOP -> R.string.onboarding_business_type_coffee_shop
        BusinessType.CLOTHING_STORE -> R.string.onboarding_business_type_clothing_store
        BusinessType.ELECTRONICS_STORE -> R.string.onboarding_business_type_electronics_store
        BusinessType.GROCERY_STORE -> R.string.onboarding_business_type_grocery_store
        BusinessType.PHARMACY -> R.string.onboarding_business_type_pharmacy
        BusinessType.HARDWARE_STORE -> R.string.onboarding_business_type_hardware_store
        BusinessType.BEAUTY_SALON -> R.string.onboarding_business_type_beauty_salon
        BusinessType.BAR -> R.string.onboarding_business_type_bar
        BusinessType.AUTOPARTS_STORE -> R.string.onboarding_business_type_autoparts_store
        BusinessType.SPORTS_STORE -> R.string.onboarding_business_type_sports_store
        BusinessType.BOOKSTORE -> R.string.onboarding_business_type_bookstore
        BusinessType.JEWELRY_STORE -> R.string.onboarding_business_type_jewelry_store
        BusinessType.SHOE_STORE -> R.string.onboarding_business_type_shoe_store
        BusinessType.STATIONERY_STORE -> R.string.onboarding_business_type_stationery_store
        BusinessType.BAKERY -> R.string.onboarding_business_type_bakery
        BusinessType.OTHER -> R.string.onboarding_business_type_other
    }
}

@DrawableRes
private fun CountryIsoCode.flagRes(): Int? {
    return when (this) {
        CountryIsoCode.ZZ -> null
        CountryIsoCode.AR -> R.drawable.flag_ar
        CountryIsoCode.BO -> R.drawable.flag_bo
        CountryIsoCode.BR -> R.drawable.flag_br
        CountryIsoCode.BZ -> R.drawable.flag_bz
        CountryIsoCode.CL -> R.drawable.flag_cl
        CountryIsoCode.CO -> R.drawable.flag_co
        CountryIsoCode.CR -> R.drawable.flag_cr
        CountryIsoCode.CU -> R.drawable.flag_cu
        CountryIsoCode.DO -> R.drawable.flag_do
        CountryIsoCode.EC -> R.drawable.flag_ec
        CountryIsoCode.GT -> R.drawable.flag_gt
        CountryIsoCode.HN -> R.drawable.flag_hn
        CountryIsoCode.HT -> R.drawable.flag_ht
        CountryIsoCode.MX -> R.drawable.flag_mx
        CountryIsoCode.NI -> R.drawable.flag_ni
        CountryIsoCode.PA -> R.drawable.flag_pa
        CountryIsoCode.PE -> R.drawable.flag_pe
        CountryIsoCode.PR -> R.drawable.flag_pr
        CountryIsoCode.PY -> R.drawable.flag_py
        CountryIsoCode.SV -> R.drawable.flag_sv
        CountryIsoCode.UY -> R.drawable.flag_uy
        CountryIsoCode.VE -> R.drawable.flag_ve
    }
}

// Nicaragua first, then the rest of Central America, then every other country.
private fun CountryIsoCode.listGroup(): Int {
    return when (this) {
        CountryIsoCode.NI -> 0
        CountryIsoCode.BZ,
        CountryIsoCode.CR,
        CountryIsoCode.GT,
        CountryIsoCode.HN,
        CountryIsoCode.PA,
        CountryIsoCode.SV -> 1
        else -> 2
    }
}

private fun CountryIsoCode.displayName(locale: Locale): String {
    return Locale.Builder().setRegion(name).build().getDisplayCountry(locale)
}

@ScreenPreview
@Composable
fun OnboardingNameScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(OnboardingScreenUiState(name = "Monkey D. Luffy"))
    }
}

@ScreenPreview
@Composable
fun OnboardingBusinessNameScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(
            OnboardingScreenUiState(
                step = OnboardingStep.BUSINESS_NAME,
                name = "Monkey D. Luffy",
                businessName = "Straw Hat Pirates",
            ),
        )
    }
}

@ScreenPreview
@Composable
fun OnboardingBusinessTypeScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(
            OnboardingScreenUiState(
                step = OnboardingStep.BUSINESS_TYPE,
                businessType = BusinessType.GROCERY_STORE,
            ),
        )
    }
}

@ScreenPreview
@Composable
fun OnboardingCountryScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(OnboardingScreenUiState(step = OnboardingStep.COUNTRY))
    }
}

@ScreenPreview
@Composable
fun OnboardingSubmittingScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(
            OnboardingScreenUiState(step = OnboardingStep.COUNTRY, isSubmitting = true),
        )
    }
}

@ScreenPreview
@Composable
fun OnboardingSignOutConfirmationScreenPreview() {
    MoshiTheme {
        OnboardingPreviewContent(
            OnboardingScreenUiState(name = "Monkey D. Luffy", isSignOutConfirmationVisible = true),
        )
    }
}

@Composable
private fun OnboardingPreviewContent(state: OnboardingScreenUiState) {
    OnboardingContent(
        state = state,
        onNameChange = {},
        onBusinessNameChange = {},
        onBusinessTypeSelected = {},
        onCountrySelected = {},
        onContinue = {},
        onBack = {},
        onSignOutConfirmed = {},
        onSignOutDismissed = {},
    )
}
