package co.mipyme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

/** Shows determinate progress as a continuous bar. [progress] is clamped to `0f..1f`. */
@Composable
fun MoshiLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        label = "linearProgress",
    )

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(current = clampedProgress, range = 0f..1f)
            }
            .fillMaxWidth()
            .height(Moshi.spacing.space1)
            .clip(CircleShape)
            .background(Moshi.colors.border.default),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Moshi.colors.foreground.primary),
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiLinearProgressIndicatorPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiLinearProgressIndicator(progress = 0f)
            MoshiLinearProgressIndicator(progress = 0.25f)
            MoshiLinearProgressIndicator(progress = 0.75f)
            MoshiLinearProgressIndicator(progress = 1f)
        }
    }
}
