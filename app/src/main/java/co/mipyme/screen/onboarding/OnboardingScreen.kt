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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.displayName
import co.mipyme.data.onboarding.OnboardingRepository
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiCardSelector
import co.mipyme.ui.component.MoshiLinearProgressIndicator
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiRadioOption
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextField
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarAction
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.onboardingScreenModule(): Module {
    singleOf(::OnboardingRepository)
    viewModelOf(::OnboardingViewModel)
    navigation<Route.Onboarding>(
        metadata = NavigationTransitions.forward,
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

    BackHandler(enabled = state.step != OnboardingStep.NAME || state.isBusy) {
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
        onSignOutClick = viewModel::onSignOutClick,
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
    onSignOutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas)
            .safeDrawingPadding(),
    ) {
        MoshiToolbar(
            navigation = if (state.step.previous != null && !state.isBusy) {
                MoshiToolbarNavigation.Back(onClick = onBack)
            } else {
                null
            },
            actions = listOf(
                MoshiToolbarAction(
                    icon = R.drawable.ic_fluent_sign_out_24_regular,
                    label = stringResource(R.string.onboarding_sign_out),
                    onClick = onSignOutClick,
                ),
            ),
            compact = true,
            windowInsets = WindowInsets(0),
        )
        MoshiLinearProgressIndicator(
            progress = (state.step.ordinal + 1).toFloat() / OnboardingStep.entries.size,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.pageGutterNarrow),
        ) {
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
                                placeholderRes = R.string.onboarding_name_placeholder,
                                isTooLong = state.isNameTooLong,
                                enabled = !state.isBusy,
                                onNext = onContinue,
                            )

                            OnboardingStep.BUSINESS_NAME -> TextStep(
                                value = state.businessName,
                                onValueChange = onBusinessNameChange,
                                labelRes = R.string.onboarding_business_name_label,
                                placeholderRes = R.string.onboarding_business_name_placeholder,
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
    @StringRes placeholderRes: Int,
    isTooLong: Boolean,
    enabled: Boolean,
    onNext: () -> Unit,
) {
    MoshiTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(labelRes),
        placeholder = stringResource(placeholderRes),
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
                icon = type.iconRes(),
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
            .filter { it in ONBOARDING_SELECTABLE_COUNTRIES }
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
    MoshiAlertDialog(
        modifier = Modifier.padding(horizontal = Moshi.spacing.space6),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = stringResource(R.string.onboarding_sign_out_confirm_title),
        message = stringResource(R.string.onboarding_sign_out_confirm_message),
        onDismissRequest = onDismiss,
        confirmAction = MoshiModalAction(
            label = stringResource(R.string.onboarding_sign_out_confirm_action),
            onClick = onConfirm,
        ),
        confirmActionVariant = MoshiButtonVariant.Destructive,
        cancelAction = MoshiModalAction(
            label = stringResource(R.string.onboarding_sign_out_confirm_cancel),
            onClick = onDismiss,
        ),
    )
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
private fun BusinessType.iconRes(): Int {
    return when (this) {
        BusinessType.CONVENIENCE_STORE -> R.drawable.ic_fluent_building_retail_24_regular
        BusinessType.RESTAURANT -> R.drawable.ic_fluent_food_24_regular
        BusinessType.COFFEE_SHOP -> R.drawable.ic_fluent_drink_coffee_24_regular
        BusinessType.CLOTHING_STORE -> R.drawable.ic_fluent_clothes_hanger_24_regular
        BusinessType.ELECTRONICS_STORE -> R.drawable.ic_fluent_phone_laptop_24_regular
        BusinessType.GROCERY_STORE -> R.drawable.ic_fluent_cart_24_regular
        BusinessType.PHARMACY -> R.drawable.ic_fluent_pill_24_regular
        BusinessType.HARDWARE_STORE -> R.drawable.ic_fluent_wrench_screwdriver_24_regular
        BusinessType.BEAUTY_SALON -> R.drawable.ic_fluent_cut_24_regular
        BusinessType.BAR -> R.drawable.ic_fluent_drink_beer_24_regular
        BusinessType.AUTOPARTS_STORE -> R.drawable.ic_fluent_vehicle_car_24_regular
        BusinessType.SPORTS_STORE -> R.drawable.ic_fluent_sport_soccer_24_regular
        BusinessType.BOOKSTORE -> R.drawable.ic_fluent_book_open_24_regular
        BusinessType.JEWELRY_STORE -> R.drawable.ic_fluent_diamond_24_regular
        BusinessType.SHOE_STORE -> R.drawable.ic_fluent_run_24_regular
        BusinessType.STATIONERY_STORE -> R.drawable.ic_fluent_pen_24_regular
        BusinessType.BAKERY -> R.drawable.ic_fluent_food_toast_24_regular
        BusinessType.OTHER -> R.drawable.ic_fluent_more_horizontal_24_regular
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
        onSignOutClick = {},
        onSignOutConfirmed = {},
        onSignOutDismissed = {},
    )
}
