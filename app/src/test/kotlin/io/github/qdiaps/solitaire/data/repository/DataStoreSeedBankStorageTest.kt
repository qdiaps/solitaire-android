package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.domain.solver.SeedBankState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSeedBankStorageTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var storage: DataStoreSeedBankStorage

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir.toFile(), "test_seed_bank.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        dataStoreManager = DataStoreManager(dataStore)
        storage = DataStoreSeedBankStorage(dataStoreManager)
    }

    @Test
    @DisplayName("load returns null when no state is stored")
    fun `load returns null when no state is stored`() = testScope.runTest {
        assertNull(storage.load())
    }

    @Test
    @DisplayName("save and load roundtrip SeedBankState preserving all collections")
    fun `save and load roundtrip SeedBankState preserving all collections`() = testScope.runTest {
        val state = SeedBankState(
            easySeeds = listOf(101L, 102L, 103L),
            mediumSeeds = listOf(201L, 202L),
            playedSeeds = setOf(101L, 999L)
        )

        storage.save(state)
        val loaded = storage.load()

        assertNotNull(loaded)
        assertEquals(state, loaded)
    }

    @Test
    @DisplayName("load returns null on corrupted JSON data")
    fun `load returns null on corrupted JSON data`() = testScope.runTest {
        dataStoreManager.setPreference(DataStoreSeedBankStorage.KEY_SEED_BANK_STATE, "invalid-json")
        assertNull(storage.load())
    }
}
