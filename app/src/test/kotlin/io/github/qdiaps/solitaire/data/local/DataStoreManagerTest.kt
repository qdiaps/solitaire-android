package io.github.qdiaps.solitaire.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
class DataStoreManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStoreManager: DataStoreManager

    private val testStringKey = stringPreferencesKey("test_string_key")
    private val testIntKey = intPreferencesKey("test_int_key")
    private val testBoolKey = booleanPreferencesKey("test_bool_key")

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir.toFile(), "test_manager.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        dataStoreManager = DataStoreManager(dataStore)
    }

    @Nested
    @DisplayName("Preference Read and Write Tests")
    inner class ReadWriteTests {

        @Test
        @DisplayName("getPreference returns default value when key is not set")
        fun `getPreference returns default value when key is not set`() = testScope.runTest {
            val stringVal = dataStoreManager.getPreference(testStringKey, "default_val").first()
            val intVal = dataStoreManager.getPreference(testIntKey, 42).first()
            val boolVal = dataStoreManager.getPreference(testBoolKey, true).first()

            assertEquals("default_val", stringVal)
            assertEquals(42, intVal)
            assertTrue(boolVal)
        }

        @Test
        @DisplayName("setPreference persists value and updates flow")
        fun `setPreference persists value and updates flow`() = testScope.runTest {
            dataStoreManager.setPreference(testStringKey, "persisted_value")
            dataStoreManager.setPreference(testIntKey, 100)
            dataStoreManager.setPreference(testBoolKey, false)

            assertEquals("persisted_value", dataStoreManager.getPreference(testStringKey, "def").first())
            assertEquals(100, dataStoreManager.getPreference(testIntKey, 0).first())
            assertFalse(dataStoreManager.getPreference(testBoolKey, true).first())
        }

        @Test
        @DisplayName("removePreference clears specific preference key")
        fun `removePreference clears specific preference key`() = testScope.runTest {
            dataStoreManager.setPreference(testStringKey, "to_be_removed")
            assertEquals("to_be_removed", dataStoreManager.getPreference(testStringKey, "def").first())

            dataStoreManager.removePreference(testStringKey)
            assertEquals("def", dataStoreManager.getPreference(testStringKey, "def").first())
        }

        @Test
        @DisplayName("clear removes all stored preferences")
        fun `clear removes all stored preferences`() = testScope.runTest {
            dataStoreManager.setPreference(testStringKey, "val1")
            dataStoreManager.setPreference(testIntKey, 77)

            dataStoreManager.clear()

            assertEquals("def", dataStoreManager.getPreference(testStringKey, "def").first())
            assertEquals(0, dataStoreManager.getPreference(testIntKey, 0).first())
        }
    }
}
