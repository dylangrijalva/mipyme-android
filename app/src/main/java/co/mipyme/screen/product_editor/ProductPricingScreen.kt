package co.mipyme.screen.product_editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.contract.CountryIsoCode
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.inventory.catalogPriceFormatter
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextFieldTrailingAction
import co.mipyme.ui.component.MoshiTextRole
import java.text.DecimalFormat
import java.text.NumberFormat

@Composable
internal fun ProductPricingScreen(
    viewModel: ProductEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    val discard = {
        viewModel.discardPricing()
        onBack()
    }
    val requestBack = {
        if (state.isPricingDirty) showDiscard = true else discard()
    }
    BackHandler(onBack = requestBack)
    LaunchedEffect(state.canEdit) {
        if (state.canEdit && state.pricingDraft == null) viewModel.openPricing()
    }
    ProductPricingContent(
        draft = state.pricingDraft ?: state.pricing,
        country = state.country,
        enabled = state.canEdit && state.pricingDraft != null,
        onBack = requestBack,
        onDone = { if (viewModel.applyPricing()) onBack() },
        onPriceChange = viewModel::setDraftPrice,
        onCostChange = viewModel::setDraftUnitCost,
    )
    if (showDiscard) {
        ProductEditorDiscardDialog(
            message = R.string.product_editor_discard_pricing_message,
            onDismiss = { showDiscard = false },
            onDiscard = discard,
        )
    }
}

@Composable
private fun ProductPricingContent(
    draft: ProductPricingDraft,
    country: CountryIsoCode?,
    enabled: Boolean,
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    onPriceChange: (String) -> Unit = {},
    onCostChange: (String) -> Unit = {},
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val locale = LocalConfiguration.current.locales[0]
    val priceFormat = remember(country, locale) { catalogPriceFormatter(country, locale) }
    val missingValue = priceFormat.format(0)
    val percentFormat = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val currencyPrefix = (priceFormat as? DecimalFormat)?.decimalFormatSymbols?.currencySymbol
        ?.takeIf { country != null && country != CountryIsoCode.ZZ }
    val profit = draft.grossProfit
    val margin = draft.marginPercent
    val costError = draft.unitCostError

    Column(Modifier.fillMaxSize().background(colors.background.surface)) {
        ProductEditorToolbar(
            title = stringResource(R.string.product_editor_pricing),
            onBack = onBack,
            actionLabel = stringResource(R.string.product_editor_done),
            actionEnabled = enabled && draft.canApply,
            onAction = onDone,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3),
        ) {
            ProductEditorDecimalField(
                value = draft.price,
                onValueChange = onPriceChange,
                label = R.string.product_editor_selling_price,
                enabled = enabled,
                error = draft.priceError?.priceMessage(),
                prefixText = currencyPrefix,
                trailingAction = if (enabled && draft.price.isNotEmpty()) {
                    MoshiTextFieldTrailingAction(
                        icon = R.drawable.ic_fluent_dismiss_24_regular,
                        contentDescription = stringResource(R.string.product_editor_clear_price),
                        onClick = { onPriceChange("") },
                    )
                } else null,
            )
            Spacer(Modifier.height(spacing.space4))
            ProductEditorDecimalField(
                value = draft.unitCost,
                onValueChange = onCostChange,
                label = R.string.product_editor_unit_cost,
                enabled = enabled && !draft.unitCostReadOnly,
                error = R.string.product_editor_invalid_cost.takeIf { costError == ProductEditorFieldError.INVALID },
                supportingText = if (costError == ProductEditorFieldError.REQUIRED) {
                    R.string.product_editor_cost_required
                } else {
                    R.string.product_editor_cost_private
                },
                prefixText = currencyPrefix,
                trailingAction = if (enabled && !draft.unitCostReadOnly && draft.unitCost.isNotEmpty()) {
                    MoshiTextFieldTrailingAction(
                        icon = R.drawable.ic_fluent_dismiss_24_regular,
                        contentDescription = stringResource(R.string.product_editor_clear_cost),
                        onClick = { onCostChange("") },
                    )
                } else null,
            )
            Spacer(Modifier.height(spacing.space4))
            Row(Modifier.fillMaxWidth()) {
                PricingResult(
                    label = stringResource(R.string.product_editor_margin),
                    value = margin?.let { percentFormat.format(it.movePointLeft(2)) } ?: missingValue,
                    isLoss = margin != null && margin.signum() < 0,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(spacing.space4))
                PricingResult(
                    label = stringResource(R.string.product_editor_profit),
                    value = profit?.let(priceFormat::format) ?: missingValue,
                    isLoss = profit != null && profit.signum() < 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PricingResult(label: String, value: String, isLoss: Boolean, modifier: Modifier = Modifier) {
    val colors = Moshi.colors
    Column(
        modifier = modifier
            .background(colors.background.subtle, RoundedCornerShape(Moshi.spacing.space3))
            .padding(Moshi.spacing.space4),
    ) {
        MoshiText(
            text = label,
            role = MoshiTextRole.Copy16,
            color = colors.foreground.secondary,
        )
        Spacer(Modifier.height(Moshi.spacing.space2))
        MoshiText(
            text = value,
            role = MoshiTextRole.Heading20,
            color = if (isLoss) colors.foreground.danger else colors.foreground.primary,
        )
    }
}

@ScreenPreview
@Composable
private fun ProductPricingPreview() {
    MoshiTheme {
        ProductPricingContent(
            draft = ProductPricingDraft(price = "15.00", unitCost = "3.00"),
            country = CountryIsoCode.NI,
            enabled = true,
        )
    }
}
