package co.mipyme.network

import co.mipyme.network.api.AccountsApi
import co.mipyme.network.api.CollectionsApi
import co.mipyme.network.api.CustomersApi
import co.mipyme.network.api.LocationsApi
import co.mipyme.network.api.ProductsApi
import co.mipyme.network.api.PurchasesApi
import co.mipyme.network.api.RealtimeApi
import co.mipyme.network.api.SalesApi
import co.mipyme.network.api.SuppliersApi
import co.mipyme.network.api.SyncApi
import co.mipyme.network.internal.MipymeJson
import co.mipyme.network.internal.runSafe
import co.mipyme.network.plugin.AuthPlugin
import co.mipyme.network.plugin.skipBearerAuthorization
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.io.Closeable

private const val SOCKET_TIMEOUT_MILLIS = 30_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

class MipymeApi(
    baseUrl: String,
    tokenProvider: AuthTokenProvider,
    logger: Logger = Logger.EMPTY,
    engine: HttpClientEngine = OkHttp.create {
        config {
            retryOnConnectionFailure(false)
        }
    },
) : Closeable {
    private val client = HttpClient(engine) {
        expectSuccess = false

        install(DefaultRequest) {
            url(baseUrl)
            accept(ContentType.Application.Json)
        }

        install(AuthPlugin) {
            this.tokenProvider = tokenProvider
        }

        install(ContentNegotiation) {
            json(MipymeJson)
        }

        install(HttpTimeout) {
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }

        install(Logging) {
            this.logger = logger
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }

    val accounts = AccountsApi(client)
    val collections = CollectionsApi(client)
    val customers = CustomersApi(client)
    val locations = LocationsApi(client)
    val products = ProductsApi(client)
    val purchases = PurchasesApi(client)
    val realtime = RealtimeApi(client)
    val sales = SalesApi(client)
    val suppliers = SuppliersApi(client)
    val sync = SyncApi(client)

    suspend fun isHealthy(): Boolean {
        return client.runSafe<Unit> {
            get("/_health") {
                skipBearerAuthorization()
            }
        }.isRight()
    }

    override fun close() {
        client.close()
    }
}
