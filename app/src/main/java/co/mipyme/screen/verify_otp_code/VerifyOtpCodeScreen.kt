package co.mipyme.screen.verify_otp_code

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import co.mipyme.ui.component.withMoshiEmphasis
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.auth.AuthError
import co.mipyme.auth.toStringResource
import co.mipyme.navigation.LocalNavBackStack
import co.mipyme.navigation.NavigationTransitions
import co.mipyme.navigation.Route
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiCodeField
import co.mipyme.ui.component.MoshiModal
import co.mipyme.ui.component.MoshiModalAction
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.navigation3.navigation

fun Module.verifyOtpCodeScreenModule(): Module {
    viewModel { params -> VerifyOtpCodeViewModel(email = params.get(), authService = get()) }
    navigation<Route.VerifyOtpCode>(
        metadata = NavigationTransitions.forwardWithBackwardPop,
    ) { route ->
        VerifyOtpCodeScreen(viewModel = koinViewModel { parametersOf(route.email) })
    }
    return this
}

@Composable
internal fun VerifyOtpCodeScreen(viewModel: VerifyOtpCodeViewModel) {
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

    VerifyOtpCodeContent(
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::onVerify,
        onResend = viewModel::onResend,
        onBack = {
            if (backStack.lastOrNull() is Route.VerifyOtpCode) {
                backStack.removeLastOrNull()
            }
        },
        onDialogDismissed = viewModel::onDialogDismissed,
    )
}

@Composable
private fun VerifyOtpCodeContent(
    state: VerifyOtpCodeScreenUiState,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val spacing = Moshi.spacing
    val colors = Moshi.colors
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val error = state.error
    val template = stringResource(R.string.verify_otp_code_description, "\u0000", OTP_CODE_LENGTH)
    val description = remember(template, state.email, colors.foreground.primary) {
        val emailIndex = template.indexOf('\u0000')
        buildAnnotatedString {
            if (emailIndex < 0) {
                append(template)
            } else {
                append(template.substring(0, emailIndex))
                withMoshiEmphasis {
                    append(state.email)
                }
                append(template.substring(emailIndex + 1))
            }
        }
    }
    val codeSupportingText = when {
        error != null -> stringResource(error.toStringResource())
        else -> null
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.surface),
    ) {
        MoshiToolbar(navigation = MoshiToolbarNavigation.Back(onClick = onBack))

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
                text = stringResource(R.string.verify_otp_code_title),
                role = MoshiTextRole.Heading24,
            )
            Spacer(Modifier.height(spacing.space2))
            MoshiText(
                text = description,
                role = MoshiTextRole.Copy16,
                color = colors.foreground.secondary,
            )
            Spacer(Modifier.height(spacing.space6))
            MoshiCodeField(
                value = state.code,
                onValueChange = onCodeChange,
                label = stringResource(R.string.verify_otp_code_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                length = OTP_CODE_LENGTH,
                isError = error != null,
                supportingText = codeSupportingText,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.isVerifyEnabled) {
                            onVerify()
                        }
                    },
                ),
            )
            if (state.wasCodeResent && error == null) {
                Spacer(Modifier.height(spacing.space2))
                MoshiText(
                    text = stringResource(R.string.verify_otp_code_resent),
                    role = MoshiTextRole.Label12,
                    color = colors.foreground.success,
                )
            }
            Spacer(Modifier.height(spacing.space6))
            MoshiButton(
                onClick = onResend,
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Primary,
                enabled = state.isResendEnabled,
                isLoading = state.isResending,
            ) {
                val label = if (state.resendSecondsRemaining > 0) {
                    stringResource(R.string.verify_otp_code_resend_countdown, state.resendSecondsRemaining)
                } else {
                    stringResource(R.string.verify_otp_code_resend)
                }
                MoshiText(text = label)
            }
            Spacer(Modifier.height(spacing.space2))
            MoshiButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                variant = MoshiButtonVariant.Text,
                enabled = !state.isVerifying,
            ) {
                MoshiText(text = stringResource(R.string.verify_otp_code_go_back))
            }
            Spacer(Modifier.height(spacing.space4))
            MoshiText(
                text = stringResource(R.string.verify_otp_code_guidance),
                role = MoshiTextRole.Copy14,
                color = colors.foreground.secondary,
            )
            Spacer(Modifier.height(spacing.space4))
        }
    }

    val blockingErrorRes = state.blockingErrorRes
    if (blockingErrorRes != null) {
        MoshiModal(
            title = stringResource(R.string.verify_otp_code_blocked_title),
            supportingText = stringResource(blockingErrorRes),
            onDismissRequest = onDialogDismissed,
            primaryAction = MoshiModalAction(
                label = stringResource(R.string.verify_otp_code_blocked_action),
                onClick = onDialogDismissed,
            ),
        )
    }
}

@ScreenPreview
@Composable
fun VerifyOtpCodeScreenPreview() {
    MoshiTheme {
        VerifyOtpCodeContent(
            state = VerifyOtpCodeScreenUiState(email = "ada@example.com", code = "123", resendSecondsRemaining = 42),
            onCodeChange = {},
            onVerify = {},
            onResend = {},
            onBack = {},
            onDialogDismissed = {},
        )
    }
}

@ScreenPreview
@Composable
fun VerifyOtpCodeScreenErrorPreview() {
    MoshiTheme {
        VerifyOtpCodeContent(
            state = VerifyOtpCodeScreenUiState(
                email = "ada@example.com",
                code = "123456",
                error = AuthError.InvalidOtpCode,
            ),
            onCodeChange = {},
            onVerify = {},
            onResend = {},
            onBack = {},
            onDialogDismissed = {},
        )
    }
}
