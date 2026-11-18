package co.mipyme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Moshi.colors.foreground.primary,
) {
    val spacing = Moshi.spacing
    val color = if (enabled) tint else Moshi.colors.foreground.disabled
    Box(
        modifier = modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = ripple(color = color),
                onClick = onClick,
            )
            .padding(spacing.space3),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(spacing.space6),
            tint = color,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiIconButtonPreview() {
    MoshiTheme {
        MoshiIconButton(
            icon = R.drawable.ic_fluent_dismiss_24_regular,
            label = stringResource(R.string.moshi_search_clear),
            onClick = {},
        )
    }
}
