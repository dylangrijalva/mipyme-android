package co.mipyme.screen.product_editor

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.ProductType
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.inventory.GoogleInventoryScanner
import co.mipyme.screen.inventory.InventoryScanResult
import co.mipyme.screen.inventory.InventoryScanSession
import co.mipyme.screen.inventory.catalogPriceFormatter
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiDivider
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiListItem
import co.mipyme.ui.component.MoshiListItemChevron
import co.mipyme.ui.component.MoshiLoadingIndicator
import co.mipyme.ui.component.MoshiProjectBanner
import co.mipyme.ui.component.MoshiProjectBannerVariant
import co.mipyme.ui.component.MoshiSection
import co.mipyme.ui.component.MoshiQuantityButton
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTag
import co.mipyme.ui.component.MoshiTagTone
import co.mipyme.ui.component.MoshiTextEmphasis
import co.mipyme.ui.component.MoshiTextField
import co.mipyme.ui.component.MoshiTextFieldTrailingAction
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.data.product_editor.DefaultProductEditorRepository
import co.mipyme.data.product_editor.ProductEditorRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.navigation3.navigation
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import java.text.DecimalFormat

internal fun Module.productEditorScreenModule(): Module {
    factoryOf(::DefaultProductEditorRepository) bind ProductEditorRepository::class
    viewModel { (route: Route.ProductEditor) ->
        ProductEditorViewModel(repository = get(), productType = route.type, productId = route.productId, savedState = get())
    }
    navigation<Route.ProductEditor>(metadata = NavigationTransitions.slideUpWithDownPop) { route ->
        ProductEditorWorkflow(route)
    }
    return this
}

@Composable
internal fun ProductEditorScreen(
    viewModel: ProductEditorViewModel,
    onBack: () -> Unit,
    onPricing: () -> Unit,
    onSaved: (String) -> Unit,
    onAdjustStock: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current 
    val scanner = remember(context) { InventoryScanSession(GoogleInventoryScanner(context)) }
    var scanning by remember { mutableStateOf(false) }
    var showScanConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }
    var showCollections by rememberSaveable { mutableStateOf(false) }
    val latestOnSaved by rememberUpdatedState(onSaved)
    val photoActions = rememberProductEditorPhotoActions(viewModel::setPhoto, viewModel::reportPhotoError)
    val leaveEditor = {
        photoActions.discardCapture()
        onBack()
    }
    val requestBack = {
        when {
            state.isSaving || photoActions.isPicking || scanning -> Unit
            state.isDirty -> showDiscard = true
            else -> leaveEditor()
        }
    }
    BackHandler(enabled = !showPhotoSheet && !showCollections, onBack = requestBack)
    DisposableEffect(scanner) { onDispose { scanner.dispose() } }
    LaunchedEffect(state.savedProductId) {
        state.savedProductId?.let { id ->
            photoActions.discardCapture()
            latestOnSaved(id)
        }
    }
    LaunchedEffect(state.collectionSelectionApplied) {
        if (state.collectionSelectionApplied) {
            showCollections = false
            viewModel.clearCollectionSelectionApplied()
        }
    }

    ProductEditorContent(
        state = state,
        actions = ProductEditorActions(
            back = requestBack,
            save = viewModel::save,
            retry = viewModel::retry,
            dismissError = viewModel::dismissError,
            name = viewModel::setName,
            barcode = viewModel::setBarcode,
            scan = { showScanConfirmation = true },
            photo = { showPhotoSheet = true },
            collections = { if (viewModel.openCollections()) showCollections = true },
            price = viewModel::setPrice,
            pricing = { if (viewModel.openPricing()) onPricing() },
            adjustStock = onAdjustStock,
        ),
        scanning = scanning,
        pickingPhoto = photoActions.isPicking,
    )

    if (showScanConfirmation) {
        MoshiAlertDialog(
            title = stringResource(R.string.inventory_scan),
            message = stringResource(R.string.product_editor_scan_confirmation),
            onDismissRequest = { showScanConfirmation = false },
            confirmAction = MoshiModalAction(
                label = stringResource(R.string.product_editor_open_scanner),
                enabled = state.canEdit && !photoActions.isPicking && !scanning,
                onClick = {
                    showScanConfirmation = false
                    if (viewModel.state.canEdit && !photoActions.isPicking && !scanning) {
                        scanning = true
                        scanner.launch { result ->
                            scanning = false
                            when (result) {
                                is InventoryScanResult.Success -> if (result.text.isBlank()) {
                                    viewModel.reportScanError()
                                } else {
                                    viewModel.setBarcode(result.text)
                                }

                                InventoryScanResult.Failure -> viewModel.reportScanError()
                                InventoryScanResult.Cancelled -> Unit
                            }
                        }
                    }
                },
            ),
            cancelAction = MoshiModalAction(
                label = stringResource(R.string.product_editor_cancel_scan),
                onClick = { showScanConfirmation = false },
            ),
        )
    }
    if (showPhotoSheet) {
        ProductEditorPhotoSheet(
            hasPhoto = state.photoUri != null || state.photoUrl != null,
            onDismiss = { showPhotoSheet = false },
            onTakePhoto = {
                showPhotoSheet = false
                if (state.canEdit) photoActions.takePhoto()
            },
            onChoosePhoto = {
                showPhotoSheet = false
                if (state.canEdit) photoActions.choosePhoto()
            },
            onRemovePhoto = {
                showPhotoSheet = false
                if (state.canEdit) {
                    viewModel.removePhoto()
                    photoActions.discardCapture()
                }
            },
        )
    }
    if (showCollections) {
        ProductEditorCollectionsSheet(
            state = state,
            onQueryChange = viewModel::setCollectionQuery,
            onSelect = viewModel::selectCollection,
            onCreate = viewModel::createCollection,
            onRetry = viewModel::retryCollection,
            onDismiss = { if (!state.isCreatingCollection) showCollections = false },
        )
    }
    if (showDiscard) {
        ProductEditorDiscardDialog(
            message = if (state.isService) {
                R.string.product_editor_discard_service_message
            } else {
                R.string.product_editor_discard_product_message
            },
            onDismiss = { showDiscard = false },
            onDiscard = {
                showDiscard = false
                leaveEditor()
            },
        )
    }
}

