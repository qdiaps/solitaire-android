package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.data.model.GameStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VictorySummaryTest {

    @Nested
    @DisplayName("First Win / Default Stats Tests")
    inner class FirstWinTests {

        @Test
        @DisplayName("First win sets all records to true when score is positive")
        fun `first win sets all records to true when score is positive`() {
            val stats = GameStats() // No games won, null best time/moves, 0 streak/high score
            val summary = VictorySummary.calculate(
                timeSeconds = 125,
                movesCount = 82,
                score = 650,
                previousStats = stats
            )

            assertEquals(125, summary.timeSeconds)
            assertEquals(82, summary.movesCount)
            assertEquals(650, summary.score)
            assertTrue(summary.isNewBestTime)
            assertTrue(summary.isNewFewestMoves)
            assertTrue(summary.isNewHighScore)
            assertTrue(summary.isNewBestStreak)
            assertTrue(summary.hasAnyNewRecord)
        }
    }

    @Nested
    @DisplayName("Subsequent Wins Record Breakthrough Tests")
    inner class RecordBreakthroughTests {

        private val baseStats = GameStats(
            gamesPlayed = 10,
            gamesWon = 5,
            currentStreak = 2,
            bestStreak = 4,
            bestTimeSeconds = 120,
            fewestMoves = 80,
            highScore = 700
        )

        @Test
        @DisplayName("Only beating time triggers isNewBestTime")
        fun `only beating time triggers isNewBestTime`() {
            val summary = VictorySummary.calculate(
                timeSeconds = 110, // Better than 120
                movesCount = 95,   // Worse than 80
                score = 600,       // Worse than 700
                previousStats = baseStats
            )

            assertTrue(summary.isNewBestTime)
            assertFalse(summary.isNewFewestMoves)
            assertFalse(summary.isNewHighScore)
            assertFalse(summary.isNewBestStreak) // next streak is 3, <= bestStreak 4
            assertTrue(summary.hasAnyNewRecord)
        }

        @Test
        @DisplayName("Only beating moves triggers isNewFewestMoves")
        fun `only beating moves triggers isNewFewestMoves`() {
            val summary = VictorySummary.calculate(
                timeSeconds = 140, // Worse than 120
                movesCount = 72,   // Better than 80
                score = 600,
                previousStats = baseStats
            )

            assertFalse(summary.isNewBestTime)
            assertTrue(summary.isNewFewestMoves)
            assertFalse(summary.isNewHighScore)
            assertFalse(summary.isNewBestStreak)
            assertTrue(summary.hasAnyNewRecord)
        }

        @Test
        @DisplayName("Only beating score triggers isNewHighScore")
        fun `only beating score triggers isNewHighScore`() {
            val summary = VictorySummary.calculate(
                timeSeconds = 150,
                movesCount = 100,
                score = 850,       // Better than 700
                previousStats = baseStats
            )

            assertFalse(summary.isNewBestTime)
            assertFalse(summary.isNewFewestMoves)
            assertTrue(summary.isNewHighScore)
            assertFalse(summary.isNewBestStreak)
            assertTrue(summary.hasAnyNewRecord)
        }

        @Test
        @DisplayName("Extending streak beyond best streak triggers isNewBestStreak")
        fun `extending streak beyond best streak triggers isNewBestStreak`() {
            val streakStats = baseStats.copy(currentStreak = 4, bestStreak = 4)
            val summary = VictorySummary.calculate(
                timeSeconds = 150,
                movesCount = 100,
                score = 600,
                previousStats = streakStats // next streak is 5 > 4
            )

            assertFalse(summary.isNewBestTime)
            assertFalse(summary.isNewFewestMoves)
            assertFalse(summary.isNewHighScore)
            assertTrue(summary.isNewBestStreak)
            assertTrue(summary.hasAnyNewRecord)
        }

        @Test
        @DisplayName("Matching existing records does not count as new breakthrough")
        fun `matching existing records does not count as new breakthrough`() {
            val summary = VictorySummary.calculate(
                timeSeconds = 120, // Equal
                movesCount = 80,   // Equal
                score = 700,       // Equal
                previousStats = baseStats
            )

            assertFalse(summary.isNewBestTime)
            assertFalse(summary.isNewFewestMoves)
            assertFalse(summary.isNewHighScore)
            assertFalse(summary.isNewBestStreak)
            assertFalse(summary.hasAnyNewRecord)
        }
    }
}
