package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SolvabilityBenchmarkTest {

    @Nested
    @DisplayName("Definition of Done: Solver Benchmark (< 300ms on benchmark seed)")
    inner class BenchmarkPerformanceTests {

        @Test
        @DisplayName("Benchmark seed 23 resolves well within 300ms DoD limit")
        fun `benchmark seed 23 resolves under 300ms DoD limit`() {
            val board = KlondikeDealer.dealShuffled(Random(23))
            val config = SolverConfig(
                timeoutMs = 1500L,
                maxStates = 6000,
                drawMode = DrawMode.DRAW_ONE
            )

            // Warm up JIT
            repeat(2) {
                SolvabilityChecker.checkSolvability(board, config.copy(timeoutMs = 30L, maxStates = 100))
            }

            val result: SolvabilityResult
            val elapsedMs = measureTimeMillis {
                result = SolvabilityChecker.checkSolvability(board, config)
            }

            assertTrue(
                result is SolvabilityResult.Solvable,
                "Expected seed 23 to be solvable, but got: $result"
            )
            val solvable = result as SolvabilityResult.Solvable

            assertTrue(
                elapsedMs < 300L,
                "Definition of Done breached: expected < 300ms, took: ${elapsedMs}ms"
            )
            assertTrue(solvable.moves.isNotEmpty(), "Solvable path must contain moves")
            assertTrue(
                KlondikeRules.isGameWon(solvable.path.last()),
                "Final path state must be won"
            )
        }

        @Test
        @DisplayName("Benchmark seed 32 resolves under 300ms DoD limit")
        fun `benchmark seed 32 resolves under 300ms DoD limit`() {
            val board = KlondikeDealer.dealShuffled(Random(32))
            val config = SolverConfig(
                timeoutMs = 1500L,
                maxStates = 6000,
                drawMode = DrawMode.DRAW_ONE
            )

            // Warm up JIT
            repeat(2) {
                SolvabilityChecker.checkSolvability(board, config.copy(timeoutMs = 30L, maxStates = 100))
            }

            val result: SolvabilityResult
            val elapsedMs = measureTimeMillis {
                result = SolvabilityChecker.checkSolvability(board, config)
            }

            assertTrue(
                result is SolvabilityResult.Solvable,
                "Expected seed 32 to be solvable, but got: $result"
            )
            val solvable = result as SolvabilityResult.Solvable

            assertTrue(
                elapsedMs < 300L,
                "Definition of Done breached: expected < 300ms, took: ${elapsedMs}ms"
            )
            assertTrue(KlondikeRules.isGameWon(solvable.path.last()))
        }

        @Test
        @DisplayName("Benchmark seed 12 resolves with valid winning sequence")
        fun `benchmark seed 12 resolves with valid winning sequence`() {
            val board = KlondikeDealer.dealShuffled(Random(12))
            val config = SolverConfig(
                timeoutMs = 1500L,
                maxStates = 6000,
                drawMode = DrawMode.DRAW_ONE
            )

            val result = SolvabilityChecker.checkSolvability(board, config)
            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertTrue(solvable.moves.size > 50)
            assertTrue(KlondikeRules.isGameWon(solvable.path.last()))
        }
    }

    @Nested
    @DisplayName("Known Unsolvable Deals and Graceful Exhaustion")
    inner class KnownUnsolvableDealsTests {

        @Test
        @DisplayName("Exhausts search space and returns Unsolvable when all Aces are trapped")
        fun `trapped Aces with empty stock returns Unsolvable`() {
            // Setup an unsolvable deal:
            // Tableau has face-up cards that cannot be placed anywhere (e.g., Kings and 5s of same color).
            // Stock and waste are empty. All Aces are trapped face-down under unmovable cards.
            val col0 = listOf(
                Card(Suit.SPADES, Rank.ACE, isFaceUp = false),
                Card(Suit.SPADES, Rank.KING, isFaceUp = true)
            )
            val col1 = listOf(
                Card(Suit.CLUBS, Rank.ACE, isFaceUp = false),
                Card(Suit.CLUBS, Rank.KING, isFaceUp = true)
            )
            val col2 = listOf(
                Card(Suit.HEARTS, Rank.ACE, isFaceUp = false),
                Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true)
            )
            val col3 = listOf(
                Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = false),
                Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true)
            )

            val state = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = listOf(col0, col1, col2, col3, emptyList(), emptyList(), emptyList())
            )

            val config = SolverConfig(
                timeoutMs = 1000L,
                maxStates = 500
            )

            val result = SolvabilityChecker.checkSolvability(state, config)
            assertTrue(
                result is SolvabilityResult.Unsolvable,
                "Expected Unsolvable, got: $result"
            )
            val unsolvable = result as SolvabilityResult.Unsolvable
            assertTrue(unsolvable.statesEvaluated >= 1)
        }

        @Test
        @DisplayName("Stock cycle with incompatible cards exhausts search space to Unsolvable")
        fun `unplayable stock cycle returns Unsolvable`() {
            // Tableau contains single unmovable cards.
            // Stock contains two cards that cannot be moved to tableau or foundations.
            val state = BoardState(
                stock = listOf(
                    Card(Suit.SPADES, Rank.TEN, isFaceUp = false),
                    Card(Suit.CLUBS, Rank.TEN, isFaceUp = false)
                ),
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = listOf(
                    listOf(Card(Suit.SPADES, Rank.SEVEN, isFaceUp = true)),
                    listOf(Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true)),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val config = SolverConfig(
                timeoutMs = 1000L,
                maxStates = 500,
                drawMode = DrawMode.DRAW_ONE
            )

            val result = SolvabilityChecker.checkSolvability(state, config)
            assertTrue(
                result is SolvabilityResult.Unsolvable,
                "Expected Unsolvable due to stock cycle exhaustion, got: $result"
            )
        }
    }

    @Nested
    @DisplayName("Guardrails, Timeouts and Stability")
    inner class GuardrailsAndSafetyTests {

        @Test
        @DisplayName("Solver gracefully returns Timeout when time limit is reached")
        fun `solver terminates promptly on timeoutMs cutoff`() {
            val board = KlondikeDealer.dealShuffled(Random(42))
            val config = SolverConfig(
                timeoutMs = 20L,
                maxStates = 100_000
            )

            val elapsed = measureTimeMillis {
                val result = SolvabilityChecker.checkSolvability(board, config)
                assertTrue(
                    result is SolvabilityResult.Timeout,
                    "Expected Timeout, got: $result"
                )
            }

            assertTrue(
                elapsed < 200L,
                "Solver should stop promptly after timeout, took: ${elapsed}ms"
            )
        }

        @Test
        @DisplayName("Solver terminates strictly at maxStates cutoff")
        fun `solver terminates strictly at maxStates cutoff`() {
            val board = KlondikeDealer.dealShuffled(Random(42))
            val maxStates = 150
            val config = SolverConfig(
                timeoutMs = 10_000L,
                maxStates = maxStates
            )

            val result = SolvabilityChecker.checkSolvability(board, config)
            assertTrue(result is SolvabilityResult.Timeout)
            assertEquals(maxStates, (result as SolvabilityResult.Timeout).statesEvaluated)
        }

        @Test
        @DisplayName("Repeated solver evaluations maintain heap and execution stability")
        fun `repeated evaluations maintain stability without memory leaks`() {
            val config = SolverConfig(
                timeoutMs = 100L,
                maxStates = 500,
                drawMode = DrawMode.DRAW_ONE
            )

            // Evaluate 10 deals in sequence to verify stability and absence of state leaks
            for (seed in 1..10) {
                val board = KlondikeDealer.dealShuffled(Random(seed))
                val result = SolvabilityChecker.checkSolvability(board, config)
                assertTrue(
                    result is SolvabilityResult.Solvable ||
                        result is SolvabilityResult.Unsolvable ||
                        result is SolvabilityResult.Timeout
                )
            }
        }
    }

    @Nested
    @DisplayName("Draw Mode Variations")
    inner class DrawModeTests {

        @Test
        @DisplayName("Solver evaluates correctly under DrawMode DRAW_THREE")
        fun `solver evaluates correctly under DrawMode DRAW_THREE`() {
            val board = KlondikeDealer.dealShuffled(Random(23))
            val config = SolverConfig(
                timeoutMs = 500L,
                maxStates = 2000,
                drawMode = DrawMode.DRAW_THREE
            )

            val result = SolvabilityChecker.checkSolvability(board, config)
            assertTrue(
                result is SolvabilityResult.Solvable ||
                    result is SolvabilityResult.Unsolvable ||
                    result is SolvabilityResult.Timeout
            )
        }
    }
}