private data class ProductEditorActions(
    val back: () -> Unit = {},
    val save: () -> Unit = {},
    val retry: () -> Unit = {},
    val dismissError: () -> Unit = {},
    val name: (String) -> Unit = {},
    val barcode: (String) -> Unit = {},
    val scan: () -> Unit = {},
    val photo: () -> Unit = {},
    val collections: () -> Unit = {},
    val price: (String) -> Unit = {},
    val pricing: () -> Unit = {},
    val adjustStock: (String) -> Unit = {},
)

@Composable
private fun ProductEditorContent(
    state: ProductEditorState,
    actions: ProductEditorActions = ProductEditorActions(),
    scanning: Boolean = false,
    pickingPhoto: Boolean = false,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val locale = LocalConfiguration.current.locales[0]
    val priceFormat = remember(state.country, locale) { catalogPriceFormatter(state.country, locale) }
    val currencyPrefix = (priceFormat as? DecimalFormat)?.decimalFormatSymbols?.currencySymbol
        ?.takeIf { state.country != null && state.country.name != "ZZ" }
    val canEdit = state.canEdit && !pickingPhoto && !scanning

    Column(Modifier
        .fillMaxSize()
        .background(colors.background.surface)) {
        ProductEditorToolbar(
            title = stringResource(state.titleResource()),
            onBack = actions.back,
            actionLabel = stringResource(R.string.product_editor_save),
            actionEnabled = canEdit && state.canSave,
            actionLoading = state.isSaving,
            onAction = actions.save,
        )
        val error = state.error
        when {
            error != null -> ProductEditorErrorBanner(error, state.canRetry && !pickingPhoto && !scanning, actions)
            state.isOffline -> ProductEditorAvailability(R.string.product_editor_offline)
            !state.isLoading && !state.canManageCatalog -> {
                ProductEditorAvailability(R.string.product_editor_access_required)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MoshiLoadingIndicator()
            }
        } else if (error != ProductEditorError.LOAD) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = spacing.space3,
                    bottom = spacing.space6,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ProductEditorPhotoTile(state.photoUri, state.photoUrl, canEdit, actions.photo, state.name)
                    }
                }
                item {
                    Spacer(Modifier.height(spacing.space1))
                    MoshiSection(title = stringResource(R.string.product_editor_details)) {
                        MoshiTextField(
                            value = state.name,
                            onValueChange = actions.name,
                            label = stringResource(
                                if (state.isService) R.string.product_editor_service_name else R.string.product_editor_product_name,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        )
                        if (state.showsBarcode) {
                            MoshiTextField(
                                value = state.barcode,
                                onValueChange = actions.barcode,
                                label = stringResource(R.string.product_editor_barcode),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canEdit,
                                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                                trailingAction = MoshiTextFieldTrailingAction(
                                    icon = R.drawable.ic_fluent_barcode_scanner_24_regular,
                                    contentDescription = stringResource(R.string.inventory_scan),
                                    onClick = actions.scan,
                                ),
                            )
                        }
                        ProductEditorCollectionRow(
                            collection = state.selectedCollection,
                            enabled = canEdit,
                            onClick = actions.collections,
                        )
                    }
                }
                item {
                    Box(Modifier.padding(start = spacing.space4, top = spacing.space5, end = spacing.space4, bottom = spacing.space4)) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(spacing.space2)
                                .background(colors.background.subtle, RoundedCornerShape(spacing.space1)),
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(spacing.space1))
                    MoshiSection(title = stringResource(R.string.product_editor_pricing)) {
                        if (state.isService) {
                            ProductEditorDecimalField(
                                value = state.price,
                                onValueChange = actions.price,
                                label = R.string.product_editor_selling_price,
                                enabled = canEdit,
                                error = state.priceError?.priceMessage(),
                                prefixText = currencyPrefix,
                            )
                        } else {
                            val missingValue = if (currencyPrefix != null) {
                                priceFormat.format(0)
                            } else {
                                stringResource(R.string.product_editor_zero_amount)
                            }
                            ProductEditorPricingRow(
                                price = state.price.moneyAmount()?.let(priceFormat::format) ?: missingValue,
                                cost = state.unitCost.moneyAmount()?.let(priceFormat::format) ?: missingValue,
                                enabled = canEdit,
                                onClick = actions.pricing,
                            )
                        }
                    }
                }
                if (!state.isService && state.locations.isNotEmpty()) {
                    item {
                        Box(Modifier.padding(start = spacing.space4, top = spacing.space5, end = spacing.space4, bottom = spacing.space4)) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(spacing.space2)
                                    .background(colors.background.subtle, RoundedCornerShape(spacing.space1)),
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(spacing.space1))
                        MoshiSection(
                            title = stringResource(R.string.product_editor_inventory),
                            description = stringResource(R.string.product_editor_location),
                        )
                    }
                    items(state.locations, key = { "summary-${it.id}" }) { location ->
                        ProductEditorInventorySummary(
                            location = location,
                            enabled = canEdit,
                            onClick = { actions.adjustStock(location.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorErrorBanner(error: ProductEditorError, canRetry: Boolean, actions: ProductEditorActions) {
    val message = when (error) {
        ProductEditorError.LOAD -> R.string.product_editor_load_error
        ProductEditorError.SAVE -> R.string.product_editor_save_error
        ProductEditorError.SAVE_OFFLINE -> R.string.product_editor_save_offline
        ProductEditorError.SAVE_TIMEOUT -> R.string.product_editor_save_timeout
        ProductEditorError.SAVE_ACCESS -> R.string.product_editor_save_access
        ProductEditorError.BARCODE_ALREADY_EXISTS -> R.string.product_editor_barcode_taken
        ProductEditorError.PRODUCT_VALIDATION -> R.string.product_editor_product_validation
        ProductEditorError.PHOTO_SAVE -> R.string.product_editor_photo_save_error
        ProductEditorError.PHOTO -> R.string.product_editor_photo_error
        ProductEditorError.SCAN -> R.string.product_editor_scan_error
    }
    val dismissible = error == ProductEditorError.PHOTO || error == ProductEditorError.SCAN
    if (canRetry || dismissible) {
        MoshiProjectBanner(
            message = stringResource(message),
            actionLabel = stringResource(if (canRetry) R.string.product_editor_retry else R.string.product_editor_dismiss),
            onAction = if (canRetry) actions.retry else actions.dismissError,
            variant = MoshiProjectBannerVariant.Error,
        )
    } else {
        ProductEditorAvailability(message, error = true)
    }
}

@Composable
private fun ProductEditorAvailability(@StringRes message: Int, error: Boolean = false) {
    val treatment = if (error) Moshi.colors.status.danger else Moshi.colors.status.warning
    val spacing = Moshi.spacing
    MoshiText(
        text = stringResource(message),
        modifier = Modifier
            .fillMaxWidth()
            .background(treatment.background)
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3)
            .semantics { liveRegion = LiveRegionMode.Polite },
        role = MoshiTextRole.Copy14,
        color = treatment.foreground,
    )
}

@Composable
private fun ProductEditorCollectionRow(
    collection: ProductEditorCollection?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val value = collection?.name ?: stringResource(R.string.product_editor_choose_collection)
    ProductEditorSummaryRow(
        label = stringResource(R.string.product_editor_collection),
        modifier = Modifier.heightIn(min = Moshi.spacing.space16),
        enabled = enabled,
        onClick = onClick,
        leadingIcon = R.drawable.ic_fluent_folder_24_regular,
    ) {
        MoshiText(
            text = value,
            role = MoshiTextRole.Copy14,
            color = Moshi.colors.foreground.secondary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ProductEditorPricingRow(price: String, cost: String, enabled: Boolean, onClick: () -> Unit) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.space3))
            .background(colors.background.subtle)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = spacing.space12)
            .padding(spacing.space4)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            MoshiText(
                text = stringResource(R.string.product_editor_selling_price),
                role = MoshiTextRole.Copy14,
                color = colors.foreground.secondary,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = price,
                role = MoshiTextRole.Copy16,
                color = colors.foreground.primary,
            )
        }
        Spacer(Modifier.width(spacing.space4))
        Spacer(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(colors.border.default),
        )
        Spacer(Modifier.width(spacing.space4))
        Column(Modifier.weight(1f)) {
            MoshiText(
                text = stringResource(R.string.product_editor_unit_cost),
                role = MoshiTextRole.Copy14,
                color = colors.foreground.secondary,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = cost,
                role = MoshiTextRole.Copy16,
                color = colors.foreground.primary,
            )
        }
    }
}

