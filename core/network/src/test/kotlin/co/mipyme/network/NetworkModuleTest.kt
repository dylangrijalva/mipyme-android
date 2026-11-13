package co.mipyme.network

import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logger
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NetworkModuleTest {
    @Test
    fun `module provides a single api built from the injected token provider and logger`() {
        val application = koinApplication {
            modules(
                module {
                    single { AuthTokenProvider { TEST_TOKEN } }
                    single<Logger> { Logger.EMPTY }
                },
                networkModule(BASE_URL),
            )
        }

        val api = application.koin.get<MipymeApi>()

        assertNotNull(api)
        assertSame(api, application.koin.get<MipymeApi>())
        application.close()
    }
}
