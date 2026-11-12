package co.mipyme.auth.internal

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SignInAttempt
import co.mipyme.auth.SignInStrategy
import co.mipyme.contract.IdentityAccount
import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.fetchToken
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.sendCode
import com.clerk.api.signin.verifyCode
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.update
import com.clerk.api.signup.sendCode as sendSignUpCode
import com.clerk.api.signup.verifyCode as verifySignUpCode
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.User
import com.clerk.api.user.get
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OTP_COOLDOWN_SECONDS = 60
private const val ONE_SECOND_MILLIS = 1_000L
private const val GOOGLE_CANCELLED_MESSAGE = "Authentication cancelled"
private const val MFA_NOT_SUPPORTED_MESSAGE = "Second factor is not supported"
private const val FORM_IDENTIFIER_NOT_FOUND = "form_identifier_not_found"
private const val LEGAL_ACCEPTED_FIELD = "legal_accepted"
private const val TOKEN_TEMPLATE = "mipyme-pos"

internal class ClerkAuthService(
    private val applicationScope: CoroutineScope,
    private val accountCache: AccountCache,
) : AuthService {

    private val otpCooldownSecondsRemaining = MutableStateFlow(0)
    // Written from Clerk continuations and read from callers' dispatchers.
    @Volatile
    private var otpCooldownEmail: String? = null
    private var otpCooldownJob: Job? = null
    @Volatile
    private var pendingEmailChallenge: EmailChallenge? = null

    override val authStateFlow: StateFlow<AuthState> = accountCache.authStateFlow
        .stateIn(applicationScope, SharingStarted.Eagerly, AuthState.Initializing)

    override val otpCooldownSecondsRemainingFlow: StateFlow<Int> =
        otpCooldownSecondsRemaining.asStateFlow()

    override suspend fun signIn(strategy: SignInStrategy): Either<AuthError, SignInAttempt> {
        // Reject a too-early retry before resetting anything, so the current state stays intact.
        if (strategy is SignInStrategy.EmailCode &&
            isOtpCooldownBlocking(otpCooldownSecondsRemaining.value, otpCooldownEmail, strategy.email)
        ) {
            return AuthError.OtpResendRateLimited.left()
        }
        pendingEmailChallenge = null
        // Discard a stale Clerk session first; doing it through the cache keeps the resulting
        // session change from being reported as an expired session.
        val reset = accountCache.clearAfter {
            if (Clerk.isSignedIn) {
                Clerk.auth.signOut()
            }
            Unit.right()
        }
        reset.onLeft { error -> return error.left() }
        val result = when (strategy) {
            is SignInStrategy.EmailCode -> signInWithEmailCode(strategy.email)
            SignInStrategy.Google -> signInWithGoogle()
        }
        return result.onLeft { error -> markBlockedIfLocked(error) }
    }

    override suspend fun completeChallenge(code: String): Either<AuthError, SignInAttempt> {
        val result = when (val challenge = pendingEmailChallenge ?: currentEmailChallenge()) {
            null -> AuthError.MissingOtpChallenge.left()
            is EmailChallenge.ExistingAccount -> completeSignInChallenge(challenge.signIn, code)
            is EmailChallenge.NewAccount -> completeSignUpChallenge(challenge.signUp, code)
        }
        return result.onLeft { error -> markBlockedIfLocked(error) }
    }

    override suspend fun resendOtpCode(): Either<AuthError, Unit> {
        if (otpCooldownSecondsRemaining.value > 0) {
            return AuthError.OtpResendRateLimited.left()
        }
        val (email, result) = when (val challenge = pendingEmailChallenge ?: currentEmailChallenge()) {
            null -> return AuthError.MissingOtpChallenge.left()
            is EmailChallenge.ExistingAccount -> {
                val email = challenge.signIn.identifier ?: return AuthError.MissingOtpChallenge.left()
                email to challenge.signIn.sendCode { this.email = email }
            }
            is EmailChallenge.NewAccount -> {
                val email = challenge.signUp.emailAddress ?: return AuthError.MissingOtpChallenge.left()
                email to challenge.signUp.sendSignUpCode { this.email = email }
            }
        }
        return when (result) {
            is ClerkResult.Failure -> result.toAuthError().left()
            is ClerkResult.Success -> {
                startOtpCooldown(email)
                Unit.right()
            }
        }
    }

    override suspend fun refresh(): Either<AuthError, Unit> {
        val user = Clerk.user ?: return AuthError.NotAuthenticated.left()
        return when (val result = user.get()) {
            is ClerkResult.Failure -> result.toAuthError().left()
            is ClerkResult.Success -> {
                accountCache.saveIfSessionStillActive(result.value.toIdentityAccount(), ::isActiveSessionOf)
            }
        }
    }

    override suspend fun signOut(): Either<AuthError, Unit> {
        pendingEmailChallenge = null
        return accountCache.clearAfter {
            val session = Clerk.session
            if (session == null) {
                // Without a server-confirmed state a session may still exist remotely.
                if (Clerk.isInitialized.value) Unit.right() else AuthError.Network.left()
            } else {
                signOutFromServer(session)
            }
        }
    }

    override suspend fun getToken(): Either<AuthError, String> {
        val session = Clerk.session
        if (session == null || session.status != Session.SessionStatus.ACTIVE) {
            return AuthError.NotAuthenticated.left()
        }
        return when (val result = session.fetchToken(GetTokenOptions(template = TOKEN_TEMPLATE))) {
            is ClerkResult.Failure -> result.toAuthError(missingDetailsMeansNetwork = true).left()
            is ClerkResult.Success -> result.value.jwt.right()
        }
    }

    // Signing out by session id only touches the server; the no-argument variant wipes the local
    // session even when the request fails, which would sign the merchant out while offline.
    private suspend fun signOutFromServer(session: Session): Either<AuthError, Unit> {
        return when (val result = Clerk.auth.signOut(sessionId = session.id)) {
            is ClerkResult.Failure -> result.toAuthError(missingDetailsMeansNetwork = true).left()
            is ClerkResult.Success -> {
                if (Clerk.session != null) {
                    // The server session is gone; this only clears Clerk's local state.
                    Clerk.auth.signOut()
                }
                Unit.right()
            }
        }
    }

    private suspend fun signInWithEmailCode(email: String): Either<AuthError, SignInAttempt> {
        return when (val signInResult = Clerk.auth.signIn { this.email = email }) {
            is ClerkResult.Failure -> {
                if (signInResult.errorCode() == FORM_IDENTIFIER_NOT_FOUND) {
                    signUpWithEmailCode(email)
                } else {
                    signInResult.toAuthError().left()
                }
            }
            is ClerkResult.Success -> {
                val signIn = signInResult.value
                when (val sendCodeResult = signIn.sendCode { this.email = email }) {
                    is ClerkResult.Failure -> sendCodeResult.toAuthError().left()
                    is ClerkResult.Success -> {
                        challengeRequired(email, EmailChallenge.ExistingAccount(signIn))
                    }
                }
            }
        }
    }

    private suspend fun signUpWithEmailCode(email: String): Either<AuthError, SignInAttempt> {
        return when (val signUpResult = Clerk.auth.signUp { this.email = email; legalAccepted = true }) {
            is ClerkResult.Failure -> signUpResult.toAuthError().left()
            is ClerkResult.Success -> {
                val signUp = signUpResult.value
                when (val sendCodeResult = signUp.sendSignUpCode { this.email = email }) {
                    is ClerkResult.Failure -> sendCodeResult.toAuthError().left()
                    is ClerkResult.Success -> {
                        challengeRequired(email, EmailChallenge.NewAccount(signUp))
                    }
                }
            }
        }
    }

    private suspend fun signInWithGoogle(): Either<AuthError, SignInAttempt> {
        return when (val result = Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)) {
            is ClerkResult.Failure -> {
                if (result.shouldSuppressCredentialFlowError ||
                    result.throwable?.message == GOOGLE_CANCELLED_MESSAGE
                ) {
                    AuthError.OAuthCancelled.left()
                } else {
                    result.toAuthError().left()
                }
            }
            is ClerkResult.Success -> SignInAttempt.Completed.right()
        }
    }

    private suspend fun completeSignInChallenge(
        signIn: SignIn,
        code: String,
    ): Either<AuthError, SignInAttempt> {
        return when (val result = signIn.verifyCode(code)) {
            is ClerkResult.Failure -> result.toAuthError().left()
            is ClerkResult.Success -> {
                pendingEmailChallenge = null
                handlePostSignIn(result.value)
            }
        }
    }

    private suspend fun completeSignUpChallenge(
        signUp: SignUp,
        code: String,
    ): Either<AuthError, SignInAttempt> {
        return when (val result = signUp.verifySignUpCode(code, VerificationType.EMAIL)) {
            is ClerkResult.Failure -> result.toAuthError().left()
            is ClerkResult.Success -> completeSignUp(result.value)
        }
    }

    private suspend fun completeSignUp(signUp: SignUp): Either<AuthError, SignInAttempt> {
        if (signUp.status == SignUp.Status.COMPLETE) {
            pendingEmailChallenge = null
            return SignInAttempt.Completed.right()
        }
        val onlyLegalAcceptanceMissing = signUp.status == SignUp.Status.MISSING_REQUIREMENTS &&
            signUp.missingFields.toSet() == setOf(LEGAL_ACCEPTED_FIELD)
        if (!onlyLegalAcceptanceMissing) {
            return AuthError.Unknown("Missing sign-up requirements: ${signUp.missingFields}").left()
        }
        return when (val result = signUp.update { legalAccepted = true }) {
            is ClerkResult.Failure -> result.toAuthError().left()
            is ClerkResult.Success -> {
                if (result.value.status == SignUp.Status.COMPLETE) {
                    pendingEmailChallenge = null
                    SignInAttempt.Completed.right()
                } else {
                    AuthError.Unknown("Missing sign-up requirements: ${result.value.missingFields}").left()
                }
            }
        }
    }

    private fun handlePostSignIn(signIn: SignIn): Either<AuthError, SignInAttempt> {
        return when (signIn.status) {
            SignIn.Status.COMPLETE -> SignInAttempt.Completed.right()
            SignIn.Status.NEEDS_SECOND_FACTOR -> AuthError.Unknown(MFA_NOT_SUPPORTED_MESSAGE).left()
            else -> AuthError.Unknown("This error should never happen").left()
        }
    }

    private fun challengeRequired(
        email: String,
        challenge: EmailChallenge,
    ): Either<AuthError, SignInAttempt> {
        pendingEmailChallenge = challenge
        startOtpCooldown(email)
        return SignInAttempt.ChallengeRequired(email = email).right()
    }

    private fun currentEmailChallenge(): EmailChallenge? {
        return Clerk.auth.currentSignIn?.let { EmailChallenge.ExistingAccount(it) }
            ?: Clerk.auth.currentSignUp?.let { EmailChallenge.NewAccount(it) }
    }

    private fun startOtpCooldown(email: String) {
        otpCooldownJob?.cancel()
        otpCooldownEmail = email.trim()
        otpCooldownSecondsRemaining.update { OTP_COOLDOWN_SECONDS }
        otpCooldownJob = applicationScope.launch {
            while (otpCooldownSecondsRemaining.value > 0) {
                delay(ONE_SECOND_MILLIS)
                otpCooldownSecondsRemaining.update { (it - 1).coerceAtLeast(0) }
            }
        }
    }

    private fun markBlockedIfLocked(error: AuthError) {
        if (error == AuthError.UserLocked || error == AuthError.UserBanned) {
            accountCache.markBlocked()
        }
    }

    private suspend fun reactToClerkSession(snapshot: ClerkSnapshot) {
        val sessionState = snapshot.session?.status?.toSessionState()
        val hasCachedAccount = accountCache.hasAccount() ?: return
        when (decideCacheAction(snapshot.isInitialized, sessionState, hasCachedAccount)) {
            CacheAction.UpdateFromActiveSession -> {
                val account = snapshot.session?.latestAccount() ?: return
                accountCache.saveIfSessionStillActive(account, ::isActiveSessionOf)
            }
            CacheAction.ClearAsExpired -> accountCache.clearAsExpired()
            CacheAction.None -> Unit
        }
    }

    private fun isActiveSessionOf(userId: String): Boolean {
        val session = Clerk.session
        return session?.status == Session.SessionStatus.ACTIVE && session.user?.id == userId
    }

    private suspend fun Session.latestAccount(): IdentityAccount? {
        val sessionUser = user ?: return null
        return when (val result = sessionUser.get()) {
            is ClerkResult.Failure -> sessionUser.toIdentityAccount()
            is ClerkResult.Success -> result.value.toIdentityAccount()
        }
    }

    init {
        combine(Clerk.isInitialized, Clerk.sessionFlow) { isInitialized, session ->
            ClerkSnapshot(isInitialized = isInitialized, session = session)
        }
            .distinctUntilChanged { previous, current ->
                previous.isInitialized == current.isInitialized &&
                    previous.session?.id == current.session?.id &&
                    previous.session?.status == current.session?.status
            }
            .onEach { snapshot -> reactToClerkSession(snapshot) }
            .launchIn(applicationScope)
    }

    private sealed interface EmailChallenge {
        data class ExistingAccount(val signIn: SignIn) : EmailChallenge
        data class NewAccount(val signUp: SignUp) : EmailChallenge
    }

    private data class ClerkSnapshot(val isInitialized: Boolean, val session: Session?)
}

