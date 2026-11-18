package co.mipyme.screen.product_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.component.MoshiBottomSheet
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiProjectBanner
import co.mipyme.ui.component.MoshiProjectBannerVariant
import co.mipyme.ui.component.MoshiSearchField
import co.mipyme.ui.component.MoshiSelectableItem
import co.mipyme.ui.component.MoshiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductEditorCollectionsSheet(
    state: ProductEditorState,
    onQueryChange: (String) -> Unit,
    onSelect: (ProductEditorCollection?) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = Moshi.spacing
    val creating by rememberUpdatedState(state.isCreatingCollection)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden || !creating },
    )
    val enabled = state.canEdit && !state.isCreatingCollection
    MoshiBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.product_editor_choose_collection),
    ) {
        MoshiSearchField(
            value = state.collectionQuery,
            onValueChange = onQueryChange,
            label = stringResource(R.string.product_editor_search_collections),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        Spacer(Modifier.height(spacing.space3))
        if (state.collectionError) {
            MoshiProjectBanner(
                message = stringResource(R.string.product_editor_collection_error),
                actionLabel = stringResource(R.string.product_editor_retry),
                onAction = onRetry,
                variant = MoshiProjectBannerVariant.Error,
            )
            Spacer(Modifier.height(spacing.space2))
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            item {
                MoshiSelectableItem(
                    label = stringResource(R.string.product_editor_no_collection),
                    selected = state.selectedCollection == null,
                    enabled = enabled,
                    onClick = { onSelect(null) },
                )
            }
            items(state.visibleCollections, key = { it.id }) { collection ->
                MoshiSelectableItem(
                    label = collection.name,
                    selected = state.selectedCollection?.id == collection.id,
                    enabled = enabled,
                    onClick = { onSelect(collection) },
                )
            }
            if (state.canCreateCollection || state.isCreatingCollection) {
                item {
                    MoshiButton(
                        onClick = onCreate,
                        modifier = Modifier.fillMaxWidth(),
                        variant = MoshiButtonVariant.Outline,
                        isLoading = state.isCreatingCollection,
                    ) {
                        MoshiText(
                            stringResource(R.string.product_editor_create_collection, state.collectionQuery.trim()),
                        )
                    }
                }
            }
        }
    }
}
