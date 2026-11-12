package co.mipyme.auth

import arrow.core.Either

interface TokenProvider {
    suspend fun getToken(): Either<AuthError, String>
}