@Composable
private fun ProductEditorSummaryRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    summary: @Composable () -> Unit,
) {
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(spacing.space3)
    val containerModifier = modifier
        .clip(shape)
        .background(Moshi.colors.background.subtle)
    if (enabled) {
        MoshiListItem(
            label = label,
            onClick = onClick,
            modifier = containerModifier,
            leadingIcon = leadingIcon,
            trailing = {
                Row(
                    modifier = Modifier.fillMaxWidth(0.66f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { summary() }
                    Spacer(Modifier.width(spacing.space3))
                    MoshiListItemChevron()
                }
            },
        )
    } else {
        Row(
            modifier = containerModifier
                .fillMaxWidth()
                .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.space6),
                    tint = Moshi.colors.foreground.primary,
                )
            }
            MoshiText(label, Modifier.weight(1f), role = MoshiTextRole.Heading16)
            Box(Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) { summary() }
        }
    }
}

@Composable
private fun ProductEditorInventorySummary(
    location: ProductEditorLocation,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val (statusLabelResource, statusTone) = when (location.stockStatus) {
        ProductStockStatus.NOT_SET -> R.string.product_editor_stock_not_set to MoshiTagTone.Neutral
        ProductStockStatus.OUT_OF_STOCK -> R.string.product_editor_stock_out to MoshiTagTone.Error
        ProductStockStatus.LOW_STOCK -> R.string.product_editor_stock_low to MoshiTagTone.Warning
        ProductStockStatus.IN_STOCK -> R.string.product_editor_stock_in to MoshiTagTone.Success
    }
    val statusLabel = stringResource(statusLabelResource)
    val quantity = location.quantity.ifBlank { stringResource(R.string.product_editor_zero) }
    val label = stringResource(R.string.product_editor_adjust_stock_label, location.name, quantity)
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (enabled && isHovered) colors.action.subtle.hovered else colors.background.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.foreground.secondary),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = statusLabel
            }
            .padding(horizontal = spacing.space4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = spacing.space16).padding(vertical = spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                MoshiText(
                    text = location.name,
                    role = MoshiTextRole.Label16,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(spacing.space1))
                MoshiTag(label = statusLabel, tone = statusTone)
            }
            Spacer(Modifier.width(spacing.space3))
            MoshiQuantityButton(
                quantity = quantity,
                label = label,
                enabled = enabled,
            )
        }
        MoshiDivider(Modifier.align(Alignment.BottomCenter))
    }
}

@StringRes
private fun ProductEditorState.titleResource(): Int {
    return when {
        isService && isEditing -> R.string.product_editor_edit_service
        isService -> R.string.product_editor_new_service
        isEditing -> R.string.product_editor_edit_product
        else -> R.string.product_editor_new_product
    }
}

@ScreenPreview
@Composable
private fun ProductEditorItemPreview() {
    MoshiTheme {
        ProductEditorContent(
            ProductEditorState(
                isLoading = false,
                canManageCatalog = true,
                country = CountryIsoCode.NI,
                locations = listOf(ProductEditorLocation(id = "1", name = "Water 7")),
            ),
        )
    }
}

@ScreenPreview
@Composable
private fun ProductEditorServicePreview() {
    MoshiTheme {
        ProductEditorContent(
            ProductEditorState(
                productType = ProductType.SERVICE,
                isLoading = false,
                canManageCatalog = true,
                country = CountryIsoCode.NI,
            ),
        )
    }
}
