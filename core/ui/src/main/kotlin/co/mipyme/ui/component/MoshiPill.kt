package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun MoshiPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    count: String? = null,
    role: Role = Role.Button,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(percent = 50)
    val background = when {
        !enabled -> colors.gray100
        selected -> colors.gray1000
        else -> colors.background100
    }
    val foreground = when {
        !enabled -> colors.gray700
        selected -> colors.background100
        else -> colors.gray1000
    }
    Row(
        modifier = modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected && enabled) background else colors.gray400,
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = role,
                interactionSource = null,
                indication = ripple(color = foreground),
                onClick = onClick,
            )
            .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoshiText(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            role = MoshiTextRole.Label14,
            color = foreground,
        )
        if (count != null) {
            Spacer(Modifier.width(spacing.space2))
            MoshiText(text = count, role = MoshiTextRole.Label14, color = foreground)
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiPillPreview() {
    MoshiTheme {
        Column {
            MoshiPill(
                label = stringResource(R.string.moshi_pill_preview_label),
                selected = true,
                onClick = {},
            )
            Spacer(Modifier.height(Moshi.spacing.space2))
            MoshiPill(
                label = stringResource(R.string.moshi_pill_preview_label),
                selected = false,
                onClick = {},
                enabled = false,
            )
        }
    }
}
