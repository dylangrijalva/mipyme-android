package co.mipyme.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiAlertDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    confirmAction: MoshiModalAction,
    cancelAction: MoshiModalAction,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    confirmActionVariant: MoshiButtonVariant = MoshiButtonVariant.Primary,
    properties: DialogProperties = DialogProperties(),
) {
    MoshiModal(
        title = title,
        onDismissRequest = onDismissRequest,
        primaryAction = confirmAction,
        modifier = modifier,
        supportingText = message,
        secondaryAction = cancelAction,
        icon = icon,
        primaryActionVariant = confirmActionVariant,
        properties = properties,
    )
}

@MoshiPreview
@Composable
private fun MoshiAlertDialogPreview() {
    MoshiTheme {
        MoshiAlertDialog(
            title = stringResource(R.string.moshi_modal_preview_title),
            message = stringResource(R.string.moshi_modal_preview_supporting_text),
            onDismissRequest = {},
            confirmAction = MoshiModalAction(
                label = stringResource(R.string.moshi_modal_preview_primary_action),
                onClick = {},
            ),
            cancelAction = MoshiModalAction(
                label = stringResource(R.string.moshi_modal_preview_secondary_action),
                onClick = {},
            ),
        )
    }
}
