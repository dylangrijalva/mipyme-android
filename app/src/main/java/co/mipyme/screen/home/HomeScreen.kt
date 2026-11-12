package co.mipyme.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.mipyme.R
import co.mipyme.navigation.Route
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import org.koin.core.module.Module
import org.koin.dsl.navigation3.navigation

fun Module.homeScreenModule(): Module {
    navigation<Route.Home> { HomeScreen() }
    return this
}

@Composable
internal fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MoshiText(text = stringResource(R.string.home_title), role = MoshiTextRole.Heading)
    }
}
