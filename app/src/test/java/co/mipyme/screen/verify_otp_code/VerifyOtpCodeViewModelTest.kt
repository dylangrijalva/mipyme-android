package co.mipyme.screen.verify_otp_code

import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.SignInAttempt
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
class VerifyOtpCodeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(authService: FakeAuthService): VerifyOtpCodeViewModel {
        return VerifyOtpCodeViewModel(email = "ada@example.com", authService = authService)
    }

    @Test
    fun `initial state carries the email and current cooldown`() = runTest(dispatcher) {
        val authService = FakeAuthService().apply { otpCooldown.value = 20 }

        val viewModel = viewModel(authService)

        assertEquals("ada@example.com", viewModel.state.email)
        assertEquals(20, viewModel.state.resendSecondsRemaining)
    }

    @Test
    fun `code change keeps only digits up to the code length`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService())

        viewModel.onCodeChange("1a2-3")

        assertEquals("123", viewModel.state.code)
        assertFalse(viewModel.state.isVerifyEnabled)
    }

    @Test
    fun `reaching six digits verifies automatically`() = runTest(dispatcher) {
        val authService = FakeAuthService(completeChallengeResult = SignInAttempt.Completed.right())
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("123456789")
        advanceUntilIdle()

        assertEquals(listOf("123456"), authService.completedCodes)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `fewer than six digits does not verify`() = runTest(dispatcher) {
        val authService = FakeAuthService(completeChallengeResult = SignInAttempt.Completed.right())
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("12345")
        viewModel.onVerify()
        advanceUntilIdle()

        assertTrue(authService.completedCodes.isEmpty())
    }

    @Test
    fun `code errors are shown inline`() = runTest(dispatcher) {
        listOf(
            AuthError.InvalidOtpCode,
            AuthError.OtpCodeExpired,
            AuthError.MaxOtpVerificationAttemptsReached,
            AuthError.MissingOtpChallenge,
        ).forEach { error ->
            val viewModel = viewModel(FakeAuthService(completeChallengeResult = error.left()))

            viewModel.onCodeChange("123456")
            advanceUntilIdle()

            assertEquals(error, viewModel.state.error)
            assertNull(viewModel.state.pendingToastRes)
            assertNull(viewModel.state.blockingErrorRes)
            assertFalse(viewModel.state.isVerifying)
        }
    }

    @Test
    fun `editing the code clears the inline error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(completeChallengeResult = AuthError.InvalidOtpCode.left()))
        viewModel.onCodeChange("123456")
        advanceUntilIdle()

        viewModel.onCodeChange("12345")

        assertNull(viewModel.state.error)
    }

    @Test
    fun `network error shows a toast`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(completeChallengeResult = AuthError.Network.left()))

        viewModel.onCodeChange("123456")
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertNull(viewModel.state.error)
        viewModel.onToastShown()
        assertNull(viewModel.state.pendingToastRes)
    }

    @Test
    fun `locked and banned errors show a blocking dialog`() = runTest(dispatcher) {
        listOf(AuthError.UserLocked, AuthError.UserBanned).forEach { error ->
            val viewModel = viewModel(FakeAuthService(completeChallengeResult = error.left()))

            viewModel.onCodeChange("123456")
            advanceUntilIdle()

            assertEquals(error.toStringResource(), viewModel.state.blockingErrorRes)
            viewModel.onDialogDismissed()
            assertNull(viewModel.state.blockingErrorRes)
        }
    }

    @Test
    fun `challenge required after verifying is treated as an unknown error`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            completeChallengeResult = SignInAttempt.ChallengeRequired("ada@example.com").right(),
        )
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("123456")
        advanceUntilIdle()

        assertEquals(AuthError.Unknown().toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isVerifying)
    }

    @Test
    fun `resend marks the code as resent and typing clears the message`() = runTest(dispatcher) {
        val authService = FakeAuthService(resendOtpCodeResult = Unit.right())
        val viewModel = viewModel(authService)

        viewModel.onResend()
        advanceUntilIdle()

        assertEquals(1, authService.resendOtpCodeCalls)
        assertTrue(viewModel.state.wasCodeResent)
        assertFalse(viewModel.state.isResending)

        viewModel.onCodeChange("1")
        assertFalse(viewModel.state.wasCodeResent)
    }

    @Test
    fun `resend is ignored during the cooldown`() = runTest(dispatcher) {
        val authService = FakeAuthService(resendOtpCodeResult = Unit.right()).apply { otpCooldown.value = 30 }
        val viewModel = viewModel(authService)

        viewModel.onResend()
        advanceUntilIdle()

        assertEquals(0, authService.resendOtpCodeCalls)
        assertFalse(viewModel.state.isResendEnabled)
    }

    @Test
    fun `resend failure shows a toast`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthService(resendOtpCodeResult = AuthError.Network.left()))

        viewModel.onResend()
        advanceUntilIdle()

        assertEquals(AuthError.Network.toStringResource(), viewModel.state.pendingToastRes)
        assertFalse(viewModel.state.isResending)
        assertFalse(viewModel.state.wasCodeResent)
    }

    @Test
    fun `cooldown seconds follow the auth service`() = runTest(dispatcher) {
        val authService = FakeAuthService()
        val viewModel = viewModel(authService)
        advanceUntilIdle()

        authService.otpCooldown.value = 15
        advanceUntilIdle()

        assertEquals(15, viewModel.state.resendSecondsRemaining)
    }

    @Test
    fun `double verify while busy calls complete challenge once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(
            completeChallengeResult = SignInAttempt.Completed.right(),
            requestGate = gate,
        )
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("123456")
        viewModel.onVerify()
        viewModel.onVerify()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, authService.completedCodes.size)
    }

    @Test
    fun `double resend while busy calls resend once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(resendOtpCodeResult = Unit.right(), requestGate = gate)
        val viewModel = viewModel(authService)

        viewModel.onResend()
        viewModel.onResend()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, authService.resendOtpCodeCalls)
    }

    @Test
    fun `code change is ignored while verifying`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val authService = FakeAuthService(
            completeChallengeResult = SignInAttempt.Completed.right(),
            requestGate = gate,
        )
        val viewModel = viewModel(authService)
        viewModel.onCodeChange("123456")
        advanceUntilIdle()

        viewModel.onCodeChange("654321")

        assertEquals("123456", viewModel.state.code)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `re entering the same code does not verify again`() = runTest(dispatcher) {
        val authService = FakeAuthService(completeChallengeResult = AuthError.InvalidOtpCode.left())
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("123456")
        advanceUntilIdle()
        viewModel.onCodeChange("123456")
        advanceUntilIdle()

        assertEquals(1, authService.completedCodes.size)
    }

    @Test
    fun `completed keeps the screen locked`() = runTest(dispatcher) {
        val authService = FakeAuthService(
            completeChallengeResult = SignInAttempt.Completed.right(),
            resendOtpCodeResult = Unit.right(),
        )
        val viewModel = viewModel(authService)

        viewModel.onCodeChange("123456")
        advanceUntilIdle()
        viewModel.onVerify()
        viewModel.onResend()
        viewModel.onCodeChange("111111")
        advanceUntilIdle()

        assertTrue(viewModel.state.isVerifying)
        assertFalse(viewModel.state.isVerifyEnabled)
        assertFalse(viewModel.state.isResendEnabled)
        assertEquals("123456", viewModel.state.code)
        assertEquals(1, authService.completedCodes.size)
        assertEquals(0, authService.resendOtpCodeCalls)
    }
}
