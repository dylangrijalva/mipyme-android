package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return

    val spacing = Moshi.spacing
    Box(
        modifier = modifier
            .clearAndSetSemantics { }
            .sizeIn(minWidth = spacing.space5, minHeight = spacing.space5)
            .background(Moshi.colors.background.inverse, CircleShape)
            .padding(horizontal = spacing.space1),
        contentAlignment = Alignment.Center,
    ) {
        MoshiText(
            text = stringResource(R.string.moshi_badge_count, count),
            role = MoshiTextRole.Label12,
            color = Moshi.colors.foreground.inverse,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiBadgePreview() {
    MoshiTheme {
        MoshiBadge(count = 2)
    }
}
