package co.mipyme.ui.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

/** Equal width, text-only tabs. [pagePosition] is read during layout and may be fractional while a pager scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoshiTabs(
    labels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pagePosition: () -> Float = { selectedIndex.toFloat() },
) {
    require(labels.isNotEmpty())
    require(selectedIndex in labels.indices)
    val colors = Moshi.colors
    val spacing = Moshi.spacing

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = colors.foreground.primary,
        divider = {},
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorLayout { measurable, constraints, positions ->
                    val (left, width) = indicatorBounds(positions, pagePosition())
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width.roundToPx(), maxWidth = width.roundToPx()),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(
                            x = left.roundToPx(),
                            y = constraints.maxHeight - placeable.height,
                        )
                    }
                },
                width = Dp.Unspecified,
                height = 2.dp,
                color = colors.selection.indicator,
            )
        },
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier.heightIn(min = spacing.space12),
                selectedContentColor = colors.foreground.primary,
                unselectedContentColor = colors.foreground.disabled,
                text = {
                    MoshiText(
                        text = label,
                        role = MoshiTextRole.Label16,
                        color = if (selectedIndex == index) colors.foreground.primary else colors.foreground.disabled,
                    )
                },
            )
        }
    }
}

private fun indicatorBounds(positions: List<TabPosition>, pagePosition: Float): Pair<Dp, Dp> {
    if (positions.isEmpty()) return 0.dp to 0.dp
    val boundedPosition = pagePosition.coerceIn(0f, positions.lastIndex.toFloat())
    val startIndex = boundedPosition.toInt()
    val endIndex = (startIndex + 1).coerceAtMost(positions.lastIndex)
    val fraction = boundedPosition - startIndex
    val start = positions[startIndex]
    val end = positions[endIndex]
    val startWidth = start.contentWidth
    val endWidth = end.contentWidth
    val startLeft = start.left + (start.width - startWidth) / 2
    val endLeft = end.left + (end.width - endWidth) / 2
    return lerp(startLeft, endLeft, fraction) to lerp(startWidth, endWidth, fraction)
}

@MoshiPreview
@Composable
private fun MoshiTabsPreview() {
    MoshiTheme {
        MoshiTabs(
            labels = listOf(
                stringResource(R.string.moshi_tabs_preview_products),
                stringResource(R.string.moshi_tabs_preview_categories),
            ),
            selectedIndex = 0,
            onTabSelected = {},
        )
    }
}
