package co.mipyme.screen.sign_in_with_email

import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.toStringResource
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiModal
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextField
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation

fun Module.signInWithEmailScreenModule(): Module {
    viewModelOf(::SignInWithEmailViewModel)
    navigation<Route.SignInWithEmail>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        } + NavDisplay.popTransitionSpec {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
    ) { SignInWithEmailScreen() }
    return this
}

@Composable
internal fun SignInWithEmailScreen(viewModel: SignInWithEmailViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backStack = LocalNavBackStack.current
    val pendingToastRes = state.pendingToastRes
    val toastText = pendingToastRes?.let { stringResource(it) }
    val pendingChallengeEmail = state.pendingChallengeEmail

    LaunchedEffect(pendingToastRes) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    LaunchedEffect(pendingChallengeEmail) {
        if (pendingChallengeEmail != null) {
            viewModel.consumeChallenge()
            backStack.add(Route.VerifyOtpCode(pendingChallengeEmail))
        }
    }

    SignInWithEmailContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onClose = {
            if (backStack.lastOrNull() == Route.SignInWithEmail) {
                backStack.removeLastOrNull()
            }
        },
        onDialogDismissed = viewModel::onDialogDismissed,
    )
}

@Composable
private fun SignInWithEmailContent(
    state: SignInWithEmailScreenUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val error = state.error
    val supportingText = when {
        error != null -> stringResource(error.toStringResource())
        state.showsCooldownHint -> {
            stringResource(R.string.sign_in_with_email_cooldown, state.otpCooldownSecondsRemaining)
        }
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas),
    ) {
        MoshiToolbar(navigation = MoshiToolbarNavigation.Back(onClick = onClose))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.pageGutterNarrow),
        ) {
            Spacer(Modifier.height(spacing.space6))
            MoshiText(
                text = stringResource(R.string.sign_in_with_email_title),
                role = MoshiTextRole.Heading,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = stringResource(R.string.sign_in_with_email_subtitle),
                role = MoshiTextRole.Body,
                color = Moshi.colors.foreground.secondary,
            )
            Spacer(Modifier.height(spacing.space6))
            MoshiTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.sign_in_with_email_label),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                supportingText = supportingText,
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.isContinueEnabled) {
                            onSubmit()
                        }
                    },
                ),
                contentType = ContentType.EmailAddress,
            )
            Spacer(Modifier.height(spacing.space6))
            MoshiButton(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isContinueEnabled,
                isLoading = state.isLoading,
            ) {
                MoshiText(text = stringResource(R.string.sign_in_with_email_continue))
            }
            Spacer(Modifier.height(spacing.space4))
        }
    }

    val blockingErrorRes = state.blockingErrorRes
    if (blockingErrorRes != null) {
        MoshiModal(
            title = stringResource(R.string.sign_in_with_email_blocked_title),
            supportingText = stringResource(blockingErrorRes),
            onDismissRequest = onDialogDismissed,
            primaryAction = MoshiModalAction(
                label = stringResource(R.string.sign_in_with_email_blocked_action),
                onClick = onDialogDismissed,
            ),
        )
    }
}

@ScreenPreview
@Composable
fun SignInWithEmailScreenPreview() {
    MoshiTheme {
        SignInWithEmailContent(
            state = SignInWithEmailScreenUiState(email = "ada@example.com"),
            onEmailChange = {},
            onSubmit = {},
            onClose = {},
            onDialogDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun SignInWithEmailScreenErrorPreview() {
    MoshiTheme {
        SignInWithEmailContent(
            state = SignInWithEmailScreenUiState(
                email = "ada@",
                error = AuthError.InvalidEmail,
            ),
            onEmailChange = {},
            onSubmit = {},
            onClose = {},
            onDialogDismissed = {},
        )
    }
}
