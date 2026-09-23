package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KlondikeRulesStockTest {

    private val cardA = Card(Suit.HEARTS, Rank.ACE, isFaceUp = false)
    private val card2 = Card(Suit.SPADES, Rank.TWO, isFaceUp = false)
    private val card3 = Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = false)
    private val card4 = Card(Suit.CLUBS, Rank.FOUR, isFaceUp = false)
    private val card5 = Card(Suit.HEARTS, Rank.FIVE, isFaceUp = false)

    @Nested
    @DisplayName("canDraw & canRecycle validation")
    inner class AvailabilityChecks {

        @Test
        @DisplayName("canDraw returns true only when stock is not empty")
        fun `canDraw checks stock availability`() {
            val stateWithStock = BoardState(stock = listOf(cardA))
            val stateEmptyStock = BoardState(stock = emptyList())

            assertTrue(KlondikeRules.canDraw(stateWithStock))
            assertFalse(KlondikeRules.canDraw(stateEmptyStock))
        }

        @Test
        @DisplayName("canRecycle returns true only when stock is empty and waste is not empty")
        fun `canRecycle checks stock emptiness and waste presence`() {
            val validRecycleState = BoardState(stock = emptyList(), waste = listOf(cardA))
            val stockNotEmptyState = BoardState(stock = listOf(cardA), waste = listOf(card2))
            val bothEmptyState = BoardState(stock = emptyList(), waste = emptyList())

            assertTrue(KlondikeRules.canRecycle(validRecycleState))
            assertFalse(KlondikeRules.canRecycle(stockNotEmptyState))
            assertFalse(KlondikeRules.canRecycle(bothEmptyState))
        }
    }

    @Nested
    @DisplayName("Stock Draw logic")
    inner class DrawLogic {

        @Test
        @DisplayName("Draw 1 moves top card from stock to waste and turns it face-up")
        fun `draw 1 moves top card to waste and flips face-up`() {
            val initialState = BoardState(
                stock = listOf(cardA, card2, card3),
                waste = emptyList(),
                score = 50,
                movesCount = 5
            )

            val nextState = KlondikeRules.draw(initialState, DrawMode.DRAW_ONE)

            assertEquals(listOf(card2, card3), nextState.stock)
            assertEquals(1, nextState.waste.size)
            assertEquals(cardA.copy(isFaceUp = true), nextState.waste.last())
            assertEquals(6, nextState.movesCount, "movesCount should increment by 1")
            assertEquals(50, nextState.score, "Score should remain unchanged on stock draw")
        }

        @Test
        @DisplayName("Multiple Draw 1 calls stack cards face-up in waste in order")
        fun `multiple draw 1 calls stack face-up cards on waste`() {
            var state = BoardState(stock = listOf(cardA, card2, card3))

            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
            state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)

            assertEquals(listOf(card3), state.stock)
            assertEquals(2, state.waste.size)
            assertEquals(cardA.copy(isFaceUp = true), state.waste[0])
            assertEquals(card2.copy(isFaceUp = true), state.waste[1])
            assertEquals(card2.copy(isFaceUp = true), state.waste.last(), "Top of waste should be last drawn card")
            assertEquals(2, state.movesCount)
        }

        @Test
        @DisplayName("Draw 3 moves up to 3 cards to waste and orders them with 3rd on top")
        fun `draw 3 moves three cards to waste with top card playable`() {
            val initialState = BoardState(
                stock = listOf(cardA, card2, card3, card4, card5),
                waste = emptyList()
            )

            val nextState = KlondikeRules.draw(initialState, DrawMode.DRAW_THREE)

            assertEquals(listOf(card4, card5), nextState.stock)
            assertEquals(3, nextState.waste.size)
            assertEquals(listOf(
                cardA.copy(isFaceUp = true),
                card2.copy(isFaceUp = true),
                card3.copy(isFaceUp = true)
            ), nextState.waste)
            assertEquals(card3.copy(isFaceUp = true), nextState.waste.last(), "Top of waste must be the 3rd card")
            assertEquals(1, nextState.movesCount)
        }

        @Test
        @DisplayName("Draw 3 with fewer than 3 remaining cards draws all available cards")
        fun `draw 3 draws remaining cards when stock has less than 3`() {
            val initialState = BoardState(
                stock = listOf(cardA, card2),
                waste = listOf(card3.copy(isFaceUp = true))
            )

            val nextState = KlondikeRules.draw(initialState, DrawMode.DRAW_THREE)

            assertTrue(nextState.stock.isEmpty())
            assertEquals(3, nextState.waste.size)
            assertEquals(listOf(
                card3.copy(isFaceUp = true),
                cardA.copy(isFaceUp = true),
                card2.copy(isFaceUp = true)
            ), nextState.waste)
            assertEquals(card2.copy(isFaceUp = true), nextState.waste.last())
        }

        @Test
        @DisplayName("Drawing from empty stock throws IllegalStateException")
        fun `draw throws IllegalStateException when stock is empty`() {
            val emptyState = BoardState(stock = emptyList(), waste = listOf(cardA))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.draw(emptyState, DrawMode.DRAW_ONE)
            }
            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.draw(emptyState, DrawMode.DRAW_THREE)
            }
        }
    }

    @Nested
    @DisplayName("Stock Recycling logic")
    inner class RecyclingLogic {

        @Test
        @DisplayName("Recycling moves all cards from waste to stock and turns them face-down")
        fun `recycle transfers waste to stock and sets face-down`() {
            val wasteCards = listOf(
                cardA.copy(isFaceUp = true),
                card2.copy(isFaceUp = true),
                card3.copy(isFaceUp = true)
            )
            val initialState = BoardState(
                stock = emptyList(),
                waste = wasteCards,
                score = 30,
                movesCount = 10
            )

            val nextState = KlondikeRules.recycle(initialState)

            assertTrue(nextState.waste.isEmpty(), "Waste must be empty after recycle")
            assertEquals(3, nextState.stock.size)
            assertTrue(nextState.stock.all { !it.isFaceUp }, "All cards in recycled stock must be face-down")
            // Bottom of waste (cardA) should become top of stock (next drawn card)
            assertEquals(listOf(cardA, card2, card3), nextState.stock)
            assertEquals(11, nextState.movesCount, "movesCount should increment on recycle")
            assertEquals(30, nextState.score, "Score should remain unchanged on recycle")
        }

        @Test
        @DisplayName("Recycling when stock is not empty throws IllegalStateException")
        fun `recycle throws IllegalStateException when stock is not empty`() {
            val invalidState = BoardState(
                stock = listOf(cardA),
                waste = listOf(card2)
            )

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.recycle(invalidState)
            }
        }

        @Test
        @DisplayName("Recycling when waste is empty throws IllegalStateException")
        fun `recycle throws IllegalStateException when waste is empty`() {
            val invalidState = BoardState(
                stock = emptyList(),
                waste = emptyList()
            )

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.recycle(invalidState)
            }
        }
    }

    @Nested
    @DisplayName("drawOrRecycle convenience method")
    inner class DrawOrRecycleLogic {

        @Test
        @DisplayName("drawOrRecycle draws when stock is not empty")
        fun `drawOrRecycle draws if stock is available`() {
            val state = BoardState(stock = listOf(cardA, card2), waste = emptyList())
            val result = KlondikeRules.drawOrRecycle(state, DrawMode.DRAW_ONE)

            assertEquals(listOf(card2), result.stock)
            assertEquals(1, result.waste.size)
            assertEquals(1, result.movesCount)
        }

        @Test
        @DisplayName("drawOrRecycle recycles when stock is empty and waste is present")
        fun `drawOrRecycle recycles if stock is empty and waste is available`() {
            val state = BoardState(stock = emptyList(), waste = listOf(cardA.copy(isFaceUp = true)))
            val result = KlondikeRules.drawOrRecycle(state, DrawMode.DRAW_ONE)

            assertEquals(listOf(cardA), result.stock)
            assertTrue(result.waste.isEmpty())
            assertEquals(1, result.movesCount)
        }

        @Test
        @DisplayName("drawOrRecycle returns same state when both stock and waste are empty")
        fun `drawOrRecycle is no-op when both stock and waste are empty`() {
            val state = BoardState(stock = emptyList(), waste = emptyList(), movesCount = 10)
            val result = KlondikeRules.drawOrRecycle(state, DrawMode.DRAW_ONE)

            assertEquals(state, result)
            assertEquals(10, result.movesCount)
        }
    }

    @Nested
    @DisplayName("Full Cycle & Invariance Tests")
    inner class FullCycleTests {

        @Test
        @DisplayName("Full pass through standard 24-card stock and recycle preserves original card sequence")
        fun `full pass and recycle restores exact 24 card stock sequence`() {
            val dealtBoard = KlondikeDealer.dealShuffled(Random(42))
            val originalStock = dealtBoard.stock
            assertEquals(24, originalStock.size)

            // Draw all cards in Draw 3 mode (8 passes of 3 cards)
            var state = dealtBoard
            while (KlondikeRules.canDraw(state)) {
                state = KlondikeRules.draw(state, DrawMode.DRAW_THREE)
            }
            assertTrue(state.stock.isEmpty())
            assertEquals(24, state.waste.size)

            // Recycle
            state = KlondikeRules.recycle(state)
            assertTrue(state.waste.isEmpty())
            assertEquals(24, state.stock.size)

            // Stock should be identical to original stock
            assertEquals(originalStock, state.stock)
        }

        @Test
        @DisplayName("Infinite recycling does not lose or duplicate cards")
        fun `infinite recycling preserves card integrity`() {
            val dealtBoard = KlondikeDealer.dealShuffled(Random(12345))
            val originalStock = dealtBoard.stock

            var state = dealtBoard
            // Perform 5 full cycles
            repeat(5) {
                while (KlondikeRules.canDraw(state)) {
                    state = KlondikeRules.draw(state, DrawMode.DRAW_ONE)
                }
                state = KlondikeRules.recycle(state)
                assertEquals(originalStock, state.stock)
            }
        }
    }
}
