package co.mipyme.screen.product_editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextField
import co.mipyme.ui.component.MoshiTextFieldTrailingAction
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation

@Composable
internal fun ProductEditorToolbar(
    title: String,
    onBack: () -> Unit,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    actionLoading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoshiToolbar(
            modifier = Modifier.weight(1f),
            title = title,
            navigation = MoshiToolbarNavigation.Back(onClick = onBack),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        MoshiButton(onClick = onAction, enabled = actionEnabled, isLoading = actionLoading) {
            MoshiText(actionLabel)
        }
        Spacer(Modifier.width(Moshi.spacing.pageGutterNarrow))
    }
}

@Composable
internal fun ProductEditorDiscardDialog(
    @StringRes message: Int,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    MoshiAlertDialog(
        title = stringResource(R.string.product_editor_discard_changes),
        message = stringResource(message),
        onDismissRequest = onDismiss,
        confirmAction = MoshiModalAction(stringResource(R.string.product_editor_discard), onDiscard),
        cancelAction = MoshiModalAction(stringResource(R.string.product_editor_keep_editing), onDismiss),
        confirmActionVariant = MoshiButtonVariant.Destructive,
    )
}

@Composable
internal fun ProductEditorDecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    enabled: Boolean,
    @StringRes error: Int? = null,
    @StringRes placeholder: Int? = null,
    @StringRes supportingText: Int? = null,
    prefixText: String? = null,
    trailingAction: MoshiTextFieldTrailingAction? = null,
) {
    MoshiTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(label),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = placeholder?.let { stringResource(it) },
        prefixText = prefixText,
        trailingAction = trailingAction,
        supportingText = (error ?: supportingText)?.let { stringResource(it) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
    )
}

@StringRes
internal fun ProductEditorFieldError.priceMessage(): Int {
    return if (this == ProductEditorFieldError.BELOW_COST) {
        R.string.product_editor_price_below_cost
    } else {
        R.string.product_editor_invalid_price
    }
}
