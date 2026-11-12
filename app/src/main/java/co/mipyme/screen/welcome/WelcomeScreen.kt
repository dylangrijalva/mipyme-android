package co.mipyme.screen.welcome

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import co.mipyme.R
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
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

private data class WelcomeSlide(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private val welcomeSlides = listOf(
    WelcomeSlide(
        imageRes = R.drawable.ic_fluent_image_24_regular,
        titleRes = R.string.welcome_slide_sell_title,
        bodyRes = R.string.welcome_slide_sell_body,
    ),
    WelcomeSlide(
        imageRes = R.drawable.ic_fluent_image_24_regular,
        titleRes = R.string.welcome_slide_stock_title,
        bodyRes = R.string.welcome_slide_stock_body,
    ),
    WelcomeSlide(
        imageRes = R.drawable.ic_fluent_image_24_regular,
        titleRes = R.string.welcome_slide_offline_title,
        bodyRes = R.string.welcome_slide_offline_body,
    ),
)

fun Module.welcomeScreenModule(): Module {
    viewModelOf(::WelcomeViewModel)
    navigation<Route.Welcome> { WelcomeScreen() }
    return this
}

@Composable
internal fun WelcomeScreen(viewModel: WelcomeViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pendingAction = state.pendingAction
    val toastText = pendingAction?.let { stringResource(it.labelRes()) }

    LaunchedEffect(pendingAction) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onIntent(WelcomeIntent.ActionHandled)
        }
    }

    WelcomeContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeScreenUiState,
    onIntent: (WelcomeIntent) -> Unit,
) {
    val spacing = Moshi.spacing
    val pagerState = rememberPagerState(pageCount = { welcomeSlides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas)
            .safeDrawingPadding()
            .padding(horizontal = spacing.pageGutterNarrow),
    ) {
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
            onAcceptedChange = { onIntent(WelcomeIntent.TermsToggled(it)) },
            onLinkClick = { onIntent(WelcomeIntent.ActionSelected(it)) },
        )

        Column(
            modifier = Modifier.padding(top = spacing.space4, bottom = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            MoshiButton(
                onClick = { onIntent(WelcomeIntent.ActionSelected(WelcomeAction.ContinueWithEmail)) },
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Primary,
                enabled = state.termsAccepted,
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
                onClick = { onIntent(WelcomeIntent.ActionSelected(WelcomeAction.ContinueWithGoogle)) },
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Outline,
                enabled = state.termsAccepted,
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
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.3f)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .weight(1f, fill = false)
                .background(Moshi.colors.background.surface, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(slide.imageRes),
                contentDescription = null,
                modifier = Modifier.size(spacing.space8),
                tint = Moshi.colors.foreground.tertiary,
            )
        }
        MoshiText(
            text = stringResource(slide.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.space8),
            role = MoshiTextRole.Display,
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
    onLinkClick: (WelcomeAction) -> Unit,
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
    val text = remember(template, termsLabel, privacyLabel, linkStyle, onLinkClick) {
        val termsIndex = template.indexOf('\u0000')
        val privacyIndex = template.indexOf('\u0001')
        buildAnnotatedString {
            append(template.substring(0, termsIndex))
            appendLink(termsLabel, linkStyle) { onLinkClick(WelcomeAction.TermsOfService) }
            append(template.substring(termsIndex + 1, privacyIndex))
            appendLink(privacyLabel, linkStyle) { onLinkClick(WelcomeAction.PrivacyPolicy) }
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

private fun AnnotatedString.Builder.appendLink(
    label: String,
    styles: TextLinkStyles,
    onClick: () -> Unit,
) {
    withLink(LinkAnnotation.Clickable(tag = label, styles = styles) { onClick() }) {
        append(label)
    }
}

@StringRes
private fun WelcomeAction.labelRes(): Int {
    return when (this) {
        WelcomeAction.ContinueWithGoogle -> R.string.welcome_continue_with_google
        WelcomeAction.ContinueWithEmail -> R.string.welcome_continue_with_email
        WelcomeAction.TermsOfService -> R.string.welcome_terms_of_service
        WelcomeAction.PrivacyPolicy -> R.string.welcome_privacy_policy
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenPreview() {
    MoshiTheme {
        WelcomeContent(state = WelcomeScreenUiState(), onIntent = {})
    }
}

@ScreenPreview
@Composable
fun WelcomeScreenTermsAcceptedPreview() {
    MoshiTheme {
        WelcomeContent(state = WelcomeScreenUiState(termsAccepted = true), onIntent = {})
    }
}
