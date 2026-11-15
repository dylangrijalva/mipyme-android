package co.mipyme.data.theme

import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import co.mipyme.testing.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRepositoryTest {

    @Test
    fun `defaults to the system theme`() = runTest {
        val repository = ThemeRepository(FakePreferencesDataStore())

        assertEquals(ThemeMode.SYSTEM, repository.themeModeFlow.first())
    }

    @Test
    fun `reads back the stored light and dark modes`() = runTest {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
            val repository = ThemeRepository(FakePreferencesDataStore())

            val result = repository.setThemeMode(mode)

            assertTrue(result.isRight())
            assertEquals(mode, repository.themeModeFlow.first())
        }
    }

    @Test
    fun `a failed write reports the failure and keeps the system theme`() = runTest {
        val repository = ThemeRepository(FakePreferencesDataStore(failWrites = true))

        val result = repository.setThemeMode(ThemeMode.DARK)

        assertEquals(Either.Left(ThemeError.PersistFailed), result)
        assertEquals(ThemeMode.SYSTEM, repository.themeModeFlow.first())
    }

    @Test
    fun `a failed read falls back to the system theme`() = runTest {
        val repository = ThemeRepository(FakePreferencesDataStore(failReads = true))

        assertEquals(ThemeMode.SYSTEM, repository.themeModeFlow.first())
    }

    @Test
    fun `an unknown stored value falls back to the system theme`() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply { set(stringPreferencesKey("theme_mode"), "SEPIA") }
        }
        val repository = ThemeRepository(dataStore)

        assertEquals(ThemeMode.SYSTEM, repository.themeModeFlow.first())
    }
}
