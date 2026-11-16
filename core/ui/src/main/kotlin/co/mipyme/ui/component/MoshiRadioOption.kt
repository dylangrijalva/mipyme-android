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
        !enabled -> colors.gray100
        selected -> colors.gray200
        else -> colors.background100
    }
    val labelColor = if (enabled) colors.gray1000 else colors.gray700

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = spacing.space16)
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = colors.gray400, shape = shape)
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
            role = MoshiTextRole.Label16,
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
        !enabled -> colors.gray400
        selected -> colors.blue900
        else -> colors.gray500
    }
    val dot = if (enabled) colors.blue900 else colors.gray900

    Box(
        modifier = Modifier
            .size(spacing.space5)
            .border(width = 2.dp, color = outline, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
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
                .background(Moshi.colors.background100)
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
                            .background(Moshi.colors.gray100, CircleShape),
                    )
                },
            )
            MoshiRadioOption(label = "Bakery", selected = false, onClick = {}, enabled = false)
            MoshiRadioOption(label = "Other", selected = true, onClick = {}, enabled = false)
        }
    }
}
