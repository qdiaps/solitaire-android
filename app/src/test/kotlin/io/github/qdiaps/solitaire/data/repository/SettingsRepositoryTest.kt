package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.GameSettings
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir.toFile(), "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        dataStoreManager = DataStoreManager(dataStore)
        repository = DataStoreSettingsRepository(dataStoreManager)
    }

    @Nested
    @DisplayName("Default Settings Tests")
    inner class DefaultSettingsTests {

        @Test
        @DisplayName("verify default GameSettings properties match specifications")
        fun `verify default GameSettings properties match specifications`() = testScope.runTest {
            val settings = repository.getSettings()

            assertEquals(DrawMode.DRAW_ONE, settings.drawMode)
            assertFalse(settings.isLeftHanded)
            assertEquals(FeltTheme.CLASSIC_GREEN, settings.feltTheme)
            assertEquals(CardBackStyle.CLASSIC_LATTICE, settings.cardBackStyle)
            assertEquals(CardFaceStyle.MODERN_CLEAN, settings.cardFaceStyle)
            assertTrue(settings.soundEnabled)
            assertTrue(settings.hapticsEnabled)
            assertFalse(settings.autoHintEnabled)
        }

        @Test
        @DisplayName("settingsFlow emits default settings initially")
        fun `settingsFlow emits default settings initially`() = testScope.runTest {
            val settings = repository.settingsFlow.first()
            assertEquals(GameSettings(), settings)
        }
    }

    @Nested
    @DisplayName("Settings Mutation Tests")
    inner class SettingsMutationTests {

        @Test
        @DisplayName("setDrawMode updates draw mode to DRAW_THREE")
        fun `setDrawMode updates draw mode to DRAW_THREE`() = testScope.runTest {
            repository.setDrawMode(DrawMode.DRAW_THREE)
            val updated = repository.getSettings()
            assertEquals(DrawMode.DRAW_THREE, updated.drawMode)
        }

        @Test
        @DisplayName("setLeftHanded toggles left-handed layout")
        fun `setLeftHanded toggles left-handed layout`() = testScope.runTest {
            repository.setLeftHanded(true)
            assertTrue(repository.getSettings().isLeftHanded)

            repository.setLeftHanded(false)
            assertFalse(repository.getSettings().isLeftHanded)
        }

        @Test
        @DisplayName("setFeltTheme updates felt table theme")
        fun `setFeltTheme updates felt table theme`() = testScope.runTest {
            repository.setFeltTheme(FeltTheme.DEEP_NAVY)
            assertEquals(FeltTheme.DEEP_NAVY, repository.getSettings().feltTheme)

            repository.setFeltTheme(FeltTheme.WINE_RED)
            assertEquals(FeltTheme.WINE_RED, repository.getSettings().feltTheme)
        }

        @Test
        @DisplayName("setCardBackStyle updates card back visual style")
        fun `setCardBackStyle updates card back visual style`() = testScope.runTest {
            repository.setCardBackStyle(CardBackStyle.CRIMSON_VINTAGE)
            assertEquals(CardBackStyle.CRIMSON_VINTAGE, repository.getSettings().cardBackStyle)

            repository.setCardBackStyle(CardBackStyle.OBSIDIAN_MINIMAL)
            assertEquals(CardBackStyle.OBSIDIAN_MINIMAL, repository.getSettings().cardBackStyle)
        }

        @Test
        @DisplayName("setCardFaceStyle updates card face style")
        fun `setCardFaceStyle updates card face style`() = testScope.runTest {
            repository.setCardFaceStyle(CardFaceStyle.MODERN_CLEAN)
            assertEquals(CardFaceStyle.MODERN_CLEAN, repository.getSettings().cardFaceStyle)
        }

        @Test
        @DisplayName("setSoundEnabled toggles audio feedback")
        fun `setSoundEnabled toggles audio feedback`() = testScope.runTest {
            repository.setSoundEnabled(false)
            assertFalse(repository.getSettings().soundEnabled)

            repository.setSoundEnabled(true)
            assertTrue(repository.getSettings().soundEnabled)
        }

        @Test
        @DisplayName("setHapticsEnabled toggles haptic feedback")
        fun `setHapticsEnabled toggles haptic feedback`() = testScope.runTest {
            repository.setHapticsEnabled(false)
            assertFalse(repository.getSettings().hapticsEnabled)

            repository.setHapticsEnabled(true)
            assertTrue(repository.getSettings().hapticsEnabled)
        }

        @Test
        @DisplayName("setAutoHintEnabled toggles idle auto hints")
        fun `setAutoHintEnabled toggles idle auto hints`() = testScope.runTest {
            repository.setAutoHintEnabled(true)
            assertTrue(repository.getSettings().autoHintEnabled)

            repository.setAutoHintEnabled(false)
            assertFalse(repository.getSettings().autoHintEnabled)
        }

        @Test
        @DisplayName("updateSettings atomically applies multiple setting changes")
        fun `updateSettings atomically applies multiple setting changes`() = testScope.runTest {
            repository.updateSettings { current ->
                current.copy(
                    drawMode = DrawMode.DRAW_THREE,
                    isLeftHanded = true,
                    feltTheme = FeltTheme.DARK_CHARCOAL,
                    cardBackStyle = CardBackStyle.EMERALD_ART_DECO,
                    soundEnabled = false,
                    hapticsEnabled = false,
                    autoHintEnabled = true
                )
            }

            val settings = repository.getSettings()
            assertEquals(DrawMode.DRAW_THREE, settings.drawMode)
            assertTrue(settings.isLeftHanded)
            assertEquals(FeltTheme.DARK_CHARCOAL, settings.feltTheme)
            assertEquals(CardBackStyle.EMERALD_ART_DECO, settings.cardBackStyle)
            assertFalse(settings.soundEnabled)
            assertFalse(settings.hapticsEnabled)
            assertTrue(settings.autoHintEnabled)
        }
    }

    @Nested
    @DisplayName("Reset and Fallback Tests")
    inner class ResetAndFallbackTests {

        @Test
        @DisplayName("resetToDefaults restores all preferences back to default values")
        fun `resetToDefaults restores all preferences back to default values`() = testScope.runTest {
            repository.setDrawMode(DrawMode.DRAW_THREE)
            repository.setLeftHanded(true)
            repository.setFeltTheme(FeltTheme.WINE_RED)
            repository.setSoundEnabled(false)
            repository.setAutoHintEnabled(true)

            repository.resetToDefaults()

            val resetSettings = repository.getSettings()
            assertEquals(GameSettings(), resetSettings)
        }

        @Test
        @DisplayName("corrupted or invalid enum values fall back gracefully to defaults")
        fun `corrupted or invalid enum values fall back gracefully to defaults`() = testScope.runTest {
            // Write invalid/corrupt values directly to DataStore
            dataStoreManager.setPreference(stringPreferencesKey("draw_mode"), "INVALID_DRAW_MODE")
            dataStoreManager.setPreference(stringPreferencesKey("felt_theme"), "unknown_felt_theme")
            dataStoreManager.setPreference(stringPreferencesKey("card_back_style"), "non_existent_back")
            dataStoreManager.setPreference(stringPreferencesKey("card_face_style"), "non_existent_face")

            val settings = repository.getSettings()
            assertEquals(DrawMode.DRAW_ONE, settings.drawMode)
            assertEquals(FeltTheme.CLASSIC_GREEN, settings.feltTheme)
            assertEquals(CardBackStyle.CLASSIC_LATTICE, settings.cardBackStyle)
            assertEquals(CardFaceStyle.MODERN_CLEAN, settings.cardFaceStyle)
        }
    }
}
