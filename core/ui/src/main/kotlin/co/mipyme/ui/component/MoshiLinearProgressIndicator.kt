package co.mipyme.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
            .background(Moshi.colors.progress.track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Moshi.colors.progress.foreground),
        )
    }
}

/** Shows indeterminate progress as a segment sliding across the bar. */
@Composable
fun MoshiLinearProgressIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "indeterminateLinearProgress")
    val position by transition.animateFloat(
        initialValue = -INDETERMINATE_SEGMENT_FRACTION,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
        ),
        label = "indeterminatePosition",
    )

    BoxWithConstraints(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .fillMaxWidth()
            .height(Moshi.spacing.space1)
            .clip(CircleShape)
            .background(Moshi.colors.progress.track),
    ) {
        Box(
            modifier = Modifier
                .offset(x = maxWidth * position)
                .width(maxWidth * INDETERMINATE_SEGMENT_FRACTION)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Moshi.colors.progress.foreground),
        )
    }
}

private const val INDETERMINATE_SEGMENT_FRACTION = 0.3f

@MoshiPreview
@Composable
private fun MoshiLinearProgressIndicatorPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Moshi.colors.background.surface)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiLinearProgressIndicator(progress = 0f)
            MoshiLinearProgressIndicator(progress = 0.25f)
            MoshiLinearProgressIndicator(progress = 0.75f)
            MoshiLinearProgressIndicator(progress = 1f)
            MoshiLinearProgressIndicator()
        }
    }
}
