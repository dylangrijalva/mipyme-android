package co.mipyme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

data class MoshiTextFieldTrailingAction(
    @param:DrawableRes val icon: Int,
    val contentDescription: String,
    val onClick: () -> Unit,
) {
    init {
        require(contentDescription.isNotBlank()) {
            "MoshiTextFieldTrailingAction contentDescription must not be blank."
        }
    }
}

@Composable
fun MoshiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    prefixText: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    onFocusChange: ((Boolean) -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentType: ContentType? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingAction: MoshiTextFieldTrailingAction? = null,
) {
    require(label.isNotBlank()) { "MoshiTextField label must not be blank." }
    require(placeholder == null || placeholder.isNotBlank()) {
        "MoshiTextField placeholder must not be blank when provided."
    }
    require(prefixText == null || prefixText.isNotBlank()) {
        "MoshiTextField prefixText must not be blank when provided."
    }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MoshiTextField supportingText must not be blank when provided."
    }

    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val trailingActionInteractionSource = remember { MutableInteractionSource() }
    val containerInteractionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val trailingActionFocused by trailingActionInteractionSource.collectIsFocusedAsState()

    LaunchedEffect(focused) {
        onFocusChange?.invoke(focused)
    }

    val isFloated = focused || value.isNotEmpty() || prefixText != null
    val labelProgress by animateFloatAsState(
        targetValue = if (isFloated) 1f else 0f,
        animationSpec = tween(
            durationMillis = FIELD_LABEL_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "MoshiTextFieldLabelPosition",
    )
    val labelScale = Moshi.typography.label12.fontSize.value / Moshi.typography.copy16.fontSize.value
    val contentColor = if (enabled) colors.gray1000 else colors.gray700
    val labelColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.gray700
            isError -> colors.red900
            isFloated -> colors.gray1000
            else -> colors.gray900
        },
        animationSpec = tween(durationMillis = FIELD_LABEL_DURATION_MILLIS),
        label = "MoshiTextFieldLabelColor",
    )
    val supportingColor = when {
        !enabled -> colors.gray700
        isError -> colors.red900
        else -> colors.gray900
    }
    val borderWidth = if (enabled && focused) 2.dp else 1.dp
    val borderColor = when {
        !enabled -> colors.gray400
        isError -> colors.red900
        focused -> colors.blue900
        else -> colors.gray400
    }
    val fieldShape = RoundedCornerShape(6.dp)
    val containerClickModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = containerInteractionSource,
            indication = null,
            onClick = focusRequester::requestFocus,
        )
    } else {
        Modifier
    }
    val trailingActionContent: (@Composable () -> Unit)? = trailingAction?.let { action ->
        {
            val focusRing = if (enabled && trailingActionFocused) {
                Modifier.border(2.dp, colors.blue900, CircleShape)
            } else {
                Modifier
            }

            IconButton(
                onClick = action.onClick,
                enabled = enabled,
                modifier = Modifier
                    .size(spacing.space12)
                    .then(focusRing),
                interactionSource = trailingActionInteractionSource,
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    contentDescription = action.contentDescription,
                    modifier = Modifier.size(spacing.space6),
                    tint = if (enabled) colors.gray1000 else colors.gray700,
                )
            }
        }
    }

    Column(modifier = modifier) {
        TextFieldLayout(
            labelProgress = labelProgress,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.space16)
                .background(
                    color = if (enabled) colors.background100 else colors.gray100,
                    shape = fieldShape,
                )
                .border(borderWidth, borderColor, fieldShape)
                .then(containerClickModifier),
            label = {
                MoshiText(
                    text = label,
                    role = MoshiTextRole.Copy16,
                    color = labelColor,
                    modifier = Modifier.graphicsLayer {
                        val scale = 1f + (labelScale - 1f) * labelProgress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
                    maxLines = 1,
                )
            },
            field = {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val sanitizedValue = newValue.replace("\n", "").replace("\r", "")
                        if (sanitizedValue != value) {
                            onValueChange(sanitizedValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics {
                            contentDescription = label
                            if (contentType != null) {
                                this.contentType = contentType
                            }
                            if (enabled && isError) {
                                error(supportingText.orEmpty())
                            }
                        },
                    enabled = enabled,
                    textStyle = Moshi.typography.copy16.copy(color = contentColor),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.gray1000),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (prefixText != null) {
                                MoshiText(
                                    text = prefixText,
                                    role = MoshiTextRole.Copy16,
                                    color = contentColor,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.width(spacing.space2))
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (focused && value.isEmpty() && placeholder != null) {
                                    MoshiText(
                                        text = placeholder,
                                        role = MoshiTextRole.Copy16,
                                        color = if (enabled) {
                                            colors.gray900
                                        } else {
                                            colors.gray700
                                        },
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            }
                        }
                    },
                )
            },
            trailingAction = trailingActionContent,
        )

        if (supportingText != null) {
            Spacer(Modifier.height(spacing.space1))
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(spacing.space4))
                MoshiText(
                    text = supportingText,
                    modifier = Modifier.weight(1f),
                    role = MoshiTextRole.Label12,
                    color = supportingColor,
                )
            }
        }
    }
}

