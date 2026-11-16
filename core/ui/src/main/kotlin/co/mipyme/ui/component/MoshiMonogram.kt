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
fun MoshiMonogram(name: String, modifier: Modifier = Modifier) {
    val spacing = Moshi.spacing
    val shortenedName = remember(name) {
        val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        val firstWords = words.take(2).joinToString(" ")
        if (words.size > 2) "$firstWords…" else firstWords
    }
    Box(
        modifier = modifier
            .size(spacing.space16)
            .clip(RoundedCornerShape(spacing.space2))
            .background(Moshi.colors.gray100)
            .padding(horizontal = spacing.space1)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        MoshiText(
            text = shortenedName,
            role = MoshiTextRole.Label12,
            color = Moshi.colors.gray900,
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
