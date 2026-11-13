package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiCardSelector(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(spacing.space2)
    val borderColor = if (enabled && selected) colors.foreground.primary else colors.border.default
    val labelColor = if (enabled) colors.foreground.primary else colors.foreground.disabled
    val supportingColor = if (enabled) colors.foreground.secondary else colors.foreground.disabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = spacing.space16)
            .clip(shape)
            .background(if (enabled) colors.background.surface else colors.background.disabled)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = null,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(start = spacing.space4, end = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.space3),
        ) {
            MoshiText(text = label, role = MoshiTextRole.Body, color = labelColor)
            if (supportingText != null) {
                MoshiText(text = supportingText, role = MoshiTextRole.Small, color = supportingColor)
            }
        }
        Spacer(Modifier.width(spacing.space4))
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.size(spacing.space10),
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.foreground.primary,
                unselectedColor = colors.border.strong,
                disabledSelectedColor = colors.foreground.disabled,
                disabledUnselectedColor = colors.border.default,
            ),
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiCardSelectorPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.canvas)
                .padding(Moshi.spacing.space4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            val label = stringResource(R.string.moshi_card_selector_preview_label)
            val supportingText = stringResource(R.string.moshi_card_selector_preview_supporting_text)
            MoshiCardSelector(label = label, supportingText = supportingText, selected = true, onClick = {})
            MoshiCardSelector(label = label, supportingText = supportingText, selected = false, onClick = {})
            MoshiCardSelector(label = label, selected = true, onClick = {})
            MoshiCardSelector(label = label, selected = false, onClick = {})
            MoshiCardSelector(label = label, selected = true, onClick = {}, enabled = false)
            MoshiCardSelector(label = label, selected = false, onClick = {}, enabled = false)
        }
    }
}
