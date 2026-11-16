package co.mipyme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(spacing.space2)
    val selectedColor = colors.blue600
    val labelColor = when {
        !enabled -> colors.gray800
        selected -> colors.blue900
        else -> colors.gray1000
    }
    val supportingColor = when {
        !enabled -> colors.gray800
        selected -> colors.blue900
        else -> colors.gray900
    }
    val backgroundColor = when {
        !enabled -> colors.gray100
        selected -> colors.blue100
        else -> colors.background100
    }
    val borderColor = if (enabled && selected) colors.blue600 else colors.gray400

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = spacing.space16)
            .clip(shape)
            .background(backgroundColor)
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
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.space6),
                    tint = labelColor,
                )
                Spacer(Modifier.height(spacing.space2))
            }
            MoshiText(text = label, role = MoshiTextRole.Label16, color = labelColor)
            if (supportingText != null) {
                MoshiText(text = supportingText, role = MoshiTextRole.Copy14, color = supportingColor)
            }
        }
        Spacer(Modifier.width(spacing.space4))
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.size(spacing.space10),
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedColor,
                unselectedColor = colors.gray500,
                disabledSelectedColor = colors.gray700,
                disabledUnselectedColor = colors.gray400,
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
                .background(Moshi.colors.background100)
                .padding(Moshi.spacing.space4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            val label = stringResource(R.string.moshi_card_selector_preview_label)
            val supportingText = stringResource(R.string.moshi_card_selector_preview_supporting_text)
            MoshiCardSelector(label = label, supportingText = supportingText, selected = true, onClick = {})
            MoshiCardSelector(label = label, supportingText = supportingText, selected = false, onClick = {})
            MoshiCardSelector(
                label = label,
                selected = true,
                onClick = {},
                icon = R.drawable.ic_fluent_shopping_bag_24_regular,
            )
            MoshiCardSelector(label = label, selected = true, onClick = {})
            MoshiCardSelector(label = label, selected = false, onClick = {})
            MoshiCardSelector(label = label, selected = true, onClick = {}, enabled = false)
            MoshiCardSelector(label = label, selected = false, onClick = {}, enabled = false)
        }
    }
}
