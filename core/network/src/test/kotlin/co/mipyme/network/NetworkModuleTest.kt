package co.mipyme.network

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NetworkModuleTest {
    @Test
    fun `module provides a single api built from the injected token provider`() {
        val application = koinApplication {
            modules(
                module {
                    single { AuthTokenProvider { TEST_TOKEN } }
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
