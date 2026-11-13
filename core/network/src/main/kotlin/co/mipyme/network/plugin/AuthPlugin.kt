package co.mipyme.network.plugin

import co.mipyme.network.AuthTokenProvider
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

private val SkipBearerAuthorization = AttributeKey<Unit>("SkipBearerAuthorization")

internal fun HttpRequestBuilder.skipBearerAuthorization() {
    attributes.put(SkipBearerAuthorization, Unit)
}

internal class AuthPluginConfig {
    lateinit var tokenProvider: AuthTokenProvider
}

internal val AuthPlugin = createClientPlugin(
    name = "AuthPlugin",
    createConfiguration = ::AuthPluginConfig,
) {
    val tokenProvider = pluginConfig.tokenProvider

    onRequest { request, _ ->
        if (request.attributes.contains(SkipBearerAuthorization)) {
            return@onRequest
        }

        request.headers.remove(HttpHeaders.Authorization)
        tokenProvider.get()?.let { token ->
            request.bearerAuth(token)
        }
    }
}
