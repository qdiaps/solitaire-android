package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.SavedGameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Repository interface for persisting and restoring active game sessions across process lifecycles.
 */
interface GamePersistenceRepository {
    /**
     * Reactive stream emitting the saved game session, or null if no session is active.
     */
    val savedSessionFlow: Flow<SavedGameSession?>

    /**
     * Retrieves the current saved game session once, or null if none exists.
     */
    suspend fun getSavedSession(): SavedGameSession?

    /**
     * Persists an in-progress game session snapshot to storage.
     *
     * @param session The [SavedGameSession] to persist.
     */
    suspend fun saveGameSession(session: SavedGameSession)

    /**
     * Clears any persisted game session snapshot (e.g., when a game is won or freshly dealt).
     */
    suspend fun clearSavedSession()
}

/**
 * DataStore Preferences implementation of [GamePersistenceRepository],
 * serializing sessions to JSON via [kotlinx.serialization].
 */
class DataStoreGamePersistenceRepository(
    private val dataStoreManager: DataStoreManager,
    private val json: Json = defaultJson
) : GamePersistenceRepository {

    override val savedSessionFlow: Flow<SavedGameSession?> = dataStoreManager.data.map { preferences ->
        val rawJson = preferences[KEY_SAVED_SESSION_JSON]
        if (rawJson.isNullOrBlank()) {
            null
        } else {
            try {
                json.decodeFromString<SavedGameSession>(rawJson)
            } catch (_: Exception) {
                null
            }
        }
    }.distinctUntilChanged()

    override suspend fun getSavedSession(): SavedGameSession? = savedSessionFlow.first()

    override suspend fun saveGameSession(session: SavedGameSession) {
        val serialized = json.encodeToString(SavedGameSession.serializer(), session)
        dataStoreManager.setPreference(KEY_SAVED_SESSION_JSON, serialized)
    }

    override suspend fun clearSavedSession() {
        dataStoreManager.removePreference(KEY_SAVED_SESSION_JSON)
    }

    companion object {
        val KEY_SAVED_SESSION_JSON = stringPreferencesKey("saved_game_session_json")

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }
}
