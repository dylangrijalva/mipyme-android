package co.mipyme.network

import co.mipyme.contract.MipymeError

sealed interface NetworkError {
    data class Response(val error: MipymeError) : NetworkError
    data class Timeout(val cause: Throwable) : NetworkError
    data class Serialization(val cause: Throwable) : NetworkError
    data class NetworkUnavailable(val cause: Throwable) : NetworkError
    data class Unexpected(val cause: Throwable) : NetworkError
}
