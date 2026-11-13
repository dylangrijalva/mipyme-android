package co.mipyme.screen.sign_in_with_email

import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.auth.toStringResource
import co.mipyme.testing.FakeAuthService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInWithEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `continue is enabled only for a valid email`() = runTest(dispatcher) {
        val viewModel = SignInWithEmailViewModel(FakeAuthService())

        assertFalse(viewModel.state.isContinueEnabled)
        viewModel.onEmailChange("ada@")
        assertFalse(viewModel.state.isContinueEnabled)
        viewModel.onEmailChange("ada@example.com")
        assertTrue(viewModel.state.isContinueEnabled)
    }

    @Test
    fun `email change clears the error`() = runTest(dispatcher) {
        val authService = FakeAuthService(signInResult = AuthError.InvalidEmail.left())
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(AuthError.InvalidEmail, viewModel.state.error)

        viewModel.onEmailChange("ada@example.org")

        assertNull(viewModel.state.error)
    }

    @Test
    fun `submit with an invalid email does not call sign in`() = runTest(dispatcher) {
        val authService = FakeAuthService()
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(authService.signInStrategies.isEmpty())
    }

    @Test
    fun `submit sends the trimmed email and exposes the pending challenge`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            signInResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
        )
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("  ada@example.com ")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf<SignInStrategy>(SignInStrategy.EmailCode("ada@example.com")), authService.signInStrategies)
        assertEquals("ada@example.com", viewModel.state.pendingChallengeEmail)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun `consume challenge clears the pending challenge`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            signInResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
        )
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")
        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.consumeChallenge()

        assertNull(viewModel.state.pendingChallengeEmail)
    }

    @Test
    fun `completed sign in leaves the screen without navigation or errors`() = runTest(dispatcher) {
        val authService = FakeAuthService(signInResult = SignInAttempt.Completed.right())
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertNull(viewModel.state.pendingChallengeEmail)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `field level errors are shown inline`() = runTest(dispatcher) {
        listOf(AuthError.InvalidEmail, AuthError.OtpResendRateLimited).forEach { error ->
            val viewModel = SignInWithEmailViewModel(FakeAuthService(signInResult = error.left()))
            viewModel.onEmailChange("ada@example.com")

            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(error, viewModel.state.error)
            assertNull(viewModel.state.pendingToastRes)
            assertNull(viewModel.state.blockingErrorRes)
            assertFalse(viewModel.state.isLoading)
        }
    }

    @Test
    fun `network and unknown errors show a toast`() = runTest(dispatcher) {
        listOf(AuthError.Network, AuthError.Unknown()).forEach { error ->
            val viewModel = SignInWithEmailViewModel(FakeAuthService(signInResult = error.left()))
            viewModel.onEmailChange("ada@example.com")

            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(error.toStringResource(), viewModel.state.pendingToastRes)
            assertNull(viewModel.state.error)
            assertNull(viewModel.state.blockingErrorRes)
        }
    }

    @Test
    fun `locked and banned errors show a blocking dialog until dismissed`() = runTest(dispatcher) {
        listOf(AuthError.UserLocked, AuthError.UserBanned).forEach { error ->
            val viewModel = SignInWithEmailViewModel(FakeAuthService(signInResult = error.left()))
            viewModel.onEmailChange("ada@example.com")

            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(error.toStringResource(), viewModel.state.blockingErrorRes)
            assertNull(viewModel.state.pendingToastRes)
            viewModel.onDialogDismissed()
            assertNull(viewModel.state.blockingErrorRes)
        }
    }

    @Test
    fun `toast shown clears the pending toast`() = runTest(dispatcher) {
        val viewModel = SignInWithEmailViewModel(FakeAuthService(signInResult = AuthError.Network.left()))
        viewModel.onEmailChange("ada@example.com")
        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.onToastShown()

        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `cooldown seconds follow the auth service`() = runTest(dispatcher) {
        val authService = FakeAuthService()
        val viewModel = SignInWithEmailViewModel(authService)
        advanceUntilIdle()

        authService.otpCooldown.value = 30
        advanceUntilIdle()

        assertEquals(30, viewModel.state.otpCooldownSecondsRemaining)
    }

    @Test
    fun `double submit while busy calls sign in once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(
            signInResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
            requestGate = gate,
        )
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")

        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, authService.signInStrategies.size)
    }

    @Test
    fun `completed sign in keeps the screen locked`() = runTest(dispatcher) {
        val authService = FakeAuthService(signInResult = SignInAttempt.Completed.right())
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.isContinueEnabled)
        assertEquals(1, authService.signInStrategies.size)
    }

    @Test
    fun `cooldown hint shows only for the email the cooldown applies to`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            signInResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
        )
        val viewModel = SignInWithEmailViewModel(authService)
        viewModel.onEmailChange("ada@example.com")
        assertFalse(viewModel.state.showsCooldownHint)

        viewModel.onSubmit()
        advanceUntilIdle()
        authService.otpCooldown.value = 30
        advanceUntilIdle()

        assertTrue(viewModel.state.showsCooldownHint)
        viewModel.onEmailChange("  ADA@example.com ")
        assertTrue(viewModel.state.showsCooldownHint)
        viewModel.onEmailChange("grace@example.com")
        assertFalse(viewModel.state.showsCooldownHint)
        viewModel.onEmailChange("ada@example.com")
        authService.otpCooldown.value = 0
        advanceUntilIdle()
        assertFalse(viewModel.state.showsCooldownHint)
    }

    @Test
    fun `continue during the cooldown returns to the pending challenge without signing in again`() =
        runTest(dispatcher) {
            val authService = FakeAuthService(
                signInResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
            )
            val viewModel = SignInWithEmailViewModel(authService)
            viewModel.onEmailChange("ada@example.com")
            viewModel.onSubmit()
            advanceUntilIdle()
            viewModel.consumeChallenge()
            authService.otpCooldown.value = 30
            advanceUntilIdle()
            val signInCalls = authService.signInStrategies.size

            viewModel.onEmailChange(" ADA@example.com ")
            assertTrue(viewModel.state.isContinueEnabled)
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(signInCalls, authService.signInStrategies.size)
            assertEquals("ada@example.com", viewModel.state.pendingChallengeEmail)
            assertFalse(viewModel.state.isLoading)
        }
}
