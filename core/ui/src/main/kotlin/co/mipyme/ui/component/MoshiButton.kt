package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

enum class MoshiButtonVariant {
    Primary,
    Secondary,
    Outline,
    Destructive,
    DestructiveOutline,
    Text,
}

enum class MoshiButtonSize {
    Default,
}

@Composable
fun MoshiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MoshiButtonVariant = MoshiButtonVariant.Primary,
    size: MoshiButtonSize = MoshiButtonSize.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val canActivate = enabled && !isLoading
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        !canActivate -> colors.background.disabled
        variant == MoshiButtonVariant.Primary && hovered -> colors.background.action.primary.hover
        variant == MoshiButtonVariant.Primary -> colors.background.action.primary.default
        variant == MoshiButtonVariant.Secondary && hovered -> colors.background.action.secondary.hover
        variant == MoshiButtonVariant.Secondary -> colors.background.action.secondary.default
        variant == MoshiButtonVariant.Destructive && hovered -> colors.background.action.destructive.hover
        variant == MoshiButtonVariant.Destructive -> colors.background.action.destructive.default
        hovered -> colors.background.action.secondary.hover
        else -> Color.Transparent
    }
    val foreground = when {
        !canActivate -> colors.foreground.disabled
        variant == MoshiButtonVariant.Primary -> colors.foreground.onAction.primary
        variant == MoshiButtonVariant.Secondary -> colors.foreground.primary
        variant == MoshiButtonVariant.Destructive -> colors.foreground.onAction.destructive
        variant == MoshiButtonVariant.DestructiveOutline -> colors.foreground.action.destructive
        else -> colors.foreground.action.secondary
    }
    val minimumSize = when (size) {
        MoshiButtonSize.Default -> spacing.space12
    }
    val radius = 12.dp
    val shape = RoundedCornerShape(radius)
    val border = when (variant) {
        MoshiButtonVariant.Outline -> Modifier.border(
            width = 1.dp,
            color = if (canActivate) colors.border.interactive else colors.border.default,
            shape = shape,
        )
        MoshiButtonVariant.DestructiveOutline -> Modifier.border(
            width = 1.dp,
            color = if (canActivate) colors.border.status.error else colors.border.default,
            shape = shape,
        )
        else -> Modifier
    }
    val loadingSemantics = if (isLoading) {
        val loadingDescription = stringResource(R.string.moshi_button_loading)
        Modifier.semantics {
            stateDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    } else {
        Modifier
    }

    val contentVisibility = if (isLoading) {
        Modifier
            .clearAndSetSemantics { }
            .layout { measurable, constraints ->
                val contentPlaceable = measurable.measure(constraints)
                layout(contentPlaceable.width, contentPlaceable.height) { }
            }
    } else {
        Modifier
    }

    CompositionLocalProvider(
        LocalMoshiTextRole provides MoshiTextRole.Small,
        LocalMoshiTextColor provides foreground,
        LocalContentColor provides foreground,
    ) {
        Box(
            modifier = modifier
                .sizeIn(minWidth = minimumSize, minHeight = minimumSize)
                .clip(shape)
                .background(background)
                .then(border)
                .clickable(
                    enabled = canActivate,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(color = foreground),
                    onClick = onClick,
                )
                .then(loadingSemantics),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = contentVisibility.padding(
                    horizontal = spacing.space6,
                    vertical = spacing.space2,
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingContent != null) {
                    Box(
                        modifier = Modifier.size(spacing.space6),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingContent()
                    }
                }
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    content()
                }
                trailingContent?.invoke()
            }
            if (isLoading) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(spacing.space4)
                            .clearAndSetSemantics { },
                        color = foreground,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiButtonPreview() {
    MoshiTheme {
        val spacing = Moshi.spacing
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            MoshiButton(onClick = {}) {
                MoshiText("Save")
            }
            MoshiButton(onClick = {}, enabled = false) {
                MoshiText("Save")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Secondary) {
                MoshiText("Skip")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Secondary, enabled = false) {
                MoshiText("Skip")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Secondary, isLoading = true) {
                MoshiText("Skip")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Destructive) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Destructive, enabled = false) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.Destructive, isLoading = true) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.DestructiveOutline) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.DestructiveOutline, enabled = false) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, variant = MoshiButtonVariant.DestructiveOutline, isLoading = true) {
                MoshiText("Delete")
            }
            MoshiButton(onClick = {}, isLoading = true) {
                MoshiText("Save")
            }
            MoshiButton(
                onClick = {},
                trailingContent = { PreviewAddIcon() },
            ) {
                MoshiText("Save and update the product information")
            }
            MoshiButton(onClick = {}, leadingContent = { PreviewAddIcon() }) {
                MoshiText("Save")
            }
            MoshiButton(onClick = {}, isLoading = true, leadingContent = { PreviewAddIcon() }) {
                MoshiText("Save")
            }
        }
    }
}

@Composable
private fun PreviewAddIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_fluent_add_24_regular),
        contentDescription = null,
        modifier = Modifier.size(Moshi.spacing.space5),
    )
}
