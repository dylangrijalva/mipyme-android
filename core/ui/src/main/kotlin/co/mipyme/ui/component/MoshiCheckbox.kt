package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val fill = when {
        !enabled && checked -> colors.selection.disabledFill
        !enabled -> colors.background.disabled
        checked -> colors.action.primary.background
        else -> colors.background.surface
    }
    val borderColor = when {
        !enabled && checked -> colors.selection.disabledFill
        !enabled -> colors.control.mutedBorder
        checked -> colors.action.primary.background
        else -> colors.control.border
    }
    val checkColor = colors.foreground.inverse

    Box(
        modifier = modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                indication = ripple(bounded = false, radius = spacing.space6),
                interactionSource = null,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.space5)
                .clip(CircleShape)
                .background(fill)
                .border(width = 2.dp, color = borderColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_checkmark_24_regular),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.space3),
                    tint = checkColor,
                )
            }
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiCheckboxPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.surface)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
        ) {
            MoshiCheckbox(checked = false, onCheckedChange = {})
            MoshiCheckbox(checked = true, onCheckedChange = {})
            MoshiCheckbox(checked = false, onCheckedChange = {}, enabled = false)
            MoshiCheckbox(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
