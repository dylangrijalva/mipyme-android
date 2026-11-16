package co.mipyme.screen.initial_sync

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiLinearProgressIndicator
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarAction
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

private const val LOGO_DRAW_MILLIS = 1300

private val LeftArchClip: List<PathNode> =
    addPathNodes("M5,20H38A33,33 0 0 1 71,53V100H49V53A11,11 0 0 0 27,53V100H5Z")
private val LeftArchPath: List<PathNode> = addPathNodes("M16,100V31H38A22,22 0 0 1 60,53V100")
private val RightArchClip: List<PathNode> =
    addPathNodes("M49,100V53A33,33 0 0 1 115,53V100H93V53A11,11 0 0 0 71,53V100Z")
private val RightArchPath: List<PathNode> = addPathNodes("M60,100V53A22,22 0 0 1 104,53V100")
private val PillarClip: List<PathNode> =
    addPathNodes("M60,28.4A33,33 0 0 1 71,53V100H49V53A33,33 0 0 1 60,28.4Z")
private val PillarPath: List<PathNode> = addPathNodes("M60,100V25")

fun Module.initialSyncScreenModule(): Module {
    viewModelOf(::InitialSyncViewModel)
    navigation<Route.InitialSync>(
        metadata = NavDisplay.transitionSpec {
            fadeIn() togetherWith fadeOut()
        },
    ) { InitialSyncScreen() }
    return this
}

@Composable
internal fun InitialSyncScreen(viewModel: InitialSyncViewModel = koinViewModel()) {
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

    InitialSyncContent(
        state = state,
        onRetry = viewModel::onRetry,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirmed = viewModel::onSignOutConfirmed,
        onSignOutDismissed = viewModel::onSignOutDismissed,
    )
}

@Composable
private fun InitialSyncContent(
    state: InitialSyncScreenUiState,
    onRetry: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background100),
    ) {
        InitialSyncLogo(modifier = Modifier.align(Alignment.Center).size(144.dp))

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    top = maxHeight / 2 + 24.dp + spacing.space6,
                    start = spacing.pageGutterNarrow,
                    end = spacing.pageGutterNarrow,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val errorRes = state.errorRes
            if (errorRes == null) {
                MoshiText(
                    text = stringResource(R.string.initial_sync_loading),
                    role = MoshiTextRole.Copy16,
                    color = Moshi.colors.gray900,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(spacing.space4))
                MoshiLinearProgressIndicator(
                    modifier = Modifier.padding(horizontal = spacing.space8),
                )
            } else {
                MoshiText(
                    text = stringResource(errorRes),
                    modifier = Modifier.fillMaxWidth(),
                    role = MoshiTextRole.Copy16,
                    color = Moshi.colors.gray900,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(spacing.space6))
                MoshiButton(
                    onClick = onRetry,
                    variant = MoshiButtonVariant.Text,
                    enabled = !state.isSyncing && !state.isSigningOut,
                ) {
                    MoshiText(text = stringResource(R.string.initial_sync_retry))
                }
            }
        }

        MoshiToolbar(
            modifier = Modifier.align(Alignment.TopCenter),
            actions = listOf(
                MoshiToolbarAction(
                    icon = R.drawable.ic_fluent_sign_out_24_regular,
                    label = stringResource(R.string.initial_sync_sign_out),
                    onClick = onSignOutClick,
                ),
            ),
            compact = true,
        )
    }

    if (state.isSignOutConfirmationVisible) {
        SignOutConfirmationDialog(
            onConfirm = onSignOutConfirmed,
            onDismiss = onSignOutDismissed,
        )
    }
}

@Composable
private fun SignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoshiAlertDialog(
        title = stringResource(R.string.initial_sync_sign_out_confirm_title),
        message = stringResource(R.string.initial_sync_sign_out_confirm_message),
        onDismissRequest = onDismiss,
        confirmAction = MoshiModalAction(
            label = stringResource(R.string.initial_sync_sign_out_confirm_action),
            onClick = onConfirm,
        ),
        confirmActionVariant = MoshiButtonVariant.Destructive,
        cancelAction = MoshiModalAction(
            label = stringResource(R.string.initial_sync_sign_out_confirm_cancel),
            onClick = onDismiss,
        ),
    )
}

@Composable
private fun InitialSyncLogo(modifier: Modifier) {
    val isInspecting = LocalInspectionMode.current
    val progress = remember { Animatable(if (isInspecting) 1f else 0f) }
    val leftArchColor = colorResource(R.color.splash_logo_left_arch)
    val rightArchColor = colorResource(R.color.logo_orange)
    val pillarColor = colorResource(R.color.splash_logo_pillar)

    if (!isInspecting) {
        LaunchedEffect(Unit) {
            while (true) {
                progress.animateTo(1f, tween(LOGO_DRAW_MILLIS, easing = LinearEasing))
                delay(400L)
                progress.animateTo(0f, tween(LOGO_DRAW_MILLIS, easing = LinearEasing))
                delay(300L)
            }
        }
    }

    val painter = rememberVectorPainter(
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
        autoMirror = false,
    ) { _, _ ->
        Group(scaleX = 0.42f, scaleY = 0.42f, translationX = 28.8f, translationY = 28.8f) {
            Group(clipPathData = LeftArchClip) {
                Path(
                    pathData = LeftArchPath,
                    stroke = SolidColor(leftArchColor),
                    strokeLineWidth = 30f,
                    strokeLineJoin = StrokeJoin.Miter,
                    trimPathEnd = segment(
                        progress.value,
                        0,
                        550,
                        FastOutLinearInEasing,
                    ),
                )
            }
            Group(clipPathData = RightArchClip) {
                Path(
                    pathData = RightArchPath,
                    stroke = SolidColor(rightArchColor),
                    strokeLineWidth = 24f,
                    trimPathEnd = segment(
                        progress.value,
                        550,
                        520,
                        LinearOutSlowInEasing,
                    ),
                )
            }
            Group(clipPathData = PillarClip) {
                Path(
                    pathData = PillarPath,
                    stroke = SolidColor(pillarColor),
                    strokeLineWidth = 24f,
                    trimPathEnd = segment(
                        progress.value,
                        1040,
                        260,
                        FastOutSlowInEasing,
                    ),
                )
            }
        }
    }

    Image(painter = painter, contentDescription = null, modifier = modifier)
}

private fun segment(progress: Float, startMillis: Int, durationMillis: Int, easing: Easing): Float {
    val elapsed = progress * LOGO_DRAW_MILLIS - startMillis
    return easing.transform((elapsed / durationMillis).coerceIn(0f, 1f))
}

@ScreenPreview
@Composable
fun InitialSyncScreenPreview() {
    MoshiTheme {
        InitialSyncContent(
            state = InitialSyncScreenUiState(),
            onRetry = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun InitialSyncScreenFailedPreview() {
    MoshiTheme {
        InitialSyncContent(
            state = InitialSyncScreenUiState(
                isSyncing = false,
                errorRes = R.string.initial_sync_error_network,
            ),
            onRetry = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun InitialSyncScreenSignOutConfirmationPreview() {
    MoshiTheme {
        InitialSyncContent(
            state = InitialSyncScreenUiState(isSignOutConfirmationVisible = true),
            onRetry = {},
            onSignOutClick = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
        )
    }
}
