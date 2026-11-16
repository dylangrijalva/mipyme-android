package co.mipyme.ui.component

import androidx.annotation.DrawableRes
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
fun MoshiNavigationDrawerItem(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes selectedIcon: Int = icon,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(spacing.space3)
    val containerColor = if (selected) colors.gray300 else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = null,
                indication = ripple(),
                onClick = onClick,
            )
            .sizeIn(minHeight = spacing.space12)
            .padding(horizontal = spacing.space3, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) selectedIcon else icon),
            contentDescription = null,
            modifier = Modifier.size(spacing.space6),
            tint = colors.gray1000,
        )
        Spacer(Modifier.width(spacing.space3))
        MoshiText(
            text = label,
            modifier = Modifier.weight(1f),
            role = MoshiTextRole.Label16,
            color = colors.gray1000,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiNavigationDrawerItemPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.gray100)
                .padding(Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            MoshiNavigationDrawerItem(
                label = "Selected destination",
                icon = R.drawable.ic_fluent_shopping_bag_24_regular,
                selected = true,
                onClick = {},
            )
            MoshiNavigationDrawerItem(
                label = "Another destination",
                icon = R.drawable.ic_fluent_text_bullet_list_ltr_24_regular,
                selected = false,
                onClick = {},
            )
            MoshiNavigationDrawerItem(
                label = "A destination with a long label that wraps onto several lines at larger font sizes",
                icon = R.drawable.ic_fluent_add_24_regular,
                selected = false,
                onClick = {},
            )
        }
    }
}
