package co.mipyme.screen.home

import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.homeScreenModule(): Module {
    viewModelOf(::HomeViewModel)
    navigation<Route.Home>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
    ) { HomeScreen() }
    return this
}

@Composable
internal fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    HomeContent(
        state = state,
        onSignOut = viewModel::onSignOut,
    )
}

@Composable
private fun HomeContent(
    state: HomeScreenUiState,
    onSignOut: () -> Unit,
) {
    val spacing = Moshi.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas)
            .padding(horizontal = spacing.pageGutterNarrow),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MoshiText(text = stringResource(R.string.home_title), role = MoshiTextRole.Heading)
            val email = state.email
            if (email != null) {
                Spacer(Modifier.height(spacing.space2))
                MoshiText(
                    text = email,
                    role = MoshiTextRole.Body,
                    color = Moshi.colors.foreground.secondary,
                )
            }
            Spacer(Modifier.height(spacing.space6))
            MoshiButton(
                onClick = onSignOut,
                variant = MoshiButtonVariant.Destructive,
                isLoading = state.isSigningOut,
            ) {
                MoshiText(text = stringResource(R.string.home_sign_out))
            }
        }
    }
}

@ScreenPreview
@Composable
fun HomeScreenPreview() {
    MoshiTheme {
        HomeContent(state = HomeScreenUiState(email = "ada@example.com"), onSignOut = {})
    }
}

@ScreenPreview
@Composable
fun HomeScreenSigningOutPreview() {
    MoshiTheme {
        HomeContent(state = HomeScreenUiState(isSigningOut = true), onSignOut = {})
    }
}
