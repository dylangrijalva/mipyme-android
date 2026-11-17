package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

enum class MoshiProjectBannerVariant {
    Default,
    Success,
    Warning,
    Error,
}

/** A persistent, project-wide state with an action that resolves it. The caller owns visibility. */
@Composable
fun MoshiProjectBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MoshiProjectBannerVariant = MoshiProjectBannerVariant.Default,
    icon: Painter? = null,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val background = when (variant) {
        MoshiProjectBannerVariant.Default -> colors.status.neutral.background
        MoshiProjectBannerVariant.Success -> colors.status.information.background
        MoshiProjectBannerVariant.Warning -> colors.status.warning.background
        MoshiProjectBannerVariant.Error -> colors.status.danger.background
    }
    val border = when (variant) {
        MoshiProjectBannerVariant.Default -> colors.status.neutral.border
        MoshiProjectBannerVariant.Success -> colors.status.information.border
        MoshiProjectBannerVariant.Warning -> colors.status.warning.border
        MoshiProjectBannerVariant.Error -> colors.status.danger.border
    }
    val foreground = when (variant) {
        MoshiProjectBannerVariant.Default -> colors.status.neutral.foreground
        MoshiProjectBannerVariant.Success -> colors.status.information.foreground
        MoshiProjectBannerVariant.Warning -> colors.status.warning.foreground
        MoshiProjectBannerVariant.Error -> colors.status.danger.foreground
    }
    val actionColor = when (variant) {
        MoshiProjectBannerVariant.Default -> colors.status.neutral.strongForeground
        MoshiProjectBannerVariant.Success -> colors.status.information.strongForeground
        MoshiProjectBannerVariant.Warning -> colors.status.warning.strongForeground
        MoshiProjectBannerVariant.Error -> colors.status.danger.strongForeground
    }
    val focusColor = when (variant) {
        MoshiProjectBannerVariant.Default -> colors.status.neutral.focus
        MoshiProjectBannerVariant.Success -> colors.status.information.focus
        MoshiProjectBannerVariant.Warning -> colors.status.warning.focus
        MoshiProjectBannerVariant.Error -> colors.status.danger.focus
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(border, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(border, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            }
            .semantics {
                isTraversalGroup = true
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = spacing.space6, vertical = spacing.space2),
    ) {
        val messageContent: @Composable (Modifier) -> Unit = { contentModifier ->
            Row(modifier = contentModifier, verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(spacing.space4),
                        tint = foreground,
                    )
                    Spacer(Modifier.width(spacing.space2))
                }
                MoshiText(message, role = MoshiTextRole.Copy14, color = foreground)
            }
        }
        val action: @Composable () -> Unit = {
            ProjectBannerAction(actionLabel, onAction, actionColor, foreground, focusColor)
        }
        // Geist switches to an inline banner at its 768 px medium breakpoint.
        if (maxWidth + spacing.space12 >= 768.dp) {
            val actionMaxWidth = maxWidth / 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                messageContent(Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(spacing.space2))
                Box(Modifier.widthIn(max = actionMaxWidth)) { action() }
            }
        } else {
            Column {
                messageContent(Modifier.fillMaxWidth())
                Spacer(Modifier.height(spacing.space2))
                Row {
                    if (icon != null) Spacer(Modifier.width(spacing.space6))
                    action()
                }
            }
        }
    }
}

@Composable
private fun ProjectBannerAction(
    label: String,
    onClick: () -> Unit,
    color: Color,
    hoverColor: Color,
    focusColor: Color,
) {
    val spacing = Moshi.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, focusColor, shape) else Modifier)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = color),
                onClick = onClick,
            )
            .padding(vertical = spacing.space1),
        contentAlignment = Alignment.CenterStart,
    ) {
        MoshiText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
            },
            role = MoshiTextRole.Label14,
            emphasis = MoshiTextEmphasis.Strong,
            color = if (hovered) hoverColor else color,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiProjectBannerPreview() {
    MoshiTheme {
        Column(Modifier.background(Moshi.colors.background.surface)) {
            MoshiProjectBanner(
                message = stringResource(R.string.moshi_project_banner_preview_default),
                actionLabel = stringResource(R.string.moshi_project_banner_preview_review),
                onAction = {},
                icon = painterResource(R.drawable.ic_fluent_shield_checkmark_24_regular),
            )
            Spacer(Modifier.height(Moshi.spacing.space4))
            MoshiProjectBanner(
                message = stringResource(R.string.moshi_project_banner_preview_success),
                actionLabel = stringResource(R.string.moshi_project_banner_preview_disable),
                onAction = {},
                variant = MoshiProjectBannerVariant.Success,
                icon = painterResource(R.drawable.ic_fluent_shield_checkmark_24_regular),
            )
            Spacer(Modifier.height(Moshi.spacing.space4))
            MoshiProjectBanner(
                message = stringResource(R.string.moshi_project_banner_preview_warning),
                actionLabel = stringResource(R.string.moshi_project_banner_preview_restore),
                onAction = {},
                variant = MoshiProjectBannerVariant.Warning,
                icon = painterResource(R.drawable.ic_fluent_arrow_counterclockwise_24_regular),
            )
            Spacer(Modifier.height(Moshi.spacing.space4))
            MoshiProjectBanner(
                message = stringResource(R.string.moshi_project_banner_preview_error),
                actionLabel = stringResource(R.string.moshi_project_banner_preview_payment),
                onAction = {},
                variant = MoshiProjectBannerVariant.Error,
                icon = painterResource(R.drawable.ic_fluent_warning_24_regular),
            )
        }
    }
}
