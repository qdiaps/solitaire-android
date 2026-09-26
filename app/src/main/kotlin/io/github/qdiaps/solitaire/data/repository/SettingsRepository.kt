package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.GameSettings
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository interface exposing reactive Solitaire configuration and user preferences.
 */
interface SettingsRepository {
    val settingsFlow: Flow<GameSettings>
    suspend fun getSettings(): GameSettings
    suspend fun updateSettings(transform: (GameSettings) -> GameSettings)
    suspend fun setDrawMode(drawMode: DrawMode)
    suspend fun setLeftHanded(isLeftHanded: Boolean)
    suspend fun setFeltTheme(feltTheme: FeltTheme)
    suspend fun setCardBackStyle(cardBackStyle: CardBackStyle)
    suspend fun setCardFaceStyle(cardFaceStyle: CardFaceStyle)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setAutoHintEnabled(enabled: Boolean)
    suspend fun resetToDefaults()
}

/**
 * DataStore-backed implementation of [SettingsRepository].
 */
class DataStoreSettingsRepository(
    private val dataStoreManager: DataStoreManager
) : SettingsRepository {

    override val settingsFlow: Flow<GameSettings> = dataStoreManager.data.map { preferences ->
        GameSettings(
            drawMode = preferences[KEY_DRAW_MODE]?.let { modeName ->
                DrawMode.entries.find { it.name == modeName } ?: DrawMode.DRAW_ONE
            } ?: DrawMode.DRAW_ONE,
            isLeftHanded = preferences[KEY_IS_LEFT_HANDED] ?: false,
            feltTheme = FeltTheme.fromId(preferences[KEY_FELT_THEME]),
            cardBackStyle = CardBackStyle.fromId(preferences[KEY_CARD_BACK_STYLE]),
            cardFaceStyle = CardFaceStyle.fromId(preferences[KEY_CARD_FACE_STYLE]),
            soundEnabled = preferences[KEY_SOUND_ENABLED] ?: true,
            hapticsEnabled = preferences[KEY_HAPTICS_ENABLED] ?: true,
            autoHintEnabled = preferences[KEY_AUTO_HINT_ENABLED] ?: false
        )
    }.distinctUntilChanged()

    override suspend fun getSettings(): GameSettings = settingsFlow.first()

    override suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val current = getSettings()
        val updated = transform(current)
        dataStoreManager.edit { preferences ->
            preferences[KEY_DRAW_MODE] = updated.drawMode.name
            preferences[KEY_IS_LEFT_HANDED] = updated.isLeftHanded
            preferences[KEY_FELT_THEME] = updated.feltTheme.id
            preferences[KEY_CARD_BACK_STYLE] = updated.cardBackStyle.id
            preferences[KEY_CARD_FACE_STYLE] = updated.cardFaceStyle.id
            preferences[KEY_SOUND_ENABLED] = updated.soundEnabled
            preferences[KEY_HAPTICS_ENABLED] = updated.hapticsEnabled
            preferences[KEY_AUTO_HINT_ENABLED] = updated.autoHintEnabled
        }
    }

    override suspend fun setDrawMode(drawMode: DrawMode) {
        dataStoreManager.setPreference(KEY_DRAW_MODE, drawMode.name)
    }

    override suspend fun setLeftHanded(isLeftHanded: Boolean) {
        dataStoreManager.setPreference(KEY_IS_LEFT_HANDED, isLeftHanded)
    }

    override suspend fun setFeltTheme(feltTheme: FeltTheme) {
        dataStoreManager.setPreference(KEY_FELT_THEME, feltTheme.id)
    }

    override suspend fun setCardBackStyle(cardBackStyle: CardBackStyle) {
        dataStoreManager.setPreference(KEY_CARD_BACK_STYLE, cardBackStyle.id)
    }

    override suspend fun setCardFaceStyle(cardFaceStyle: CardFaceStyle) {
        dataStoreManager.setPreference(KEY_CARD_FACE_STYLE, cardFaceStyle.id)
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStoreManager.setPreference(KEY_SOUND_ENABLED, enabled)
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStoreManager.setPreference(KEY_HAPTICS_ENABLED, enabled)
    }

    override suspend fun setAutoHintEnabled(enabled: Boolean) {
        dataStoreManager.setPreference(KEY_AUTO_HINT_ENABLED, enabled)
    }

    override suspend fun resetToDefaults() {
        dataStoreManager.edit { preferences ->
            preferences.remove(KEY_DRAW_MODE)
            preferences.remove(KEY_IS_LEFT_HANDED)
            preferences.remove(KEY_FELT_THEME)
            preferences.remove(KEY_CARD_BACK_STYLE)
            preferences.remove(KEY_CARD_FACE_STYLE)
            preferences.remove(KEY_SOUND_ENABLED)
            preferences.remove(KEY_HAPTICS_ENABLED)
            preferences.remove(KEY_AUTO_HINT_ENABLED)
        }
    }

    companion object {
        val KEY_DRAW_MODE = stringPreferencesKey("draw_mode")
        val KEY_IS_LEFT_HANDED = booleanPreferencesKey("is_left_handed")
        val KEY_FELT_THEME = stringPreferencesKey("felt_theme")
        val KEY_CARD_BACK_STYLE = stringPreferencesKey("card_back_style")
        val KEY_CARD_FACE_STYLE = stringPreferencesKey("card_face_style")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_AUTO_HINT_ENABLED = booleanPreferencesKey("auto_hint_enabled")
    }
}
