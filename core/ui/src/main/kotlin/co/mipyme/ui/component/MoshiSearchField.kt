package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClear: () -> Unit = { onValueChange("") },
    clearLabel: String? = null,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(spacing.space6)
    val foreground = if (enabled) colors.foreground.primary else colors.foreground.disabled

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentDescription = label },
        enabled = enabled,
        singleLine = true,
        textStyle = Moshi.typography.copy16.copy(color = foreground),
        cursorBrush = SolidColor(colors.foreground.primary),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = spacing.space12)
                    .background(colors.background.subtle, shape)
                    .then(
                        if (focused && enabled) Modifier.border(2.dp, colors.border.focus, shape) else Modifier,
                    )
                    .padding(start = spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_search_24_regular),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.space6),
                    tint = if (enabled) colors.foreground.secondary else colors.foreground.disabled,
                )
                Spacer(Modifier.width(spacing.space2))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = spacing.space2),
                ) {
                    if (value.isEmpty()) {
                        MoshiText(
                            text = label,
                            color = if (enabled) colors.foreground.secondary else colors.foreground.disabled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    MoshiIconButton(
                        icon = R.drawable.ic_fluent_dismiss_24_regular,
                        label = clearLabel ?: stringResource(R.string.moshi_search_clear),
                        onClick = onClear,
                        enabled = enabled,
                    )
                } else {
                    Spacer(Modifier.width(spacing.space4))
                }
            }
        },
    )
}

@MoshiPreview
@Composable
private fun MoshiSearchFieldPreview() {
    MoshiTheme {
        MoshiSearchField(
            value = "",
            onValueChange = {},
            label = stringResource(R.string.moshi_search_preview_label),
            modifier = Modifier.padding(Moshi.spacing.space4),
        )
    }
}
