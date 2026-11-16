package co.mipyme.screen.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.component.MoshiMonogram
import co.mipyme.ui.component.MoshiStatusBadge
import co.mipyme.ui.component.MoshiStatusBadgeTone
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import coil3.compose.SubcomposeAsyncImage

@Composable
internal fun InventoryProductListRow(row: InventoryProductRow, modifier: Modifier = Modifier) {
    val spacing = Moshi.spacing
    Row(
        modifier = modifier.fillMaxWidth()
            .clickable(interactionSource = null, indication = ripple(), onClick = {})
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            model = row.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(spacing.space16).clip(RoundedCornerShape(spacing.space2)),
            contentScale = ContentScale.Crop,
            loading = { MoshiMonogram(row.name, Modifier.fillMaxSize()) },
            error = { MoshiMonogram(row.name, Modifier.fillMaxSize()) },
        )
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f)) {
            MoshiText(row.name, role = MoshiTextRole.Copy16)
            Spacer(Modifier.height(spacing.space1))
            MoshiText(row.price, role = MoshiTextRole.Copy14, color = Moshi.colors.gray900)
            Spacer(Modifier.height(spacing.space1))
            MoshiText(
                text = if (row.isService) stringResource(R.string.inventory_service) else {
                    row.quantity?.let { stringResource(R.string.inventory_stock_quantity, it) }
                        ?: stringResource(R.string.inventory_stock_unavailable)
                },
                role = MoshiTextRole.Copy14,
                color = Moshi.colors.gray900,
            )
            row.badge?.let { badge ->
                Spacer(Modifier.height(spacing.space2))
                MoshiStatusBadge(
                    label = stringResource(when (badge) {
                        InventoryBadge.DRAFT -> R.string.inventory_status_draft
                        InventoryBadge.ARCHIVED -> R.string.inventory_status_archived
                        InventoryBadge.OUT_OF_STOCK -> R.string.inventory_out_of_stock
                    }),
                    tone = if (badge == InventoryBadge.OUT_OF_STOCK) MoshiStatusBadgeTone.Warning else MoshiStatusBadgeTone.Neutral,
                )
            }
        }
    }
}

@Composable
internal fun InventoryCategoryListRow(row: InventoryCategoryRow) {
    val spacing = Moshi.spacing
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(interactionSource = null, indication = ripple(), onClick = {})
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoshiMonogram(row.name)
        Spacer(Modifier.width(spacing.space3))
        MoshiText(row.name, modifier = Modifier.weight(1f))
    }
}
