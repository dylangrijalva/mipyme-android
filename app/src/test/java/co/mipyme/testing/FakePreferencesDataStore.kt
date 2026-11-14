package co.mipyme.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakePreferencesDataStore(
    private val failReads: Boolean = false,
    private val failWrites: Boolean = false,
) : DataStore<Preferences> {
    private val preferences = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = if (failReads) {
        flow { throw IOException("Read failed") }
    } else {
        preferences
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("Write failed")
        val updated = transform(preferences.value)
        preferences.value = updated
        return updated
    }
}
