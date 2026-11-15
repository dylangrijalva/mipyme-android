package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

private val DRAWER_WIDTH = 310.dp

/**
 * A modal navigation drawer on the start edge that slides over [content].
 *
 * [header] and [footer] span the full sheet width, and [items] is inset and spaced for
 * [MoshiNavigationDrawerItem]. The header and items scroll together while the footer stays pinned
 * to the bottom. Control it through [drawerState]; [gesturesEnabled] allows opening and closing by swipe.
 */
@Composable
fun MoshiNavigationDrawer(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    header: @Composable ColumnScope.() -> Unit = {},
    items: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing

    ModalNavigationDrawer(
        drawerContent = {
            val scrollState = rememberScrollState()
            key(drawerState.isOpen) {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    modifier = Modifier.width(DRAWER_WIDTH),
                    drawerShape = RectangleShape,
                    drawerContainerColor = colors.background.canvas,
                    drawerContentColor = colors.foreground.primary,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState),
                        ) {
                            header()
                            Column(
                                modifier = Modifier
                                    .padding(spacing.space3)
                                    .selectableGroup(),
                                verticalArrangement = Arrangement.spacedBy(spacing.space4),
                                content = items,
                            )
                        }
                        footer?.invoke(this)
                    }
                }
            }
        },
        modifier = modifier.clipToBounds(),
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        scrimColor = colors.background.overlay,
        content = content,
    )
}

@MoshiPreview
@Composable
private fun MoshiNavigationDrawerPreview() {
    MoshiTheme {
        MoshiNavigationDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            header = {
                Column(modifier = Modifier.padding(Moshi.spacing.space4)) {
                    MoshiText(text = "Downtown store", role = MoshiTextRole.Heading)
                    MoshiText(
                        text = "ada@example.com",
                        role = MoshiTextRole.Body,
                        color = Moshi.colors.foreground.secondary,
                    )
                }
                MoshiDivider()
            },
            items = {
                MoshiNavigationDrawerItem(
                    label = "Checkout",
                    icon = R.drawable.ic_fluent_shopping_bag_24_regular,
                    selected = true,
                    onClick = {},
                )
                MoshiNavigationDrawerItem(
                    label = "Inventory",
                    icon = R.drawable.ic_fluent_text_bullet_list_ltr_24_regular,
                    selected = false,
                    onClick = {},
                )
            },
            footer = {
                MoshiDivider()
                Spacer(Modifier.height(Moshi.spacing.space4))
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Moshi.colors.background.canvas),
            )
        }
    }
}
