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
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

enum class MoshiStatusBadgeTone {
    Neutral,
    Warning,
    Error,
    Success,
}

@Composable
fun MoshiStatusBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: MoshiStatusBadgeTone = MoshiStatusBadgeTone.Neutral,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val background = when (tone) {
        MoshiStatusBadgeTone.Neutral -> colors.gray100
        MoshiStatusBadgeTone.Warning -> colors.amber100
        MoshiStatusBadgeTone.Error -> colors.red100
        MoshiStatusBadgeTone.Success -> colors.green100
    }
    val foreground = when (tone) {
        MoshiStatusBadgeTone.Neutral -> colors.gray900
        MoshiStatusBadgeTone.Warning -> colors.amber900
        MoshiStatusBadgeTone.Error -> colors.red900
        MoshiStatusBadgeTone.Success -> colors.green900
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(spacing.space1))
            .padding(horizontal = spacing.space2, vertical = spacing.space1),
    ) {
        MoshiText(text = label, role = MoshiTextRole.Label12, color = foreground)
    }
}

@MoshiPreview
@Composable
private fun MoshiStatusBadgePreview() {
    MoshiTheme {
        Column {
            MoshiStatusBadge(
                label = stringResource(R.string.moshi_badge_preview_label),
                tone = MoshiStatusBadgeTone.Neutral,
            )
            Spacer(Modifier.height(Moshi.spacing.space2))
            MoshiStatusBadge(
                label = stringResource(R.string.moshi_badge_preview_label),
                tone = MoshiStatusBadgeTone.Warning,
            )
        }
    }
}
