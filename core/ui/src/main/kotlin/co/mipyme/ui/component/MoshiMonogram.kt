package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiMonogram(
    name: String,
    modifier: Modifier = Modifier,
    colorSeed: String? = null,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val palette = listOf(
        colors.blue200 to colors.blue1000,
        colors.green200 to colors.green1000,
        colors.teal200 to colors.teal1000,
        colors.purple200 to colors.purple1000,
        colors.pink200 to colors.pink1000,
        colors.amber200 to colors.amber1000,
    )
    val (background, foreground) = if (colorSeed == null) {
        colors.gray100 to colors.gray900
    } else {
        palette[Math.floorMod(colorSeed.hashCode(), palette.size)]
    }
    val shortenedName = remember(name) {
        val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        val firstWords = words.take(2).joinToString(" ")
        if (words.size > 2) "$firstWords…" else firstWords
    }
    Box(
        modifier = modifier
            .size(spacing.space16)
            .clip(RoundedCornerShape(spacing.space2))
            .background(background)
            .padding(horizontal = spacing.space1)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        MoshiText(
            text = shortenedName,
            role = if (colorSeed == null) MoshiTextRole.Label12 else MoshiTextRole.Label16,
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiMonogramPreview() {
    MoshiTheme {
        MoshiMonogram(name = stringResource(R.string.moshi_monogram_preview_name))
    }
}
