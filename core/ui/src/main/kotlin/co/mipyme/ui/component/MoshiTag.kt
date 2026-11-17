package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

enum class MoshiTagTone {
    Neutral,
    Warning,
    Error,
    Success,
}

@Composable
fun MoshiTag(
    label: String,
    modifier: Modifier = Modifier,
    tone: MoshiTagTone = MoshiTagTone.Neutral,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val background = when (tone) {
        MoshiTagTone.Neutral -> colors.status.neutral.background
        MoshiTagTone.Warning -> colors.status.warning.background
        MoshiTagTone.Error -> colors.status.danger.background
        MoshiTagTone.Success -> colors.status.success.background
    }
    val foreground = when (tone) {
        MoshiTagTone.Neutral -> colors.status.neutral.foreground
        MoshiTagTone.Warning -> colors.status.warning.foreground
        MoshiTagTone.Error -> colors.status.danger.foreground
        MoshiTagTone.Success -> colors.status.success.foreground
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(spacing.space1))
            .padding(horizontal = spacing.space2, vertical = spacing.space1),
    ) {
        MoshiText(text = label.uppercase(), role = MoshiTextRole.Label12, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@MoshiPreview
@Composable
private fun MoshiTagPreview() {
    MoshiTheme {
        Column {
            MoshiTag(
                label = stringResource(R.string.moshi_badge_preview_label),
                tone = MoshiTagTone.Neutral,
            )
            Spacer(Modifier.height(Moshi.spacing.space2))
            MoshiTag(
                label = stringResource(R.string.moshi_badge_preview_label),
                tone = MoshiTagTone.Warning,
            )
        }
    }
}
