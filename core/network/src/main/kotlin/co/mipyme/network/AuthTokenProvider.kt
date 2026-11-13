package co.mipyme.network

fun interface AuthTokenProvider {
    suspend fun get(): String?
}
