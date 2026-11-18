package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiQuantityButton(
    quantity: String,
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val foreground = if (enabled) colors.foreground.primary else colors.foreground.disabled
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                else Modifier,
            )
            .semantics { contentDescription = label }
            .padding(horizontal = spacing.space2, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fluent_subtract_24_regular),
            contentDescription = null,
            modifier = Modifier.size(spacing.space4),
            tint = foreground,
        )
        Spacer(Modifier.width(spacing.space2))
        MoshiText(
            text = quantity,
            modifier = Modifier
                .weight(1f, fill = false)
                .sizeIn(minWidth = spacing.space12)
                .background(colors.background.subtle, shape)
                .padding(horizontal = spacing.space3, vertical = spacing.space1)
                .clearAndSetSemantics {},
            role = MoshiTextRole.Label14,
            color = foreground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(spacing.space2))
        Icon(
            painter = painterResource(R.drawable.ic_fluent_add_24_regular),
            contentDescription = null,
            modifier = Modifier.size(spacing.space4),
            tint = foreground,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiQuantityButtonPreview() {
    MoshiTheme {
        MoshiQuantityButton(
            quantity = "0",
            label = stringResource(R.string.moshi_quantity_button_preview_label),
            onClick = {},
        )
    }
}
