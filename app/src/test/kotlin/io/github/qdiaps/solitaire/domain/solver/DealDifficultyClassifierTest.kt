package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Move
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DealDifficultyClassifierTest {

    private fun createSolvableResult(statesEvaluated: Int): SolvabilityResult.Solvable {
        return SolvabilityResult.Solvable(
            moves = emptyList(),
            path = emptyList(),
            statesEvaluated = statesEvaluated,
            durationMs = 10L
        )
    }

    private fun createBoardWithTableauMoves(openingMovesCount: Int): BoardState {
        // Build 7 tableau columns
        val cols = MutableList(7) { mutableListOf<Card>() }

        when (openingMovesCount) {
            0 -> {
                // Column 0: King of Spades (no moves to foundation or other tableau)
                cols[0].add(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
                // Column 1: King of Hearts
                cols[1].add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
            }
            1 -> {
                // Move 1: Ace of Spades can move to foundation
                cols[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
                // Other column: King (no tableau-to-tableau moves)
                cols[1].add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
            }
            2 -> {
                // Move 1: Ace of Spades to foundation
                cols[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
                // Move 2: 7 of Diamonds onto 8 of Spades
                cols[1].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
                cols[2].add(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
            }
            3 -> {
                // Move 1: Ace of Spades to foundation
                cols[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
                // Move 2: Ace of Hearts to foundation
                cols[1].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
                // Move 3: 7 of Diamonds onto 8 of Spades
                cols[2].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
                cols[3].add(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
            }
        }

        return BoardState(tableau = cols)
    }

    @Nested
    @DisplayName("DealDifficulty Enum Specification")
    inner class EnumTests {

        @Test
        @DisplayName("DealDifficulty contains EASY, MEDIUM, and RANDOM entries")
        fun `DealDifficulty contains all expected difficulty modes`() {
            val names = DealDifficulty.entries.map { it.name }
            assertEquals(listOf("EASY", "MEDIUM", "RANDOM"), names)
        }
    }

    @Nested
    @DisplayName("Solvability Filtering & Failure Handling")
    inner class UnsolvableTests {

        @Test
        @DisplayName("classify returns null for Unsolvable results")
        fun `classify returns null for unsolvable results`() {
            val board = createBoardWithTableauMoves(2)
            val result = SolvabilityResult.Unsolvable(statesEvaluated = 500, durationMs = 100L)
            assertNull(DealDifficultyClassifier.classify(board, result))
        }

        @Test
        @DisplayName("classify returns null for Timeout results")
        fun `classify returns null for timeout results`() {
            val board = createBoardWithTableauMoves(2)
            val result = SolvabilityResult.Timeout(statesEvaluated = 10_000, durationMs = 1500L)
            assertNull(DealDifficultyClassifier.classify(board, result))
        }
    }

    @Nested
    @DisplayName("Easy vs Medium Classification Heuristics")
    inner class ClassificationHeuristicsTests {

        @Test
        @DisplayName("DEFAULT_MAX_EASY_STATES_EVALUATED is 1000")
        fun `DEFAULT_MAX_EASY_STATES_EVALUATED is 1000`() {
            assertEquals(1000, DealDifficultyClassifier.DEFAULT_MAX_EASY_STATES_EVALUATED)
        }

        @Test
        @DisplayName("classify returns EASY when statesEvaluated is low and opening moves are high")
        fun `classify returns EASY when states evaluated is low and opening moves are sufficient`() {
            val board = createBoardWithTableauMoves(2)
            val result = createSolvableResult(statesEvaluated = 300)

            val classification = DealDifficultyClassifier.classify(board, result)
            assertEquals(DealDifficulty.EASY, classification)
        }

        @Test
        @DisplayName("classify returns EASY at exact boundary statesEvaluated == 1000")
        fun `classify returns EASY at exact boundary statesEvaluated 1000`() {
            val board = createBoardWithTableauMoves(2)
            val result = createSolvableResult(statesEvaluated = 1000)

            val classification = DealDifficultyClassifier.classify(board, result)
            assertEquals(DealDifficulty.EASY, classification)
        }

        @Test
        @DisplayName("classify returns MEDIUM when statesEvaluated exceeds easy threshold (> 1000)")
        fun `classify returns MEDIUM when states evaluated exceeds easy limit`() {
            val board = createBoardWithTableauMoves(2)
            val result = createSolvableResult(statesEvaluated = 1001)

            val classification = DealDifficultyClassifier.classify(board, result)
            assertEquals(DealDifficulty.MEDIUM, classification)
        }

        @Test
        @DisplayName("classify returns MEDIUM when opening moves are fewer than easy threshold")
        fun `classify returns MEDIUM when opening moves are insufficient`() {
            val board = createBoardWithTableauMoves(1)
            val result = createSolvableResult(statesEvaluated = 100)

            val classification = DealDifficultyClassifier.classify(board, result)
            assertEquals(DealDifficulty.MEDIUM, classification)
        }

        @Test
        @DisplayName("classify treats empty tableau as EASY for mock boards and won states")
        fun `classify treats empty tableau as EASY`() {
            val emptyTableauBoard = BoardState(stock = listOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)))
            val result = createSolvableResult(statesEvaluated = 1)

            val classification = DealDifficultyClassifier.classify(emptyTableauBoard, result)
            assertEquals(DealDifficulty.EASY, classification)
        }
    }

    @Nested
    @DisplayName("Opening Tableau Moves Calculus")
    inner class OpeningMovesTests {

        @Test
        @DisplayName("countOpeningTableauMoves correctly counts tableau originating moves only")
        fun `countOpeningTableauMoves counts tableau moves correctly`() {
            val board0 = createBoardWithTableauMoves(0)
            val board1 = createBoardWithTableauMoves(1)
            val board2 = createBoardWithTableauMoves(2)
            val board3 = createBoardWithTableauMoves(3)

            assertEquals(0, DealDifficultyClassifier.countOpeningTableauMoves(board0))
            assertEquals(1, DealDifficultyClassifier.countOpeningTableauMoves(board1))
            assertEquals(2, DealDifficultyClassifier.countOpeningTableauMoves(board2))
            assertEquals(3, DealDifficultyClassifier.countOpeningTableauMoves(board3))
        }

        @Test
        @DisplayName("countOpeningTableauMoves ignores moves originating from stock or waste")
        fun `countOpeningTableauMoves ignores stock moves`() {
            val baseBoard = createBoardWithTableauMoves(1)
            val boardWithStock = baseBoard.copy(
                stock = listOf(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = false))
            )

            // Stock draw transition must NOT increment opening tableau moves
            assertEquals(1, DealDifficultyClassifier.countOpeningTableauMoves(boardWithStock))
        }
    }
}
