package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.GameStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var repository: StatsRepository

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir.toFile(), "test_stats.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        dataStoreManager = DataStoreManager(dataStore)
        repository = DataStoreStatsRepository(dataStoreManager)
    }

    @Nested
    @DisplayName("Default Statistics Tests")
    inner class DefaultStatsTests {

        @Test
        @DisplayName("verify default GameStats properties are zero or null")
        fun `verify default GameStats properties are zero or null`() = testScope.runTest {
            val stats = repository.getStats()

            assertEquals(0, stats.gamesPlayed)
            assertEquals(0, stats.gamesWon)
            assertEquals(0, stats.currentStreak)
            assertEquals(0, stats.bestStreak)
            assertNull(stats.bestTimeSeconds)
            assertNull(stats.fewestMoves)
            assertEquals(0, stats.highScore)
            assertFalse(stats.hasGameInProgress)
            assertEquals(0.0, stats.winPercentage)
            assertEquals(0, stats.winRatePercent)
        }

        @Test
        @DisplayName("statsFlow emits default GameStats initially")
        fun `statsFlow emits default GameStats initially`() = testScope.runTest {
            val stats = repository.statsFlow.first()
            assertEquals(GameStats(), stats)
        }
    }

    @Nested
    @DisplayName("Game Lifecycle & Streak Tests")
    inner class LifecycleAndStreakTests {

        @Test
        @DisplayName("recordGameStarted increments gamesPlayed and sets inProgress")
        fun `recordGameStarted increments gamesPlayed and sets inProgress`() = testScope.runTest {
            repository.recordGameStarted()

            val stats = repository.getStats()
            assertEquals(1, stats.gamesPlayed)
            assertEquals(0, stats.gamesWon)
            assertTrue(stats.hasGameInProgress)
        }

        @Test
        @DisplayName("consecutive wins increment currentStreak and bestStreak")
        fun `consecutive wins increment currentStreak and bestStreak`() = testScope.runTest {
            // Game 1
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 120, moves = 90, score = 500)

            var stats = repository.getStats()
            assertEquals(1, stats.gamesPlayed)
            assertEquals(1, stats.gamesWon)
            assertEquals(1, stats.currentStreak)
            assertEquals(1, stats.bestStreak)
            assertFalse(stats.hasGameInProgress)

            // Game 2
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 110, moves = 85, score = 550)

            stats = repository.getStats()
            assertEquals(2, stats.gamesPlayed)
            assertEquals(2, stats.gamesWon)
            assertEquals(2, stats.currentStreak)
            assertEquals(2, stats.bestStreak)

            // Game 3
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 100, moves = 80, score = 600)

            stats = repository.getStats()
            assertEquals(3, stats.gamesPlayed)
            assertEquals(3, stats.gamesWon)
            assertEquals(3, stats.currentStreak)
            assertEquals(3, stats.bestStreak)
        }

        @Test
        @DisplayName("abandoning a game by starting a new one resets currentStreak but preserves bestStreak")
        fun `abandoning a game by starting a new one resets currentStreak but preserves bestStreak`() = testScope.runTest {
            // Win 2 consecutive games
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 120, moves = 90, score = 500)
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 110, moves = 85, score = 550)

            assertEquals(2, repository.getStats().currentStreak)
            assertEquals(2, repository.getStats().bestStreak)

            // Start Game 3, but abandon by starting Game 4
            repository.recordGameStarted() // Game 3 in progress
            assertTrue(repository.getStats().hasGameInProgress)
            assertEquals(2, repository.getStats().currentStreak) // Streak preserved while playing Game 3

            repository.recordGameStarted() // Game 4 started -> Game 3 abandoned!
            val statsAfterAbandon = repository.getStats()
            assertEquals(4, statsAfterAbandon.gamesPlayed)
            assertEquals(2, statsAfterAbandon.gamesWon)
            assertEquals(0, statsAfterAbandon.currentStreak) // Streak reset to 0!
            assertEquals(2, statsAfterAbandon.bestStreak) // Best streak preserved!
            assertTrue(statsAfterAbandon.hasGameInProgress)

            // Win Game 4
            repository.recordGameWon(timeSeconds = 95, moves = 75, score = 620)
            val statsAfterWin = repository.getStats()
            assertEquals(4, statsAfterWin.gamesPlayed)
            assertEquals(3, statsAfterWin.gamesWon)
            assertEquals(1, statsAfterWin.currentStreak) // Streak starts anew at 1
            assertEquals(2, statsAfterWin.bestStreak) // Best streak still 2
        }

        @Test
        @DisplayName("explicit recordGameAbandoned resets currentStreak and clears inProgress")
        fun `explicit recordGameAbandoned resets currentStreak and clears inProgress`() = testScope.runTest {
            // Win 1 game
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 120, moves = 90, score = 500)
            assertEquals(1, repository.getStats().currentStreak)

            // Start game and explicitly abandon
            repository.recordGameStarted()
            repository.recordGameAbandoned()

            val stats = repository.getStats()
            assertEquals(2, stats.gamesPlayed)
            assertEquals(1, stats.gamesWon)
            assertEquals(0, stats.currentStreak)
            assertEquals(1, stats.bestStreak)
            assertFalse(stats.hasGameInProgress)
        }
    }

    @Nested
    @DisplayName("Performance Records Tests")
    inner class RecordsTests {

        @Test
        @DisplayName("bestTimeSeconds tracks the minimum elapsed time across all wins")
        fun `bestTimeSeconds tracks the minimum elapsed time across all wins`() = testScope.runTest {
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 100, score = 400)
            assertEquals(150, repository.getStats().bestTimeSeconds)

            // Slower win -> best time unchanged
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 200, moves = 100, score = 400)
            assertEquals(150, repository.getStats().bestTimeSeconds)

            // Faster win -> best time updated
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 88, moves = 100, score = 400)
            assertEquals(88, repository.getStats().bestTimeSeconds)
        }

        @Test
        @DisplayName("fewestMoves tracks the minimum moves count across all wins")
        fun `fewestMoves tracks the minimum moves count across all wins`() = testScope.runTest {
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 110, score = 400)
            assertEquals(110, repository.getStats().fewestMoves)

            // More moves -> fewest moves unchanged
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 135, score = 400)
            assertEquals(110, repository.getStats().fewestMoves)

            // Fewer moves -> fewest moves updated
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 72, score = 400)
            assertEquals(72, repository.getStats().fewestMoves)
        }

        @Test
        @DisplayName("highScore tracks the maximum score across all wins")
        fun `highScore tracks the maximum score across all wins`() = testScope.runTest {
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 100, score = 520)
            assertEquals(520, repository.getStats().highScore)

            // Lower score -> high score unchanged
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 100, score = 380)
            assertEquals(520, repository.getStats().highScore)

            // Higher score -> high score updated
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 150, moves = 100, score = 740)
            assertEquals(740, repository.getStats().highScore)
        }

        @Test
        @DisplayName("winPercentage calculates accurately")
        fun `winPercentage calculates accurately`() = testScope.runTest {
            assertEquals(0.0, repository.getStats().winPercentage)
            assertEquals(0, repository.getStats().winRatePercent)

            // 1 played, 1 won -> 100%
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 100, moves = 80, score = 500)
            assertEquals(100.0, repository.getStats().winPercentage)
            assertEquals(100, repository.getStats().winRatePercent)

            // 2 played, 1 won -> 50%
            repository.recordGameStarted()
            repository.recordGameAbandoned()
            assertEquals(50.0, repository.getStats().winPercentage)
            assertEquals(50, repository.getStats().winRatePercent)

            // 3 played, 1 won -> 33.333%
            repository.recordGameStarted()
            repository.recordGameAbandoned()
            assertEquals(33.33, repository.getStats().winPercentage, 0.01)
            assertEquals(33, repository.getStats().winRatePercent)
        }
    }

    @Nested
    @DisplayName("Reset Statistics Tests")
    inner class ResetStatsTests {

        @Test
        @DisplayName("resetStats clears all records back to empty default state")
        fun `resetStats clears all records back to empty default state`() = testScope.runTest {
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 90, moves = 65, score = 650)
            repository.recordGameStarted()
            repository.recordGameWon(timeSeconds = 85, moves = 60, score = 700)

            repository.resetStats()

            val stats = repository.getStats()
            assertEquals(0, stats.gamesPlayed)
            assertEquals(0, stats.gamesWon)
            assertEquals(0, stats.currentStreak)
            assertEquals(0, stats.bestStreak)
            assertNull(stats.bestTimeSeconds)
            assertNull(stats.fewestMoves)
            assertEquals(0, stats.highScore)
            assertFalse(stats.hasGameInProgress)
        }
    }
}
