package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

private val RadioDotSize = 10.dp

@Composable
fun MoshiRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(12.dp)
    val containerColor = when {
        !enabled -> colors.background.disabled
        selected -> colors.background.selected
        else -> colors.background.surface
    }
    val borderColor = when {
        !enabled -> colors.border.default
        selected -> colors.border.interactive
        else -> colors.border.default
    }
    val labelColor = if (enabled) colors.foreground.primary else colors.foreground.disabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = spacing.space16)
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = null,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(spacing.space3))
        }
        MoshiText(
            text = label,
            modifier = Modifier.weight(1f),
            role = MoshiTextRole.Body,
            color = labelColor,
        )
        Spacer(Modifier.width(spacing.space3))
        RadioIndicator(selected = selected, enabled = enabled)
    }
}

@Composable
private fun RadioIndicator(selected: Boolean, enabled: Boolean) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val outline = when {
        !enabled -> colors.border.default
        selected -> colors.background.action.primary.default
        else -> colors.border.interactive
    }
    val dot = if (enabled) colors.background.action.primary.default else colors.foreground.disabled

    Box(
        modifier = Modifier
            .size(spacing.space5)
            .border(width = 2.dp, color = outline, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(RadioDotSize)
                    .background(color = dot, shape = CircleShape),
            )
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiRadioOptionPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiRadioOption(label = "Grocery store", selected = true, onClick = {})
            MoshiRadioOption(label = "Restaurant", selected = false, onClick = {})
            MoshiRadioOption(
                label = "Nicaragua",
                selected = false,
                onClick = {},
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(Moshi.spacing.space8)
                            .background(Moshi.colors.background.selected, CircleShape),
                    )
                },
            )
            MoshiRadioOption(label = "Bakery", selected = false, onClick = {}, enabled = false)
            MoshiRadioOption(label = "Other", selected = true, onClick = {}, enabled = false)
        }
    }
}
