package co.mipyme.network.internal

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.contract.MipymeError
import co.mipyme.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal suspend inline fun <reified TResponse> HttpClient.runSafe(
    crossinline block: suspend HttpClient.() -> HttpResponse,
): Either<NetworkError, TResponse> {
    return try {
        val response = block(this@runSafe)
        when {
            response.status == HttpStatusCode.NoContent -> {
                @Suppress("UNCHECKED_CAST")
                (Unit as TResponse).right()
            }

            response.status.isSuccess() -> response.body<TResponse>().right()
            else -> NetworkError.Response(response.body<MipymeError>()).left()
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        val error = when (exception) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> NetworkError.Timeout(exception)

            is ContentConvertException,
            is SerializationException -> NetworkError.Serialization(exception)

            is IOException -> NetworkError.NetworkUnavailable(exception)
            else -> NetworkError.Unexpected(exception)
        }
        error.left()
    }
}
