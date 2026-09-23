package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
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

class SolvabilityCheckerTest {

    private fun createWonBoard(): BoardState {
        val fullFoundations = Suit.entries.map { suit ->
            Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
        return BoardState(foundations = fullFoundations)
    }

    @Nested
    @DisplayName("Immediate and trivial wins")
    inner class TrivialWinsTests {

        @Test
        @DisplayName("Already won board resolves immediately with 0 moves")
        fun `already won board returns Solvable with empty moves`() {
            val wonState = createWonBoard()
            val result = SolvabilityChecker.checkSolvability(wonState)

            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertEquals(0, solvable.moves.size)
            assertEquals(1, solvable.path.size)
            assertEquals(wonState, solvable.path.first())
        }

        @Test
        @DisplayName("Single move to win resolves with 1 move")
        fun `single move to win returns Solvable with 1 move`() {
            // All foundations full except Spades is missing King
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

            val result = SolvabilityChecker.checkSolvability(state)

            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertEquals(1, solvable.moves.size)
            assertEquals(2, solvable.path.size)
            assertTrue(KlondikeRules.isGameWon(solvable.path.last()))
        }
    }

    @Nested
    @DisplayName("Multi-step solvable scenarios")
    inner class MultiStepSolvableTests {

        @Test
        @DisplayName("Solves small 2-card endgame cascade on tableau")
        fun `solves 2 card endgame cascade`() {
            // Spades, Clubs, Diamonds foundations are full (13 cards each).
            // Hearts foundation has Ace through Jack (11 cards).
            val fullSuits = listOf(Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS).map { suit ->
                Rank.entries.map { Card(suit, it, isFaceUp = true) }
            }
            val heartsFoundation = Rank.entries.take(11).map { rank ->
                Card(Suit.HEARTS, rank, isFaceUp = true)
            }

            val foundations = listOf(
                heartsFoundation,
                fullSuits[0],
                fullSuits[1],
                fullSuits[2]
            )

            // Tableau col 0 has Queen of Hearts, col 1 has King of Hearts
            val queenHearts = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
            val kingHearts = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)

            val state = BoardState(
                foundations = foundations,
                tableau = listOf(
                    listOf(queenHearts),
                    listOf(kingHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val result = SolvabilityChecker.checkSolvability(state)

            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertTrue(solvable.moves.isNotEmpty())
            assertTrue(KlondikeRules.isGameWon(solvable.path.last()))
        }

        @Test
        @DisplayName("Solves position requiring draw from stock")
        fun `solves position requiring draw from stock`() {
            // All foundations full except Hearts missing King
            val kingHearts = Card(Suit.HEARTS, Rank.KING, isFaceUp = false)
            val testFoundations = listOf(
                Rank.entries.filter { it != Rank.KING }.map { Card(Suit.HEARTS, it, isFaceUp = true) },
                Rank.entries.map { Card(Suit.SPADES, it, isFaceUp = true) },
                Rank.entries.map { Card(Suit.CLUBS, it, isFaceUp = true) },
                Rank.entries.map { Card(Suit.DIAMONDS, it, isFaceUp = true) }
            )

            val state = BoardState(
                stock = listOf(kingHearts),
                waste = emptyList(),
                foundations = testFoundations,
                tableau = List(7) { emptyList() }
            )

            val result = SolvabilityChecker.checkSolvability(state)
            assertTrue(result is SolvabilityResult.Solvable)
            val solvable = result as SolvabilityResult.Solvable
            assertTrue(solvable.moves.any { it.source is CardLocation.Stock })
            assertTrue(KlondikeRules.isGameWon(solvable.path.last()))
        }
    }

    @Nested
    @DisplayName("Unsolvable and Deadlocked states")
    inner class UnsolvableTests {

        @Test
        @DisplayName("Returns Unsolvable when no legal moves exist")
        fun `returns Unsolvable on completely blocked board`() {
            // Stock and waste empty. Foundations empty.
            // Tableau has non-Aces that cannot move anywhere.
            val state = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = listOf(
                    listOf(Card(Suit.SPADES, Rank.FIVE, isFaceUp = true)),
                    listOf(Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true)),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val result = SolvabilityChecker.checkSolvability(state)
            assertTrue(result is SolvabilityResult.Unsolvable)
            val unsolvable = result as SolvabilityResult.Unsolvable
            assertTrue(unsolvable.statesEvaluated >= 1)
        }
    }

    @Nested
    @DisplayName("Guardrails: Timeout and MaxStates limits")
    inner class GuardrailsTests {

        @Test
        @DisplayName("Times out gracefully when timeoutMs is 0")
        fun `times out gracefully when timeout is 0`() {
            val state = BoardState(
                stock = listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = false)),
                waste = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true)),
                tableau = listOf(
                    listOf(Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true)),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val config = SolverConfig(timeoutMs = 0L, maxStates = 1000)
            val result = SolvabilityChecker.checkSolvability(state, config)

            assertTrue(result is SolvabilityResult.Timeout)
        }

        @Test
        @DisplayName("Times out when maxStates limit is reached")
        fun `times out when maxStates limit is reached`() {
            val state = BoardState(
                stock = listOf(
                    Card(Suit.HEARTS, Rank.ACE, isFaceUp = false),
                    Card(Suit.SPADES, Rank.TWO, isFaceUp = false)
                ),
                tableau = List(7) { emptyList() }
            )

            val config = SolverConfig(timeoutMs = 10_000L, maxStates = 1)
            val result = SolvabilityChecker.checkSolvability(state, config)

            assertTrue(result is SolvabilityResult.Timeout)
            assertEquals(1, (result as SolvabilityResult.Timeout).statesEvaluated)
        }
    }
}
