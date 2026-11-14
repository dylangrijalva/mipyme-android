package co.mipyme.auth

sealed interface SessionReadiness {
    data object Initializing : SessionReadiness

    data object Ready : SessionReadiness

    data object Unavailable : SessionReadiness
}
