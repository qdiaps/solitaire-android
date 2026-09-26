package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.SavedGameSession
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class GamePersistenceRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var repository: GamePersistenceRepository

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir.toFile(), "test_persistence.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        dataStoreManager = DataStoreManager(dataStore)
        repository = DataStoreGamePersistenceRepository(dataStoreManager)
    }

    private fun sampleBoard(): BoardState {
        val card1 = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "h_ace")
        val card2 = Card(Suit.SPADES, Rank.KING, isFaceUp = true, id = "s_king")
        val card3 = Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = false, id = "d_seven")

        return BoardState(
            stock = listOf(card3),
            waste = listOf(card1),
            foundations = listOf(listOf(card1), emptyList(), emptyList(), emptyList()),
            tableau = listOf(listOf(card2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            score = 145,
            movesCount = 18
        )
    }

    @Nested
    @DisplayName("Empty and Default State Tests")
    inner class EmptyStateTests {

        @Test
        @DisplayName("getSavedSession returns null when no session has been saved")
        fun `getSavedSession returns null when no session has been saved`() = testScope.runTest {
            val session = repository.getSavedSession()
            assertNull(session)
        }

        @Test
        @DisplayName("savedSessionFlow initially emits null")
        fun `savedSessionFlow initially emits null`() = testScope.runTest {
            val initial = repository.savedSessionFlow.first()
            assertNull(initial)
        }
    }

    @Nested
    @DisplayName("Save and Restore Tests")
    inner class SaveAndRestoreTests {

        @Test
        @DisplayName("saveGameSession persists session and getSavedSession restores exact data")
        fun `saveGameSession persists session and getSavedSession restores exact data`() = testScope.runTest {
            val currentBoard = sampleBoard()
            val priorBoard = BoardState(score = 50, movesCount = 5)
            val sessionToSave = SavedGameSession(
                boardState = currentBoard,
                elapsedTimeSeconds = 182L,
                drawMode = DrawMode.DRAW_THREE,
                undoHistory = listOf(priorBoard),
                savedAtTimestamp = 1727376000000L
            )

            repository.saveGameSession(sessionToSave)

            val restored = repository.getSavedSession()
            assertNotNull(restored)
            assertEquals(currentBoard, restored?.boardState)
            assertEquals(182L, restored?.elapsedTimeSeconds)
            assertEquals(DrawMode.DRAW_THREE, restored?.drawMode)
            assertEquals(1, restored?.undoHistory?.size)
            assertEquals(priorBoard, restored?.undoHistory?.first())
            assertEquals(1727376000000L, restored?.savedAtTimestamp)
        }

        @Test
        @DisplayName("savedSessionFlow emits updated session after saving")
        fun `savedSessionFlow emits updated session after saving`() = testScope.runTest {
            val session = SavedGameSession(
                boardState = sampleBoard(),
                elapsedTimeSeconds = 95L,
                drawMode = DrawMode.DRAW_ONE
            )

            repository.saveGameSession(session)

            val emitted = repository.savedSessionFlow.first()
            assertNotNull(emitted)
            assertEquals(95L, emitted?.elapsedTimeSeconds)
        }

        @Test
        @DisplayName("overwriting saved session replaces previous state")
        fun `overwriting saved session replaces previous state`() = testScope.runTest {
            val first = SavedGameSession(boardState = sampleBoard(), elapsedTimeSeconds = 50L)
            repository.saveGameSession(first)

            val second = SavedGameSession(boardState = sampleBoard(), elapsedTimeSeconds = 120L)
            repository.saveGameSession(second)

            val restored = repository.getSavedSession()
            assertEquals(120L, restored?.elapsedTimeSeconds)
        }
    }

    @Nested
    @DisplayName("Clear Session Tests")
    inner class ClearSessionTests {

        @Test
        @DisplayName("clearSavedSession removes saved session and flow emits null")
        fun `clearSavedSession removes saved session and flow emits null`() = testScope.runTest {
            val session = SavedGameSession(boardState = sampleBoard(), elapsedTimeSeconds = 60L)
            repository.saveGameSession(session)
            assertNotNull(repository.getSavedSession())

            repository.clearSavedSession()

            assertNull(repository.getSavedSession())
            assertNull(repository.savedSessionFlow.first())
        }

        @Test
        @DisplayName("clearSavedSession when nothing saved does not throw")
        fun `clearSavedSession when nothing saved does not throw`() = testScope.runTest {
            repository.clearSavedSession()
            assertNull(repository.getSavedSession())
        }
    }

    @Nested
    @DisplayName("Error Resilience Tests")
    inner class ErrorResilienceTests {

        @Test
        @DisplayName("corrupted JSON in DataStore returns null without crashing")
        fun `corrupted JSON in DataStore returns null without crashing`() = testScope.runTest {
            dataStoreManager.edit { preferences ->
                preferences[DataStoreGamePersistenceRepository.KEY_SAVED_SESSION_JSON] = "{ not valid json !!! }"
            }

            val result = repository.getSavedSession()
            assertNull(result)
        }
    }
}