private fun Session.SessionStatus.toSessionState(): SessionState {
    return when (this) {
        Session.SessionStatus.ACTIVE -> SessionState.ACTIVE
        Session.SessionStatus.PENDING -> SessionState.PENDING
        Session.SessionStatus.UNKNOWN -> SessionState.UNKNOWN
        Session.SessionStatus.ABANDONED -> SessionState.ABANDONED
        Session.SessionStatus.ENDED -> SessionState.ENDED
        Session.SessionStatus.EXPIRED -> SessionState.EXPIRED
        Session.SessionStatus.REMOVED -> SessionState.REMOVED
        Session.SessionStatus.REPLACED -> SessionState.REPLACED
        Session.SessionStatus.REVOKED -> SessionState.REVOKED
    }
}

private fun User.toIdentityAccount(): IdentityAccount {
    return identityAccountOf(
        id = id,
        email = primaryEmailAddress?.emailAddress,
        firstName = firstName,
        lastName = lastName,
        imageUrl = imageUrl,
        publicMetadata = publicMetadata,
    )
}

private fun ClerkResult.Failure<ClerkErrorResponse>.errorCode(): String? {
    return error?.errors?.firstOrNull()?.code
}

// Some Clerk calls (sign-out by session id, token fetching) drop the IOException and return a
// failure without error details; for those, missing details means the request never got through.
private fun ClerkResult.Failure<ClerkErrorResponse>.toAuthError(
    missingDetailsMeansNetwork: Boolean = false,
): AuthError {
    val isIoFailure = errorType == ClerkResult.Failure.ErrorType.UNKNOWN && throwable is IOException
    val hasNoDetails = error?.errors.isNullOrEmpty()
    val isNetworkFailure = isIoFailure || (missingDetailsMeansNetwork && hasNoDetails)
    return mapAuthErrorCode(code = errorCode(), isNetworkFailure = isNetworkFailure)
}
