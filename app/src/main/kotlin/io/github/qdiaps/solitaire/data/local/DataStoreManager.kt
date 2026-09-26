package io.github.qdiaps.solitaire.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.solitaireDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DataStoreManager.PREFERENCES_NAME
)

/**
 * Type-safe manager wrapping Jetpack [DataStore] with error-resilient preference streams.
 */
class DataStoreManager(
    val dataStore: DataStore<Preferences>
) {
    val data: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    suspend fun <T> getPreferenceOnce(key: Preferences.Key<T>, defaultValue: T): T {
        return getPreference(key, defaultValue).first()
    }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun <T> removePreference(key: Preferences.Key<T>) {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(transform)
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        const val PREFERENCES_NAME = "solitaire_preferences"

        fun fromContext(context: Context): DataStoreManager {
            return DataStoreManager(context.solitaireDataStore)
        }
    }
}