@Composable
private fun TextFieldLayout(
    labelProgress: Float,
    modifier: Modifier,
    label: @Composable () -> Unit,
    field: @Composable () -> Unit,
    trailingAction: (@Composable () -> Unit)?,
) {
    val spacing = Moshi.spacing
    val horizontalPadding = spacing.space4
    val topPadding = spacing.space2
    val bottomPadding = spacing.space2
    val labelFieldGap = spacing.space1
    val fieldTop = spacing.space6
    val trailingActionGap = spacing.space2

    Layout(
        modifier = modifier,
        contents = listOfNotNull(label, field, trailingAction),
    ) { measurables, constraints ->
        val horizontalPaddingPx = horizontalPadding.roundToPx()
        val trailingActionPlaceable = measurables.getOrNull(2)
            ?.getOrNull(0)
            ?.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = (constraints.maxWidth - horizontalPaddingPx * 2).coerceAtLeast(0),
                ),
            )
        val trailingActionGapPx = if (trailingActionPlaceable != null) trailingActionGap.roundToPx() else 0
        val trailingActionWidth = trailingActionPlaceable?.width ?: 0
        val contentConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = (
                constraints.maxWidth -
                    horizontalPaddingPx * 2 -
                    trailingActionWidth -
                    trailingActionGapPx
            ).coerceAtLeast(0),
        )
        val labelPlaceable = measurables[0][0].measure(contentConstraints)
        val fieldPlaceable = measurables[1][0].measure(contentConstraints)
        val floatedFieldY = maxOf(
            fieldTop.roundToPx(),
            topPadding.roundToPx() + labelPlaceable.height + labelFieldGap.roundToPx(),
        )
        val contentHeight = floatedFieldY + fieldPlaceable.height + bottomPadding.roundToPx()
        val width = constraints.maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val restingLabelY = (height - labelPlaceable.height) / 2
        val labelY = restingLabelY + ((topPadding.roundToPx() - restingLabelY) * labelProgress).toInt()

        layout(width, height) {
            labelPlaceable.placeRelative(horizontalPaddingPx, labelY)
            fieldPlaceable.placeRelative(horizontalPaddingPx, floatedFieldY)
            trailingActionPlaceable?.placeRelative(
                x = width - horizontalPaddingPx - trailingActionPlaceable.width,
                y = (height - trailingActionPlaceable.height) / 2,
            )
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiTextFieldPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background100)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            MoshiTextField(
                value = "",
                onValueChange = {},
                label = "Customer name",
                placeholder = "Ada Lovelace",
                trailingAction = MoshiTextFieldTrailingAction(
                    icon = R.drawable.ic_fluent_more_vertical_24_regular,
                    contentDescription = "More customer options",
                    onClick = {},
                ),
            )
            MoshiTextField(
                value = "ada@example.com",
                onValueChange = {},
                label = "Email",
                supportingText = "Used for receipts and account recovery.",
            )
            MoshiTextField(
                value = "not-an-email",
                onValueChange = {},
                label = "Email",
                supportingText = "Enter a valid email address.",
                isError = true,
            )
            MoshiTextField(
                value = "15.00",
                onValueChange = {},
                label = "Price",
                prefixText = "USD",
            )
            MoshiTextField(
                value = "Unavailable",
                onValueChange = {},
                label = "Disabled field",
                enabled = false,
            )
        }
    }
}

private const val FIELD_LABEL_DURATION_MILLIS = 150
