package co.mipyme.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import co.mipyme.R
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.database.CatalogGrouping
import co.mipyme.database.CatalogQuery
import co.mipyme.ui.Moshi
import co.mipyme.ui.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InventoryFilterSheet(
    draft: CatalogQuery,
    categories: List<InventoryCategoryRow>,
    onChange: (CatalogQuery) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = Moshi.spacing
    MoshiBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.inventory_filters)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            MoshiButton(onClick = onDismiss, variant = MoshiButtonVariant.Text) {
                MoshiText(stringResource(R.string.inventory_close))
            }
            FilterHeading(stringResource(R.string.inventory_filter_category))
            categories.forEach { category ->
                FilterCheckbox(category.name, category.id in draft.categoryIds) {
                    onChange(draft.copy(categoryIds = draft.categoryIds.toggle(category.id)))
                }
            }
            FilterCheckbox(stringResource(R.string.inventory_uncategorized), draft.includeUncategorized) {
                onChange(draft.copy(includeUncategorized = !draft.includeUncategorized))
            }
            Spacer(Modifier.height(spacing.space4))
            FilterHeading(stringResource(R.string.inventory_filter_type))
            Column(Modifier.selectableGroup()) {
                listOf(null, ProductType.ITEM, ProductType.SERVICE).forEach { type ->
                    MoshiSelectableItem(
                        label = stringResource(when (type) {
                            null -> R.string.inventory_type_all
                            ProductType.ITEM -> R.string.inventory_products
                            ProductType.SERVICE -> R.string.inventory_services
                        }),
                        selected = draft.type == type,
                        onClick = { onChange(draft.copy(type = type)) },
                    )
                }
            }
            Spacer(Modifier.height(spacing.space4))
            FilterHeading(stringResource(R.string.inventory_filter_status))
            ProductStatus.entries.forEach { status ->
                FilterCheckbox(stringResource(status.inventoryLabel()), status in draft.statuses) {
                    onChange(draft.copy(statuses = draft.statuses.toggle(status)))
                }
            }
            Spacer(Modifier.height(spacing.space4))
            FilterHeading(stringResource(R.string.inventory_group_by))
            Column(Modifier.selectableGroup()) {
                CatalogGrouping.entries.forEach { grouping ->
                    MoshiSelectableItem(
                        label = stringResource(when (grouping) {
                            CatalogGrouping.NONE -> R.string.inventory_group_none
                            CatalogGrouping.CATEGORY -> R.string.inventory_filter_category
                            CatalogGrouping.STATUS -> R.string.inventory_filter_status
                        }),
                        selected = draft.grouping == grouping,
                        onClick = { onChange(draft.copy(grouping = grouping)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.space4))
        MoshiButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
            MoshiText(stringResource(R.string.inventory_apply))
        }
        Spacer(Modifier.height(spacing.space2))
        MoshiButton(onClick = { onChange(draft.clearFilters()) }, modifier = Modifier.fillMaxWidth(), variant = MoshiButtonVariant.Text) {
            MoshiText(stringResource(R.string.inventory_clear_filters))
        }
    }
}

@Composable
private fun FilterHeading(label: String) {
    MoshiText(label, role = MoshiTextRole.Heading20, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Moshi.spacing.space2))
}

@Composable
private fun FilterCheckbox(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MoshiText(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Moshi.spacing.space3))
        MoshiCheckbox(checked, { onToggle() }, Modifier.semantics { contentDescription = label })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InventoryAddSheet(onDismiss: () -> Unit) {
    MoshiBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.inventory_add)) {
        MoshiListItem(stringResource(R.string.inventory_add_product), onDismiss, leadingIcon = R.drawable.ic_fluent_box_24_regular)
        MoshiListItem(stringResource(R.string.inventory_add_service), onDismiss, leadingIcon = R.drawable.ic_fluent_wrench_24_regular)
        MoshiListItem(stringResource(R.string.inventory_add_category), onDismiss, leadingIcon = R.drawable.ic_fluent_tag_24_regular)
    }
}

internal fun ProductStatus.inventoryLabel(): Int {
    return when (this) {
        ProductStatus.ACTIVE -> R.string.inventory_status_active
        ProductStatus.DRAFT -> R.string.inventory_status_draft
        ProductStatus.ARCHIVED -> R.string.inventory_status_archived
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) this - value else this + value
}
