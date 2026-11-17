package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiSelectableItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val containerColor = if (selected) colors.background.selected else Color.Transparent
    val labelColor = if (enabled) colors.foreground.primary else colors.foreground.disabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.space3))
            .background(containerColor)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = null,
                indication = ripple(),
                onClick = onClick,
            )
            .sizeIn(minHeight = spacing.space12)
            .padding(horizontal = spacing.space4, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MoshiText(
                text = label,
                role = MoshiTextRole.Label16,
                color = labelColor,
            )
            if (supportingText != null) {
                MoshiText(
                    text = supportingText,
                    role = MoshiTextRole.Copy14,
                    color = if (enabled) colors.foreground.secondary else colors.foreground.disabled,
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(spacing.space3))
            Icon(
                painter = painterResource(R.drawable.ic_fluent_checkmark_24_regular),
                contentDescription = null,
                modifier = Modifier.size(spacing.space6),
                tint = colors.foreground.primary,
            )
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiSelectableItemPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.surface)
                .padding(Moshi.spacing.space4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space1),
        ) {
            MoshiSelectableItem(
                label = "System",
                supportingText = "Match your device's appearance",
                selected = true,
                onClick = {},
            )
            MoshiSelectableItem(label = "Light", selected = false, onClick = {})
            MoshiSelectableItem(
                label = "Dark",
                supportingText = "Always use the dark appearance",
                selected = false,
                onClick = {},
            )
            MoshiSelectableItem(label = "Disabled", selected = false, onClick = {}, enabled = false)
            MoshiSelectableItem(
                label = "An option with a long label that wraps onto several lines at larger font sizes",
                supportingText = "A long supporting text that also wraps onto several lines at larger font sizes",
                selected = true,
                onClick = {},
            )
        }
    }
}
