package co.mipyme.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

/** The navigation action shown before the toolbar title. */
@Immutable
sealed interface MoshiToolbarNavigation {
    val onClick: () -> Unit

    /**
     * Returns to the previous screen. Pass [icon] to replace the default back arrow, and
     * [contentDescription] to replace the default "Back" label so it matches that icon.
     */
    class Back(
        @DrawableRes val icon: Int = R.drawable.ic_fluent_arrow_left_24_regular,
        val contentDescription: String? = null,
        override val onClick: () -> Unit,
    ) : MoshiToolbarNavigation

    /** Opens the app menu. */
    class Menu(override val onClick: () -> Unit) : MoshiToolbarNavigation
}

/** An action shown at the end of the toolbar, either as an icon button or inside the overflow menu. */
@Immutable
class MoshiToolbarAction(
    @DrawableRes val icon: Int,
    val label: String,
    val onClick: () -> Unit,
)

private const val MAX_VISIBLE_ACTIONS = 2

/**
 * A top toolbar with an optional navigation action, title, subtitle, and end actions.
 *
 * Up to [MAX_VISIBLE_ACTIONS] actions show as icon buttons and more collapse into an overflow menu.
 * Set [compact] to always place [actions] in the overflow menu, even when there is only one.
 */
@Composable
fun MoshiToolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    navigation: MoshiToolbarNavigation? = null,
    actions: List<MoshiToolbarAction> = emptyList(),
    compact: Boolean = false,
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background.surface)
            .windowInsetsPadding(windowInsets)
            .sizeIn(minHeight = spacing.space16)
            .padding(horizontal = spacing.space1, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigation != null) {
            ToolbarNavigationButton(navigation = navigation)
            Spacer(Modifier.width(spacing.space1))
        } else {
            Spacer(Modifier.width(spacing.space3))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing.space3),
            verticalArrangement = Arrangement.Center,
        ) {
            if (title != null) {
                MoshiText(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    role = MoshiTextRole.Heading20,
                    color = colors.foreground.primary,
                )
            }
            if (subtitle != null) {
                MoshiText(
                    text = subtitle,
                    role = MoshiTextRole.Copy16,
                    color = colors.foreground.secondary,
                )
            }
        }
        if (!compact && actions.size <= MAX_VISIBLE_ACTIONS) {
            actions.forEach { action ->
                ToolbarIconButton(
                    iconRes = action.icon,
                    contentDescription = action.label,
                    onClick = action.onClick,
                )
            }
        } else if (actions.isNotEmpty()) {
            ToolbarOverflowMenu(actions = actions)
        }
    }
}

@Composable
private fun ToolbarNavigationButton(navigation: MoshiToolbarNavigation) {
    when (navigation) {
        is MoshiToolbarNavigation.Back -> {
            ToolbarIconButton(
                iconRes = navigation.icon,
                contentDescription = navigation.contentDescription
                    ?: stringResource(R.string.moshi_toolbar_back),
                onClick = navigation.onClick,
            )
        }
        is MoshiToolbarNavigation.Menu -> {
            ToolbarIconButton(
                iconRes = R.drawable.ic_fluent_navigation_24_regular,
                contentDescription = stringResource(R.string.moshi_toolbar_menu),
                onClick = navigation.onClick,
            )
        }
    }
}

@Composable
private fun ToolbarOverflowMenu(actions: List<MoshiToolbarAction>) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val background = colors.background.elevated
    var expanded by remember { mutableStateOf(false) }

    Box {
        ToolbarIconButton(
            iconRes = R.drawable.ic_fluent_more_vertical_24_regular,
            contentDescription = stringResource(R.string.moshi_toolbar_more_actions),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(spacing.space2),
            containerColor = background,
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        MoshiText(
                            text = action.label,
                            role = MoshiTextRole.Copy16,
                            color = colors.foreground.primary,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(action.icon),
                            contentDescription = null,
                            modifier = Modifier.size(spacing.space6),
                            tint = colors.foreground.primary,
                        )
                    },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val spacing = Moshi.spacing

    Box(
        modifier = Modifier
            .size(spacing.space12)
            .clickable(
                role = Role.Button,
                interactionSource = null,
                indication = ripple(
                    bounded = false,
                    radius = spacing.space6,
                    color = Moshi.colors.foreground.primary,
                ),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(spacing.space6),
            tint = Moshi.colors.foreground.primary,
        )
    }
}

@MoshiPreview
@Composable
private fun MoshiToolbarPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Moshi.colors.background.surface)
                .padding(vertical = Moshi.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
        ) {
            MoshiToolbar(
                title = "Location name",
                navigation = MoshiToolbarNavigation.Menu(onClick = {}),
            )
            MoshiToolbar(title = "Location name")
            MoshiToolbar(
                title = "Page name",
                navigation = MoshiToolbarNavigation.Back(onClick = {}),
            )
            MoshiToolbar(
                title = "Page name",
                subtitle = "Location name here lorem ipsum",
                navigation = MoshiToolbarNavigation.Back(onClick = {}),
            )
            MoshiToolbar(navigation = MoshiToolbarNavigation.Back(onClick = {}))
            MoshiToolbar(
                title = "Page name",
                navigation = MoshiToolbarNavigation.Back(
                    icon = R.drawable.ic_fluent_checkmark_24_regular,
                    contentDescription = "Done",
                    onClick = {},
                ),
                actions = listOf(
                    MoshiToolbarAction(icon = R.drawable.ic_fluent_add_24_regular, label = "Add", onClick = {}),
                    MoshiToolbarAction(
                        icon = R.drawable.ic_fluent_shopping_bag_24_regular,
                        label = "Cart",
                        onClick = {},
                    ),
                ),
            )
            MoshiToolbar(
                title = "Page name",
                navigation = MoshiToolbarNavigation.Menu(onClick = {}),
                actions = listOf(
                    MoshiToolbarAction(icon = R.drawable.ic_fluent_add_24_regular, label = "Add", onClick = {}),
                    MoshiToolbarAction(
                        icon = R.drawable.ic_fluent_shopping_bag_24_regular,
                        label = "Cart",
                        onClick = {},
                    ),
                    MoshiToolbarAction(
                        icon = R.drawable.ic_fluent_warning_24_regular,
                        label = "Report",
                        onClick = {},
                    ),
                ),
            )
        }
    }
}
