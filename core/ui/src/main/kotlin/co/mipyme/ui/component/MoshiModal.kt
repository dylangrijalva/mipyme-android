package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

data class MoshiModalAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isLoading: Boolean = false,
)

@Composable
fun MoshiModal(
    title: String,
    onDismissRequest: () -> Unit,
    primaryAction: MoshiModalAction,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    secondaryAction: MoshiModalAction? = null,
    icon: (@Composable () -> Unit)? = null,
    primaryActionVariant: MoshiButtonVariant = MoshiButtonVariant.Primary,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        MoshiModalContent(
            title = title,
            primaryAction = primaryAction,
            modifier = modifier,
            supportingText = supportingText,
            secondaryAction = secondaryAction,
            icon = icon,
            primaryActionVariant = primaryActionVariant,
        )
    }
}

@Composable
private fun MoshiModalContent(
    title: String,
    primaryAction: MoshiModalAction,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    secondaryAction: MoshiModalAction? = null,
    icon: (@Composable () -> Unit)? = null,
    primaryActionVariant: MoshiButtonVariant = MoshiButtonVariant.Primary,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors

    Column(
        modifier = modifier
            .widthIn(max = 296.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.space3))
            .background(colors.background.elevated)
            .semantics { paneTitle = title }
            .verticalScroll(rememberScrollState())
            .padding(
                start = spacing.space6,
                end = spacing.space6,
                top = if (icon != null) spacing.space4 else spacing.space8,
                bottom = spacing.space6,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(spacing.space16 + spacing.space4),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(Modifier.height(spacing.space4))
        }
        MoshiText(
            text = title,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            role = MoshiTextRole.Subheading,
            color = colors.foreground.primary,
            textAlign = TextAlign.Center,
        )
        if (supportingText != null) {
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = supportingText,
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Body,
                color = colors.foreground.secondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(spacing.space8))
        ModalButton(action = primaryAction, variant = primaryActionVariant)
        if (secondaryAction != null) {
            Spacer(Modifier.height(spacing.space4))
            ModalButton(action = secondaryAction, variant = MoshiButtonVariant.Outline)
        }
    }
}

@Composable
private fun ModalButton(action: MoshiModalAction, variant: MoshiButtonVariant) {
    MoshiButton(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = variant,
        enabled = action.enabled,
        isLoading = action.isLoading,
    ) {
        MoshiText(text = action.label, textAlign = TextAlign.Center)
    }
}

@MoshiPreview
@Composable
private fun MoshiModalOneActionPreview() {
    ModalPreview(showIcon = false, showSecondaryAction = false)
}

@MoshiPreview
@Composable
private fun MoshiModalTwoActionsPreview() {
    ModalPreview(showIcon = false, showSecondaryAction = true)
}

@MoshiPreview
@Composable
private fun MoshiModalIconOneActionPreview() {
    ModalPreview(showIcon = true, showSecondaryAction = false)
}

@MoshiPreview
@Composable
private fun MoshiModalIconTwoActionsPreview() {
    ModalPreview(showIcon = true, showSecondaryAction = true)
}

@Composable
private fun ModalPreview(showIcon: Boolean, showSecondaryAction: Boolean) {
    MoshiTheme {
        Box(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space6),
        ) {
            MoshiModalContent(
                title = stringResource(R.string.moshi_modal_preview_title),
                supportingText = stringResource(R.string.moshi_modal_preview_supporting_text),
                primaryAction = MoshiModalAction(
                    label = stringResource(R.string.moshi_modal_preview_primary_action),
                    onClick = {},
                ),
                secondaryAction = if (showSecondaryAction) {
                    MoshiModalAction(
                        label = stringResource(R.string.moshi_modal_preview_secondary_action),
                        onClick = {},
                    )
                } else {
                    null
                },
                icon = if (showIcon) {
                    { ModalPreviewIcon() }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ModalPreviewIcon() {
    Box(
        modifier = Modifier
            .size(Moshi.spacing.space16 + Moshi.spacing.space4)
            .background(Moshi.colors.background.status.warning, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fluent_warning_24_regular),
            contentDescription = null,
            modifier = Modifier.size(Moshi.spacing.space10),
            tint = Moshi.colors.foreground.status.warning,
        )
    }
}
