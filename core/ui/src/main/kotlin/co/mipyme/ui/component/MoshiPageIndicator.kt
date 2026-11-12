package co.mipyme.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

private val IndicatorHeight = 8.dp
private val SelectedIndicatorWidth = 24.dp

@Composable
fun MoshiPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.moshi_page_indicator, currentPage + 1, pageCount)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(Moshi.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            PageIndicatorMark(selected = page == currentPage)
        }
    }
}

@Composable
private fun PageIndicatorMark(selected: Boolean) {
    val width: Dp by animateDpAsState(
        targetValue = if (selected) SelectedIndicatorWidth else IndicatorHeight,
        label = "pageIndicatorWidth",
    )
    val color by animateColorAsState(
        targetValue = if (selected) Moshi.colors.foreground.primary else Moshi.colors.border.strong,
        label = "pageIndicatorColor",
    )

    Box(
        modifier = Modifier
            .clearAndSetSemantics { }
            .width(width)
            .height(IndicatorHeight)
            .clip(CircleShape)
            .background(color),
    )
}

@MoshiPreview
@Composable
private fun MoshiPageIndicatorPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiPageIndicator(pageCount = 3, currentPage = 0)
            MoshiPageIndicator(pageCount = 3, currentPage = 2)
        }
    }
}
