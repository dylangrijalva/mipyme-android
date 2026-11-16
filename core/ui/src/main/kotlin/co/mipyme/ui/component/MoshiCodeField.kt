package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

@Composable
fun MoshiCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(label.isNotBlank()) { "MoshiCodeField label must not be blank." }
    require(length > 0) { "MoshiCodeField length must be positive." }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MoshiCodeField supportingText must not be blank when provided."
    }

    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val activeIndex = value.length.coerceAtMost(length - 1)
    val supportingColor = when {
        !enabled -> colors.gray700
        isError -> colors.red900
        else -> colors.gray900
    }
    val containerClickModifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            )
        }
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val sanitizedValue = newValue.filter(Char::isDigit).take(length)
                if (sanitizedValue != value) {
                    onValueChange(sanitizedValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = label
                    if (enabled && isError) {
                        error(supportingText.orEmpty())
                    }
                },
            enabled = enabled,
            textStyle = Moshi.typography.label20.copy(color = Color.Transparent),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth().then(containerClickModifier)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        repeat(length) { index ->
                            CodeCell(
                                digit = value.getOrNull(index),
                                isActive = focused && index == activeIndex,
                                enabled = enabled,
                                isError = isError,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Box(modifier = Modifier.size(spacing.space1).alpha(0f)) {
                        innerTextField()
                    }
                }
            },
        )

        if (supportingText != null) {
            Spacer(Modifier.height(spacing.space1))
            MoshiText(
                text = supportingText,
                modifier = Modifier.fillMaxWidth(),
                role = MoshiTextRole.Label12,
                color = supportingColor,
            )
        }
    }
}

@Composable
private fun CodeCell(
    digit: Char?,
    isActive: Boolean,
    enabled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Moshi.colors
    val shape = RoundedCornerShape(6.dp)
    val borderColor = when {
        !enabled -> colors.gray400
        isError -> colors.red900
        isActive -> colors.blue900
        else -> colors.gray500
    }
    val borderWidth = if (enabled && isActive) 2.dp else 1.dp

    Box(
        modifier = modifier
            .heightIn(min = Moshi.spacing.space16)
            .background(
                color = if (enabled) colors.background100 else colors.gray100,
                shape = shape,
            )
            .border(borderWidth, borderColor, shape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            MoshiText(
                text = digit.toString(),
                role = MoshiTextRole.Label20,
                color = if (enabled) colors.gray1000 else colors.gray700,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiCodeFieldPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background100)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            MoshiCodeField(value = "", onValueChange = {}, label = "Verification code")
            MoshiCodeField(value = "123", onValueChange = {}, label = "Verification code")
            MoshiCodeField(
                value = "123456",
                onValueChange = {},
                label = "Verification code",
                isError = true,
                supportingText = "The code is incorrect. Check it and try again.",
            )
            MoshiCodeField(
                value = "123",
                onValueChange = {},
                label = "Verification code",
                enabled = false,
            )
        }
    }
}
