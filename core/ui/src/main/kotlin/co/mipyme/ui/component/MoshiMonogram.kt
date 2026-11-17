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
    val palette = colors.identity.swatches
    val (background, foreground) = if (colorSeed == null) {
        colors.background.subtle to colors.foreground.secondary
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
            .size(width = spacing.space16, height = spacing.space12)
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
