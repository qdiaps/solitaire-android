package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.Deck
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SolverStateKeyTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val kingSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val queenHearts = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val jackClubs = Card(Suit.CLUBS, Rank.JACK, isFaceUp = true)
    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
    private val aceDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)

    @Nested
    @DisplayName("Tableau column symmetry and canonicalization")
    inner class TableauSymmetryTests {

        @Test
        @DisplayName("Swapping two tableau columns produces identical key and hash code")
        fun `swapping two tableau columns produces identical key and hash code`() {
            val col0 = listOf(kingSpades, queenHearts)
            val col1 = listOf(jackClubs)

            val state1 = BoardState(
                tableau = listOf(col0, col1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )
            val state2 = BoardState(
                tableau = listOf(col1, col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertEquals(key1, key2)
            assertEquals(key1.hashCode(), key2.hashCode())
        }

        @Test
        @DisplayName("Permuting multiple columns with empty columns preserves key equality")
        fun `permuting multiple columns preserves key equality`() {
            val colA = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
            val colB = listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
            val colC = listOf(
                Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false),
                Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true)
            )

            val state1 = BoardState(
                tableau = listOf(colA, emptyList(), colB, emptyList(), colC, emptyList(), emptyList())
            )
            val state2 = BoardState(
                tableau = listOf(emptyList(), colC, emptyList(), colA, emptyList(), colB, emptyList())
            )

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertEquals(key1, key2)
            assertEquals(key1.hashCode(), key2.hashCode())
        }

        @Test
        @DisplayName("Different face-up vs face-down card orientation produces different keys")
        fun `face-down versus face-up cards produce different keys`() {
            val colHidden = listOf(Card(Suit.HEARTS, Rank.TEN, isFaceUp = false))
            val colVisible = listOf(Card(Suit.HEARTS, Rank.TEN, isFaceUp = true))

            val state1 = BoardState(tableau = listOf(colHidden) + List(6) { emptyList() })
            val state2 = BoardState(tableau = listOf(colVisible) + List(6) { emptyList() })

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertNotEquals(key1, key2)
        }

        @Test
        @DisplayName("Different card contents in tableau produce different keys")
        fun `different card contents produce different keys`() {
            val state1 = BoardState(tableau = listOf(listOf(kingSpades)) + List(6) { emptyList() })
            val state2 = BoardState(tableau = listOf(listOf(queenHearts)) + List(6) { emptyList() })

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertNotEquals(key1, key2)
        }
    }

    @Nested
    @DisplayName("Foundation symmetry and suit packing")
    inner class FoundationSymmetryTests {

        @Test
        @DisplayName("Placing same foundation suit in different pile index produces identical key")
        fun `same foundation suit in different pile index produces identical key`() {
            // Hearts foundation in slot 0 vs slot 3
            val state1 = BoardState(
                foundations = listOf(
                    listOf(aceHearts, twoHearts),
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            )
            val state2 = BoardState(
                foundations = listOf(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(aceHearts, twoHearts)
                )
            )

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertEquals(key1, key2)
            assertEquals(key1.hashCode(), key2.hashCode())
            assertEquals(2, key1.getFoundationRank(Suit.HEARTS))
            assertEquals(0, key1.getFoundationRank(Suit.DIAMONDS))
            assertEquals(0, key1.getFoundationRank(Suit.CLUBS))
            assertEquals(0, key1.getFoundationRank(Suit.SPADES))
        }

        @Test
        @DisplayName("All four suits in foundations are correctly tracked regardless of pile position")
        fun `all four suits in foundations are correctly tracked`() {
            val state = BoardState(
                foundations = listOf(
                    listOf(aceClubs),
                    listOf(aceHearts, twoHearts),
                    listOf(aceDiamonds),
                    listOf(aceSpades)
                )
            )

            val key = SolverStateKey.from(state)

            assertEquals(2, key.getFoundationRank(Suit.HEARTS))
            assertEquals(1, key.getFoundationRank(Suit.DIAMONDS))
            assertEquals(1, key.getFoundationRank(Suit.CLUBS))
            assertEquals(1, key.getFoundationRank(Suit.SPADES))
        }

        @Test
        @DisplayName("Different foundation ranks produce different keys")
        fun `different foundation ranks produce different keys`() {
            val state1 = BoardState(foundations = listOf(listOf(aceHearts), emptyList(), emptyList(), emptyList()))
            val state2 = BoardState(foundations = listOf(listOf(aceHearts, twoHearts), emptyList(), emptyList(), emptyList()))

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertNotEquals(key1, key2)
        }
    }

    @Nested
    @DisplayName("Stock cycle and visited pruning")
    inner class StockCyclePruningTests {

        @Test
        @DisplayName("Full cycle of stock returning to start produces identical key")
        fun `full stock cycle returning to start produces identical key`() {
            val cardA = Card(Suit.HEARTS, Rank.ACE, isFaceUp = false)
            val cardB = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = false)
            val cardC = Card(Suit.CLUBS, Rank.THREE, isFaceUp = false)

            var state = BoardState(stock = listOf(cardA, cardB, cardC), waste = emptyList())
            val initialKey = SolverStateKey.from(state)

            // Draw cardA
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            val afterDraw1 = SolverStateKey.from(state)
            assertNotEquals(initialKey, afterDraw1)

            // Draw cardB
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            // Draw cardC
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            assertTrue(state.stock.isEmpty())
            assertEquals(3, state.waste.size)

            // Recycle waste back to stock
            state = KlondikeRules.recycle(state)
            assertEquals(3, state.stock.size)
            assertTrue(state.waste.isEmpty())

            // Re-draw first card
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            val afterCycleKey = SolverStateKey.from(state)

            // The state after completing one full loop and drawing cardA must match the state after first drawing cardA
            assertEquals(afterDraw1, afterCycleKey)
            assertEquals(afterDraw1.hashCode(), afterCycleKey.hashCode())
        }

        @Test
        @DisplayName("Visited set prunes cyclic stock draws")
        fun `visited set prunes cyclic stock draws`() {
            val cardA = Card(Suit.HEARTS, Rank.FOUR, isFaceUp = false)
            val cardB = Card(Suit.SPADES, Rank.FIVE, isFaceUp = false)

            val visited = mutableSetOf<SolverStateKey>()

            var state = BoardState(stock = listOf(cardA, cardB))
            visited.add(SolverStateKey.from(state))

            // Draw 1
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            assertTrue(visited.add(SolverStateKey.from(state)))

            // Draw 2
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            assertTrue(visited.add(SolverStateKey.from(state)))

            // Recycle
            state = KlondikeRules.recycle(state)
            // Now stock has cardA, cardB and waste is empty: identical to initial state!
            val keyAfterRecycle = SolverStateKey.from(state)
            assertFalse(visited.add(keyAfterRecycle), "Recycled state should already exist in visited set!")
        }

        @Test
        @DisplayName("Score and moves count do not affect key equality")
        fun `score and movesCount do not affect key equality`() {
            val state1 = BoardState(
                score = 0,
                movesCount = 0,
                stock = listOf(aceHearts),
                waste = listOf(twoHearts)
            )
            val state2 = BoardState(
                score = 500,
                movesCount = 42,
                stock = listOf(aceHearts),
                waste = listOf(twoHearts)
            )

            val key1 = SolverStateKey.from(state1)
            val key2 = SolverStateKey.from(state2)

            assertEquals(key1, key2)
            assertEquals(key1.hashCode(), key2.hashCode())
        }
    }

    @Nested
    @DisplayName("Edge cases and memory efficiency")
    inner class EdgeCasesAndPerformanceTests {

        @Test
        @DisplayName("Completely empty board key can be constructed and compared")
        fun `empty board key construction and comparison`() {
            val empty1 = BoardState()
            val empty2 = BoardState()

            val key1 = SolverStateKey.from(empty1)
            val key2 = SolverStateKey.from(empty2)

            assertEquals(key1, key2)
            assertEquals(key1.hashCode(), key2.hashCode())
        }

        @Test
        @DisplayName("Won board state produces consistent key")
        fun `won board state produces consistent key`() {
            val suits = Suit.entries
            val fullFoundations = suits.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }

            val wonState = BoardState(foundations = fullFoundations)
            val wonKey = SolverStateKey.from(wonState)

            assertEquals(13, wonKey.getFoundationRank(Suit.HEARTS))
            assertEquals(13, wonKey.getFoundationRank(Suit.DIAMONDS))
            assertEquals(13, wonKey.getFoundationRank(Suit.CLUBS))
            assertEquals(13, wonKey.getFoundationRank(Suit.SPADES))
        }

        @Test
        @DisplayName("Key creation performance benchmark for 10,000 states runs under 500ms")
        fun `key creation performance benchmark`() {
            val state = KlondikeDealer.deal(Deck.createStandard52())

            // Warm up
            repeat(100) { SolverStateKey.from(state) }

            val elapsedMs = measureTimeMillis {
                repeat(10_000) {
                    SolverStateKey.from(state)
                }
            }

            // Benchmark requirement: 10,000 keys should be generated well under 500ms
            assertTrue(elapsedMs < 500, "10,000 state keys generated in ${elapsedMs}ms, should be < 500ms")
        }

        @Test
        @DisplayName("HashSet with thousands of keys has zero false collision lookups")
        fun `hashSet with diverse keys performs fast lookups`() {
            val random = Random(42)
            val keysSet = mutableSetOf<SolverStateKey>()

            repeat(200) {
                val board = KlondikeDealer.dealShuffled(random)
                keysSet.add(SolverStateKey.from(board))
            }

            // All 200 random deals should have distinct keys
            assertEquals(200, keysSet.size)
        }
    }
}
