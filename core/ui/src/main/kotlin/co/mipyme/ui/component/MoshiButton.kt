package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
    val focused by interactionSource.collectIsFocusedAsState()
    val background = when {
        !canActivate -> when (variant) {
            MoshiButtonVariant.Primary,
            MoshiButtonVariant.Secondary,
            MoshiButtonVariant.Destructive -> colors.background.disabled
            MoshiButtonVariant.Outline,
            MoshiButtonVariant.DestructiveOutline,
            MoshiButtonVariant.Text -> Color.Transparent
        }
        variant == MoshiButtonVariant.Primary && hovered -> colors.action.primary.hovered
        variant == MoshiButtonVariant.Primary -> colors.action.primary.background
        variant == MoshiButtonVariant.Secondary && hovered -> colors.action.secondary.hovered
        variant == MoshiButtonVariant.Secondary -> colors.action.secondary.background
        variant == MoshiButtonVariant.Destructive && hovered ->
            colors.action.danger.hovered
        variant == MoshiButtonVariant.Destructive -> colors.action.danger.background
        hovered -> colors.action.subtle.hovered
        else -> Color.Transparent
    }
    val foreground = when {
        !canActivate -> colors.foreground.disabled
        variant == MoshiButtonVariant.Primary -> colors.action.primary.foreground
        variant == MoshiButtonVariant.Secondary -> colors.foreground.primary
        variant == MoshiButtonVariant.Destructive -> colors.action.danger.foreground
        variant == MoshiButtonVariant.DestructiveOutline -> colors.foreground.danger
        else -> colors.foreground.primary
    }
    val minimumSize = when (size) {
        MoshiButtonSize.Default -> spacing.space12
    }
    val radius = 12.dp
    val shape = RoundedCornerShape(radius)
    val borderColor = when {
        !canActivate -> when (variant) {
            MoshiButtonVariant.Secondary,
            MoshiButtonVariant.Outline,
            MoshiButtonVariant.DestructiveOutline -> colors.control.disabledBorder
            MoshiButtonVariant.Primary,
            MoshiButtonVariant.Destructive,
            MoshiButtonVariant.Text -> Color.Transparent
        }
        focused -> colors.border.focus
        variant == MoshiButtonVariant.DestructiveOutline -> colors.border.danger
        variant == MoshiButtonVariant.Secondary || variant == MoshiButtonVariant.Outline -> colors.border.default
        else -> Color.Transparent
    }
    val border = Modifier.border(
        width = if (focused && canActivate) 2.dp else 1.dp,
        color = borderColor,
        shape = shape,
    )
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
        LocalMoshiTextRole provides MoshiTextRole.Button14,
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
                .background(Moshi.colors.background.surface)
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
