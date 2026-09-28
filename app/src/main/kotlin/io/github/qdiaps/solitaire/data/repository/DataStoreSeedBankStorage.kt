package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.domain.solver.SeedBankState
import io.github.qdiaps.solitaire.domain.solver.SeedBankStorage
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * DataStore-backed persistent storage for [SeedBankState].
 */
class DataStoreSeedBankStorage(
    private val dataStoreManager: DataStoreManager
) : SeedBankStorage {

    companion object {
        val KEY_SEED_BANK_STATE = stringPreferencesKey("seed_bank_state")
        private val json = Json { ignoreUnknownKeys = true }
    }

    override suspend fun load(): SeedBankState? {
        val jsonString = dataStoreManager.getPreference(KEY_SEED_BANK_STATE, "").first()
        if (jsonString.isBlank()) return null
        return try {
            json.decodeFromString<SeedBankState>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun save(state: SeedBankState) {
        val jsonString = json.encodeToString(SeedBankState.serializer(), state)
        dataStoreManager.setPreference(KEY_SEED_BANK_STATE, jsonString)
    }
}
