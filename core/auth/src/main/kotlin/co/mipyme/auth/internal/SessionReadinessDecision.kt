package co.mipyme.auth.internal

import co.mipyme.auth.SessionReadiness

internal fun decideSessionReadiness(
    isInitialized: Boolean,
    hasInitializationError: Boolean,
    hasEverInitialized: Boolean = false,
): SessionReadiness {
    return when {
        isInitialized || hasEverInitialized -> SessionReadiness.Ready
        hasInitializationError -> SessionReadiness.Unavailable
        else -> SessionReadiness.Initializing
    }
}
