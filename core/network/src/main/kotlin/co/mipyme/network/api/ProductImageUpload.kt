package co.mipyme.network.api

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.mipyme.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.preparePut
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val MAX_PRODUCT_PHOTO_BYTES = 5 * 1024 * 1024

@Serializable
internal data class AuthorizeProductImageUploadRequest(val byteSize: Int)

@Serializable
internal class ProductImageUploadAuthorization(
    val id: String,
    val uploadUrl: String,
    val expiresAt: String,
    val headers: ProductImageUploadHeaders,
)

@Serializable
internal data class ProductImageUploadHeaders(
    @SerialName("content-type") val contentType: String,
    @SerialName("content-length") val contentLength: String,
)

internal suspend fun HttpClient.putProductImage(
    authorization: ProductImageUploadAuthorization,
    bytes: ByteArray,
): Either<NetworkError, Unit> {
    return try {
        val url = Url(authorization.uploadUrl)
        val headers = authorization.headers
        if (url.protocol != URLProtocol.HTTPS || url.host.isEmpty() ||
            url.user != null || url.password != null ||
            headers.contentType != "image/webp" || headers.contentLength != bytes.size.toString()
        ) {
            return NetworkError.Unexpected(IllegalStateException("Invalid product photo upload authorization")).left()
        }

        preparePut(url) {
            // The engine sends Content-Type and Content-Length from this byte-array body.
            setBody(ByteArrayContent(bytes, ContentType.parse(headers.contentType)))
        }.execute { response ->
            if (response.status.isSuccess()) {
                Unit.right()
            } else {
                NetworkError.Unexpected(IOException("Photo upload failed with HTTP ${response.status.value}")).left()
            }
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        // HTTP client exceptions can contain the signed URL.
        val cause = IOException("Photo upload request failed")
        when (exception) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> NetworkError.Timeout(cause)
            is IOException -> NetworkError.NetworkUnavailable(cause)
            else -> NetworkError.Unexpected(cause)
        }.left()
    }
}
