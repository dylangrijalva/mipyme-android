package co.mipyme.auth

import co.mipyme.contract.IdentityAccount

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val account: IdentityAccount) : AuthState
    data object SessionExpired : AuthState
    data object Blocked : AuthState
}
