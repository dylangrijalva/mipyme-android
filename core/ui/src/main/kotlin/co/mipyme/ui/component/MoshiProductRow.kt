package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ripple
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import androidx.compose.ui.tooling.preview.Preview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

/** Display-only values; consumers own stock, status, formatting, and image loading. */
@Composable
fun MoshiProductRow(
    name: String,
    price: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String? = null,
    tagTone: MoshiTagTone = MoshiTagTone.Neutral,
    showDivider: Boolean = true,
    image: (@Composable () -> Unit)? = null,
) {
    val spacing = Moshi.spacing
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(
        modifier = modifier.fillMaxWidth()
            .height((56.dp + spacing.space2 * 2) * fontScale)
            .background(Moshi.colors.background.surface)
            .clickable(enabled = enabled, interactionSource = null, indication = ripple(), role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (image == null) MoshiMonogram(name) else image()
            Spacer(Modifier.width(spacing.space3))
            Column(Modifier.weight(1f)) {
                MoshiText(
                    name,
                    role = MoshiTextRole.Copy16,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = Moshi.typography.label12.fontSize,
                        maxFontSize = Moshi.typography.copy16.fontSize,
                    ),
                )
                if (tag != null) {
                    Spacer(Modifier.height(spacing.space2))
                    MoshiTag(label = tag, tone = tagTone)
                }
            }
            Spacer(Modifier.width(spacing.space3))
            Column(horizontalAlignment = Alignment.End) {
                MoshiText(price, role = MoshiTextRole.Copy16, textAlign = TextAlign.End, maxLines = 1)
                Spacer(Modifier.height(spacing.space1))
                MoshiText(
                    supportingText,
                    role = MoshiTextRole.Copy14,
                    color = Moshi.colors.foreground.secondary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
        if (showDivider) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                MoshiDivider()
            }
        }
    }
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowInStockPreview() {
    ProductRowPreviewContent(showDivider = true)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowDraftPreview() {
    ProductRowPreviewContent(tag = R.string.moshi_badge_preview_label, supportingText = R.string.moshi_product_preview_zero_stock)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowArchivedPreview() {
    ProductRowPreviewContent(tag = R.string.moshi_product_preview_archived)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowOutOfStockPreview() {
    ProductRowPreviewContent(
        supportingText = R.string.moshi_product_preview_zero_stock,
        tag = R.string.moshi_product_preview_out_of_stock,
        tagTone = MoshiTagTone.Error,
    )
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowStockNotSetPreview() {
    ProductRowPreviewContent(supportingText = R.string.moshi_product_preview_stock_not_set)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowServicePreview() {
    ProductRowPreviewContent(
        name = R.string.moshi_product_preview_service_name,
        supportingText = R.string.moshi_product_preview_service,
    )
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowDisabledPreview() {
    ProductRowPreviewContent(enabled = false)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowLongNamePreview() {
    ProductRowPreviewContent(name = R.string.moshi_product_preview_long_name, tag = R.string.moshi_badge_preview_label)
}

@Preview(widthDp = 800)
@Composable
private fun MoshiProductRowLargeTextPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        ProductRowPreviewContent(
            name = R.string.moshi_product_preview_long_name,
            supportingText = R.string.moshi_product_preview_zero_stock,
            tag = R.string.moshi_product_preview_out_of_stock,
            tagTone = MoshiTagTone.Error,
            narrow = true,
        )
    }
}

@Composable
private fun ProductRowPreviewContent(
    @StringRes name: Int = R.string.moshi_product_preview_name,
    @StringRes supportingText: Int = R.string.moshi_product_preview_stock,
    @StringRes tag: Int? = null,
    tagTone: MoshiTagTone = MoshiTagTone.Neutral,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    narrow: Boolean = false,
) {
    Row {
        listOf(false, true).forEach { darkTheme ->
            MoshiTheme(darkTheme = darkTheme) {
                MoshiProductRow(
                    name = stringResource(name),
                    price = stringResource(R.string.moshi_product_preview_price),
                    supportingText = stringResource(supportingText),
                    tag = tag?.let { stringResource(it) },
                    tagTone = tagTone,
                    enabled = enabled,
                    showDivider = showDivider,
                    modifier = Modifier.width(if (narrow) 320.dp else 400.dp),
                    onClick = {},
                )
            }
        }
    }
}
