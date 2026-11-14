package co.mipyme.screen.welcome

import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiAlertDialog
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiCheckbox
import co.mipyme.ui.component.MoshiPageIndicator
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

private const val ILLUSTRATION_ASPECT_RATIO = 4f / 3f

private val welcomeSlides = listOf(
    WelcomeSlide(
        imageRes = R.drawable.illustration_welcome_sell,
        titleRes = R.string.welcome_slide_sell_title,
        bodyRes = R.string.welcome_slide_sell_body,
    ),
    WelcomeSlide(
        imageRes = R.drawable.illustration_welcome_stock,
        titleRes = R.string.welcome_slide_stock_title,
        bodyRes = R.string.welcome_slide_stock_body,
    ),
    WelcomeSlide(
        imageRes = R.drawable.illustration_welcome_offline,
        titleRes = R.string.welcome_slide_offline_title,
        bodyRes = R.string.welcome_slide_offline_body,
    ),
    WelcomeSlide(
        imageRes = R.drawable.illustration_welcome_trial,
        titleRes = R.string.welcome_slide_trial_title,
        bodyRes = R.string.welcome_slide_trial_body,
    ),
)

fun Module.welcomeScreenModule(): Module {
    viewModelOf(::WelcomeViewModel)
    navigation<Route.Welcome>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
    ) { WelcomeScreen() }
    return this
}

@Composable
internal fun WelcomeScreen(viewModel: WelcomeViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backStack = LocalNavBackStack.current
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    WelcomeContent(
        state = state,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onGoogleSignIn = viewModel::onGoogleSignIn,
        onEmailSignIn = {
            viewModel.onEmailSignInRecoveryDismissed()
            if (backStack.lastOrNull() != Route.SignInWithEmail) {
                backStack.add(Route.SignInWithEmail)
            }
        },
        onTermsClick = viewModel::onTermsClick,
        onPrivacyClick = viewModel::onPrivacyClick,
        onEmailSignInRecoveryDismissed = viewModel::onEmailSignInRecoveryDismissed,
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeScreenUiState,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onEmailSignInRecoveryDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val pagerState = rememberPagerState(pageCount = { welcomeSlides.size })

    if (state.showEmailSignInRecovery) {
        MoshiAlertDialog(
            title = stringResource(R.string.welcome_google_sign_in_failed_title),
            message = stringResource(R.string.welcome_google_sign_in_failed_message),
            onDismissRequest = onEmailSignInRecoveryDismissed,
            confirmAction = MoshiModalAction(
                label = stringResource(R.string.welcome_continue_with_email),
                onClick = onEmailSignIn,
            ),
            cancelAction = MoshiModalAction(
                label = stringResource(R.string.welcome_google_sign_in_recovery_dismiss),
                onClick = onEmailSignInRecoveryDismissed,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas)
            .safeDrawingPadding()
            .padding(horizontal = spacing.pageGutterNarrow),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_lockup),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = spacing.space6)
                .height(56.dp),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            WelcomeSlidePage(slide = welcomeSlides[page])
        }

        MoshiPageIndicator(
            pageCount = welcomeSlides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = spacing.space6),
        )

        TermsRow(
            accepted = state.termsAccepted,
            onAcceptedChange = onTermsAcceptedChange,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
        )

        Column(
            modifier = Modifier.padding(top = spacing.space4, bottom = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            MoshiButton(
                onClick = onEmailSignIn,
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Primary,
                enabled = state.termsAccepted && !state.isSigningIn,
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_mail_24_regular),
                        contentDescription = null,
                        modifier = Modifier.size(spacing.space5),
                    )
                },
            ) {
                MoshiText(text = stringResource(R.string.welcome_continue_with_email))
            }
            MoshiButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Outline,
                enabled = state.termsAccepted,
                isLoading = state.isSigningIn,
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(spacing.space5),
                        tint = if (state.termsAccepted) Color.Unspecified else LocalContentColor.current,
                    )
                },
            ) {
                MoshiText(text = stringResource(R.string.welcome_continue_with_google))
            }
        }
    }
}

@Composable
private fun WelcomeSlidePage(slide: WelcomeSlide) {
    val spacing = Moshi.spacing

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(slide.imageRes),
            contentDescription = null,
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(ILLUSTRATION_ASPECT_RATIO),
            contentScale = ContentScale.Fit,
        )
        MoshiText(
            text = stringResource(slide.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.space6),
            role = MoshiTextRole.Heading,
            textAlign = TextAlign.Center,
        )
        MoshiText(
            text = stringResource(slide.bodyRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.space2),
            role = MoshiTextRole.Body,
            textAlign = TextAlign.Center,
            color = Moshi.colors.foreground.secondary,
        )
    }
}

@Composable
private fun TermsRow(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = Moshi.colors.foreground.link,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        ),
    )
    val termsLabel = stringResource(R.string.welcome_terms_of_service)
    val privacyLabel = stringResource(R.string.welcome_privacy_policy)
    val template = stringResource(R.string.welcome_terms, "\u0000", "\u0001")
    val text = remember(template, termsLabel, privacyLabel, linkStyle, onTermsClick, onPrivacyClick) {
        val termsIndex = template.indexOf('\u0000')
        val privacyIndex = template.indexOf('\u0001')
        buildAnnotatedString {
            append(template.substring(0, termsIndex))
            withLink(LinkAnnotation.Clickable(tag = termsLabel, styles = linkStyle) { onTermsClick() }) {
                append(termsLabel)
            }
            append(template.substring(termsIndex + 1, privacyIndex))
            withLink(LinkAnnotation.Clickable(tag = privacyLabel, styles = linkStyle) { onPrivacyClick() }) {
                append(privacyLabel)
            }
            append(template.substring(privacyIndex + 1))
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        MoshiCheckbox(checked = accepted, onCheckedChange = onAcceptedChange)
        MoshiText(
            text = text,
            modifier = Modifier
                .weight(1f)
                .clickable { onAcceptedChange(!accepted) },
            role = MoshiTextRole.Small,
            color = Moshi.colors.foreground.secondary,
            textAlign = TextAlign.Start,
        )
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenPreview() {
    MoshiTheme {
        WelcomeContent(
            state = WelcomeScreenUiState(),
            onTermsAcceptedChange = {},
            onGoogleSignIn = {},
            onEmailSignIn = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onEmailSignInRecoveryDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenTermsAcceptedPreview() {
    MoshiTheme {
        WelcomeContent(
            state = WelcomeScreenUiState(termsAccepted = true),
            onTermsAcceptedChange = {},
            onGoogleSignIn = {},
            onEmailSignIn = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onEmailSignInRecoveryDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenSigningInPreview() {
    MoshiTheme {
        WelcomeContent(
            state = WelcomeScreenUiState(termsAccepted = true, isSigningIn = true),
            onTermsAcceptedChange = {},
            onGoogleSignIn = {},
            onEmailSignIn = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onEmailSignInRecoveryDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenEmailRecoveryPreview() {
    MoshiTheme {
        WelcomeContent(
            state = WelcomeScreenUiState(termsAccepted = true, showEmailSignInRecovery = true),
            onTermsAcceptedChange = {},
            onGoogleSignIn = {},
            onEmailSignIn = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onEmailSignInRecoveryDismissed = {},
        )
    }
}
