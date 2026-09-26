package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HintResolverTest {

    private fun createEmptyBoard(): BoardState = BoardState(
        stock = emptyList(),
        waste = emptyList(),
        foundations = List(4) { emptyList() },
        tableau = List(7) { emptyList() }
    )

    @Nested
    @DisplayName("Priority 1: Tableau move uncovering face-down card")
    inner class Priority1UncoverFaceDownTests {

        @Test
        fun `recommends tableau move that uncovers face-down card over foundation promotion`() {
            // Col 0: Face-down card + Face-up 5 of Diamonds
            // Col 1: Face-up 6 of Spades
            // Waste: Ace of Clubs (can go to foundation)
            val col0 = listOf(
                Card(Suit.HEARTS, Rank.KING, isFaceUp = false),
                Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true)
            )
            val col1 = listOf(
                Card(Suit.SPADES, Rank.SIX, isFaceUp = true)
            )
            val waste = listOf(
                Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
            )

            val state = createEmptyBoard().copy(
                waste = waste,
                tableau = listOf(col0, col1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.UNCOVER_FACE_DOWN, hint!!.priority)
            assertEquals(CardLocation.Tableau(0, 1), hint.from)
            assertEquals(CardLocation.Tableau(1, 1), hint.to)
        }

        @Test
        fun `recommends foundation promotion that uncovers face-down card as Priority 1`() {
            // Col 0: Face-down card + Face-up Ace of Hearts
            val col0 = listOf(
                Card(Suit.CLUBS, Rank.TEN, isFaceUp = false),
                Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
            )

            val state = createEmptyBoard().copy(
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.UNCOVER_FACE_DOWN, hint!!.priority)
            assertEquals(CardLocation.Tableau(0, 1), hint.from)
            assertTrue(hint.to is CardLocation.Foundation)
        }

        @Test
        fun `tie-breaker prioritizes tableau column with more face-down cards`() {
            // Col 0: 1 face-down + face-up 8 of Hearts
            // Col 1: 3 face-down + face-up 8 of Diamonds
            // Col 2: Face-up 9 of Spades
            val col0 = listOf(
                Card(Suit.CLUBS, Rank.TWO, isFaceUp = false),
                Card(Suit.HEARTS, Rank.EIGHT, isFaceUp = true)
            )
            val col1 = listOf(
                Card(Suit.CLUBS, Rank.THREE, isFaceUp = false),
                Card(Suit.CLUBS, Rank.FOUR, isFaceUp = false),
                Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false),
                Card(Suit.DIAMONDS, Rank.EIGHT, isFaceUp = true)
            )
            val col2 = listOf(
                Card(Suit.SPADES, Rank.NINE, isFaceUp = true)
            )

            val state = createEmptyBoard().copy(
                tableau = listOf(col0, col1, col2, emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.UNCOVER_FACE_DOWN, hint!!.priority)
            // Should prefer Col 1 because it has 3 face-down cards vs 1
            assertEquals(CardLocation.Tableau(1, 3), hint.from)
            assertEquals(CardLocation.Tableau(2, 1), hint.to)
        }
    }

    @Nested
    @DisplayName("Priority 2: Foundation promotions from Tableau or Waste")
    inner class Priority2FoundationPromotionTests {

        @Test
        fun `recommends foundation promotion when no face-down cards are uncovered`() {
            // Col 0: Face-up Ace of Spades at index 0 (no face-down card underneath)
            val col0 = listOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
            val state = createEmptyBoard().copy(
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.FOUNDATION_PROMOTION, hint!!.priority)
            assertEquals(CardLocation.Tableau(0, 0), hint.from)
            assertEquals(CardLocation.Foundation(0), hint.to)
        }

        @Test
        fun `recommends waste to foundation promotion`() {
            val waste = listOf(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true))
            val state = createEmptyBoard().copy(waste = waste)

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.FOUNDATION_PROMOTION, hint!!.priority)
            assertEquals(CardLocation.Waste, hint.from)
            assertTrue(hint.to is CardLocation.Foundation)
        }
    }

    @Nested
    @DisplayName("Priority 3: Productive Tableau sequence reorganization")
    inner class Priority3TableauProgressTests {

        @Test
        fun `recommends waste to tableau placement when no foundation moves exist`() {
            // Col 0: Face-up Black King (cannot go to empty foundation)
            // Waste: Face-up Red Queen
            val col0 = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
            val waste = listOf(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true))

            val state = createEmptyBoard().copy(
                waste = waste,
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.TABLEAU_PROGRESS, hint!!.priority)
            assertEquals(CardLocation.Waste, hint.from)
            assertEquals(CardLocation.Tableau(0, 1), hint.to)
        }

        @Test
        fun `recommends tableau to tableau sequence move when no face-down cards are uncovered`() {
            // Col 0: Face-up Black King, Red Queen (at index 0, 1)
            // Col 1: Face-up Black Jack (at index 0)
            // Jack can move onto Queen
            val col0 = listOf(
                Card(Suit.SPADES, Rank.KING, isFaceUp = true),
                Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
            )
            val col1 = listOf(Card(Suit.SPADES, Rank.JACK, isFaceUp = true))

            val state = createEmptyBoard().copy(
                tableau = listOf(col0, col1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.TABLEAU_PROGRESS, hint!!.priority)
            assertEquals(CardLocation.Tableau(1, 0), hint.from)
            assertEquals(CardLocation.Tableau(0, 2), hint.to)
        }

        @Test
        fun `does not recommend lateral King move between empty columns`() {
            // Col 0: Face-up King at index 0 (already at base of column)
            // Other columns empty
            val col0 = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
            val state = createEmptyBoard().copy(
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val hint = HintResolver.findHint(state)
            // King moving to another empty column is useless and pruned
            assertNull(hint)
        }
    }

    @Nested
    @DisplayName("Priority 4: Productive Stock Draw")
    inner class Priority4StockDrawTests {

        @Test
        fun `recommends drawing from stock when stock contains playable cards`() {
            // Stock has Ace of Hearts which can play to Foundation
            val stock = listOf(Card(Suit.HEARTS, Rank.ACE, isFaceUp = false))
            val state = createEmptyBoard().copy(stock = stock)

            val hint = HintResolver.findHint(state)
            assertNotNull(hint)
            assertEquals(HintPriority.STOCK_DRAW, hint!!.priority)
            assertEquals(CardLocation.Stock, hint.from)
            assertEquals(CardLocation.Waste, hint.to)
            assertEquals(1, hint.cards.size)
        }

        @Test
        fun `recommends drawing 3 cards in Draw 3 mode when stock has cards`() {
            val stock = listOf(
                Card(Suit.HEARTS, Rank.TWO, isFaceUp = false),
                Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false),
                Card(Suit.HEARTS, Rank.ACE, isFaceUp = false)
            )
            val state = createEmptyBoard().copy(stock = stock)

            val hint = HintResolver.findHint(state, drawMode = DrawMode.DRAW_THREE)
            assertNotNull(hint)
            assertEquals(HintPriority.STOCK_DRAW, hint!!.priority)
            assertEquals(CardLocation.Stock, hint.from)
            assertEquals(CardLocation.Waste, hint.to)
            assertEquals(3, hint.cards.size)
        }

        @Test
        fun `does not recommend stock draw when stock has zero playable cards for current board`() {
            // Tableau has Red 2, Foundation empty.
            // Stock has King of Spades (cannot play to empty foundation, cannot play to Red 2).
            val col0 = listOf(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
            val stock = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = false))

            // Notice tableau has no empty columns, so King cannot play!
            val state = BoardState(
                stock = stock,
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = List(7) { col0 } // All 7 columns occupied by Red 2
            )

            val hint = HintResolver.findHint(state)
            assertNull(hint)
        }

        @Test
        fun `recommends recycling waste when stock is empty and waste cycle has playable cards`() {
            val col0 = listOf(Card(Suit.SPADES, Rank.TEN, isFaceUp = true))
            val waste = listOf(Card(Suit.HEARTS, Rank.NINE, isFaceUp = true))
            // Waste has 9 of Hearts which is playable on 10 of Spades
            val state = createEmptyBoard().copy(
                stock = emptyList(),
                waste = waste,
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            // Waste top card can already be played to tableau directly (Priority 3)
            val directHint = HintResolver.findHint(state)
            assertEquals(HintPriority.TABLEAU_PROGRESS, directHint?.priority)

            // But if waste top card is unplayable, and buried waste card is playable:
            val buriedWaste = listOf(
                Card(Suit.HEARTS, Rank.NINE, isFaceUp = true), // Buried: can play on 10 of Spades
                Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)   // Top: cannot play
            )
            val buriedState = createEmptyBoard().copy(
                stock = emptyList(),
                waste = buriedWaste,
                tableau = listOf(col0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )

            val recycleHint = HintResolver.findHint(buriedState)
            assertNotNull(recycleHint)
            assertEquals(HintPriority.STOCK_DRAW, recycleHint!!.priority)
            assertEquals(CardLocation.Stock, recycleHint.from)
            assertEquals(CardLocation.Stock, recycleHint.to)
            assertEquals(2, recycleHint.cards.size)
        }
    }

    @Nested
    @DisplayName("Terminal and Deadlock Cases")
    inner class TerminalAndDeadlockTests {

        @Test
        fun `returns null when game is already won`() {
            val wonFoundations = Suit.entries.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val wonState = createEmptyBoard().copy(foundations = wonFoundations)

            val hint = HintResolver.findHint(wonState)
            assertNull(hint)
        }

        @Test
        fun `returns null on complete deadlock where no productive moves or draws exist`() {
            val state = createEmptyBoard()
            val hint = HintResolver.findHint(state)
            assertNull(hint)
        }
    }
}
