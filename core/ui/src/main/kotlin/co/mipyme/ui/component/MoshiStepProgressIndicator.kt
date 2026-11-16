package co.mipyme.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

/** Shows the current step and completed steps. [currentStep] is zero-based. */
@Composable
fun MoshiStepProgressIndicator(
    stepCount: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    if (stepCount <= 0) {
        return
    }

    val activeStep = currentStep.coerceIn(0, stepCount - 1)
    val description = stringResource(
        R.string.moshi_step_progress_indicator,
        activeStep + 1,
        stepCount,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalArrangement = Arrangement.spacedBy(Moshi.spacing.space1),
    ) {
        repeat(stepCount) { step ->
            StepProgressSegment(
                active = step <= activeStep,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepProgressSegment(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (active) Moshi.colors.gray1000 else Moshi.colors.gray400,
        label = "stepProgressSegmentColor",
    )

    Box(
        modifier = modifier
            .clearAndSetSemantics { }
            .height(Moshi.spacing.space1)
            .clip(CircleShape)
            .background(color),
    )
}

@MoshiPreview
@Composable
private fun MoshiStepProgressIndicatorPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Moshi.colors.background100)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiStepProgressIndicator(stepCount = 4, currentStep = 0)
            MoshiStepProgressIndicator(stepCount = 4, currentStep = 1)
            MoshiStepProgressIndicator(stepCount = 4, currentStep = 3)
        }
    }
}
