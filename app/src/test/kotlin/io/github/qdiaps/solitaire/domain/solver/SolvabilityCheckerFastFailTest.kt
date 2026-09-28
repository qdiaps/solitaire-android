package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SolvabilityCheckerFastFailTest {

    @Nested
    @DisplayName("Fast-Fail Configuration Specifications")
    inner class ConfigurationTests {

        @Test
        @DisplayName("Fast-fail constants match specifications (150ms timeout, 2000 max states)")
        fun `fast-fail constants match specification`() {
            assertEquals(150L, SolvabilityChecker.FAST_FAIL_TIMEOUT_MS)
            assertEquals(2_000, SolvabilityChecker.FAST_FAIL_MAX_STATES)
            assertEquals(150L, SolverConfig.FAST_FAIL_TIMEOUT_MS)
            assertEquals(2_000, SolverConfig.FAST_FAIL_MAX_STATES)
        }

        @Test
        @DisplayName("SolverConfig fastFail factory produces expected parameters")
        fun `SolverConfig fastFail factory creates correct config`() {
            val config = SolverConfig.fastFail(drawMode = DrawMode.DRAW_THREE)
            assertEquals(2_000, config.maxStates)
            assertEquals(150L, config.timeoutMs)
            assertEquals(DrawMode.DRAW_THREE, config.drawMode)
            assertTrue(config.autoCollapseSafePromotions)
        }
    }

    @Nested
    @DisplayName("Fast-Fail Search Execution")
    inner class SearchExecutionTests {

        @Test
        @DisplayName("Quickly solves easily winnable deal under fast-fail thresholds")
        fun `quickly solves winnable deal under fast-fail thresholds`() {
            val kingSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
            val foundations = Suit.entries.map { suit ->
                if (suit == Suit.SPADES) {
                    Rank.entries.filter { it != Rank.KING }.map { Card(suit, it, isFaceUp = true) }
                } else {
                    Rank.entries.map { Card(suit, it, isFaceUp = true) }
                }
            }

            val state = BoardState(
                foundations = foundations,
                tableau = listOf(
                    listOf(kingSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val result = SolvabilityChecker.checkSolvabilityFastFail(state)

            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertEquals(1, solvable.moves.size)
            assertTrue(solvable.statesEvaluated <= SolvabilityChecker.FAST_FAIL_MAX_STATES)
            assertTrue(solvable.durationMs <= SolvabilityChecker.FAST_FAIL_TIMEOUT_MS + 250L)
        }

        @Test
        @DisplayName("Terminates rapidly with Timeout when deal requires deep exploration or stalls")
        fun `terminates rapidly with Timeout on deep or difficult deals`() {
            val deal = KlondikeDealer.dealShuffled(Random(12345))

            val startTime = System.currentTimeMillis()
            val result = SolvabilityChecker.checkSolvabilityFastFail(deal)
            val duration = System.currentTimeMillis() - startTime

            assertTrue(result.statesEvaluated <= SolvabilityChecker.FAST_FAIL_MAX_STATES)
            assertTrue(duration < 600L, "Fast-fail took too long: ${duration}ms")
            assertTrue(
                result is SolvabilityResult.Timeout ||
                    result is SolvabilityResult.Solvable ||
                    result is SolvabilityResult.Unsolvable
            )
        }
    }
}
