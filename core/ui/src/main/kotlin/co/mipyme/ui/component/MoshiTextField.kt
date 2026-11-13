package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

@Composable
fun MoshiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        !enabled -> colors.border.default
        isError -> colors.border.status.error
        focused -> colors.border.focus
        else -> colors.border.interactive
    }
    val borderWidth = if (focused && enabled) 2.dp else 1.dp
    val textColor = if (enabled) colors.foreground.primary else colors.foreground.disabled
    val textStyle = Moshi.typography.body.copy(color = textColor)
    val fieldSemantics = Modifier.semantics {
        if (isError && supportingText != null) {
            error(supportingText)
        }
    }

    // The label and supporting text live in the decoration box, so they merge into the field's own
    // semantics node and are announced with the typed text, as Material's text fields do.
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(fieldSemantics),
        enabled = enabled,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.foreground.primary),
        decorationBox = { innerTextField ->
            Column {
                if (label != null) {
                    MoshiText(
                        text = label,
                        role = MoshiTextRole.Small,
                        color = if (enabled) colors.foreground.primary else colors.foreground.disabled,
                    )
                    Spacer(Modifier.height(spacing.space1))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = spacing.space12)
                        .background(
                            color = if (enabled) colors.background.surface else colors.background.disabled,
                            shape = shape,
                        )
                        .border(width = borderWidth, color = borderColor, shape = shape)
                        .padding(horizontal = spacing.space4, vertical = spacing.space3),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        MoshiText(
                            text = placeholder,
                            color = if (enabled) colors.foreground.tertiary else colors.foreground.disabled,
                        )
                    }
                    innerTextField()
                }
                if (supportingText != null) {
                    Spacer(Modifier.height(spacing.space1))
                    MoshiText(
                        text = supportingText,
                        role = MoshiTextRole.Caption,
                        color = when {
                            !enabled -> colors.foreground.disabled
                            isError -> colors.foreground.status.error
                            else -> colors.foreground.secondary
                        },
                    )
                }
            }
        },
    )
}

@MoshiPreview
@Composable
private fun MoshiTextFieldPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            MoshiTextField(value = "", onValueChange = {}, label = "Your name", placeholder = "Ana Pérez")
            MoshiTextField(value = "Ana Pérez", onValueChange = {}, label = "Your name")
            MoshiTextField(
                value = "Ana",
                onValueChange = {},
                label = "Your name",
                supportingText = "Use up to 64 characters.",
            )
            MoshiTextField(
                value = "",
                onValueChange = {},
                label = "Your name",
                supportingText = "Enter your name.",
                isError = true,
            )
            MoshiTextField(value = "Ana Pérez", onValueChange = {}, label = "Your name", enabled = false)
        }
    }
}
