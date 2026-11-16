package co.mipyme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiListItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val contentColor = if (destructive) colors.red900 else colors.gray1000

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .sizeIn(minHeight = spacing.space12)
            .padding(horizontal = spacing.pageGutterNarrow, vertical = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                modifier = Modifier.size(spacing.space6),
                tint = contentColor,
            )
            Spacer(Modifier.width(spacing.space3))
        }
        MoshiText(
            text = label,
            modifier = Modifier.weight(1f),
            role = MoshiTextRole.Label16,
            color = contentColor,
        )
        if (trailing != null) {
            Spacer(Modifier.width(spacing.space3))
            trailing()
        }
    }
}

@Composable
fun MoshiListItemChevron(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_fluent_chevron_right_24_regular),
        contentDescription = null,
        modifier = modifier.size(Moshi.spacing.space4),
        tint = Moshi.colors.gray900,
    )
}

@MoshiPreview
@Composable
private fun MoshiListItemPreview() {
    MoshiTheme {
        Column(modifier = Modifier.background(Moshi.colors.background100)) {
            MoshiListItem(
                label = "Default item",
                onClick = {},
                trailing = { MoshiListItemChevron() },
            )
            MoshiListItem(
                label = "Item with a leading icon",
                onClick = {},
                leadingIcon = R.drawable.ic_fluent_shopping_bag_24_regular,
                trailing = { MoshiListItemChevron() },
            )
            MoshiListItem(
                label = "Destructive item",
                onClick = {},
                leadingIcon = R.drawable.ic_fluent_warning_24_regular,
                destructive = true,
            )
            MoshiListItem(
                label = "An item with a long label that wraps onto several lines at larger font sizes",
                onClick = {},
                leadingIcon = R.drawable.ic_fluent_add_24_regular,
                trailing = { MoshiListItemChevron() },
            )
        }
    }
}
