package co.mipyme.auth.internal

internal enum class SessionState {
    ACTIVE,
    PENDING,
    UNKNOWN,
    ABANDONED,
    ENDED,
    EXPIRED,
    REMOVED,
    REPLACED,
    REVOKED,
}

internal sealed interface CacheAction {
    data object UpdateFromActiveSession : CacheAction
    data object ClearAsExpired : CacheAction
    data object None : CacheAction
}

internal fun decideCacheAction(
    isInitialized: Boolean,
    sessionState: SessionState?,
    hasCachedAccount: Boolean,
): CacheAction {
    if (!isInitialized) return CacheAction.None
    return when (sessionState) {
        SessionState.ACTIVE -> CacheAction.UpdateFromActiveSession
        SessionState.PENDING, SessionState.UNKNOWN -> CacheAction.None
        null,
        SessionState.ABANDONED,
        SessionState.ENDED,
        SessionState.EXPIRED,
        SessionState.REMOVED,
        SessionState.REPLACED,
        SessionState.REVOKED,
        -> if (hasCachedAccount) CacheAction.ClearAsExpired else CacheAction.None
    }
}
