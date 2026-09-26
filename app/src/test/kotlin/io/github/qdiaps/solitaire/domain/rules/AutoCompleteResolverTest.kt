package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AutoCompleteResolverTest {

    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
    private val twoSpades = Card(Suit.SPADES, Rank.TWO, isFaceUp = true)
    private val threeSpades = Card(Suit.SPADES, Rank.THREE, isFaceUp = true)
    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
    private val twoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)
    private val aceDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val twoDiamonds = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true)

    @Nested
    @DisplayName("isAutoCompleteReady readiness checks")
    inner class ReadinessChecks {

        @Test
        @DisplayName("Returns false when game is already won")
        fun `returns false when game is already won`() {
            val wonFoundations = Suit.entries.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val state = BoardState(foundations = wonFoundations)
            assertTrue(KlondikeRules.isGameWon(state))

            assertFalse(AutoCompleteResolver.isAutoCompleteReady(state))
            assertFalse(AutoCompleteResolver.canAutoComplete(state))
        }

        @Test
        @DisplayName("Returns false when board is completely empty")
        fun `returns false when board is empty`() {
            val state = BoardState()

            assertFalse(AutoCompleteResolver.isAutoCompleteReady(state))
            assertFalse(AutoCompleteResolver.canAutoComplete(state))
        }

        @Test
        @DisplayName("Returns true when all tableau cards are face-up even if stock is not empty")
        fun `returns true when all tableau cards are face-up even if stock is not empty`() {
            val state = BoardState(
                stock = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = false)),
                tableau = List(7) { col -> if (col == 0) listOf(aceSpades) else emptyList() }
            )

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))
        }

        @Test
        @DisplayName("Returns false when any tableau card is face down")
        fun `returns false when any tableau card is face down`() {
            val hiddenCard = Card(Suit.SPADES, Rank.KING, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(hiddenCard, aceSpades) else emptyList()
                }
            )

            assertFalse(AutoCompleteResolver.isAutoCompleteReady(state))
        }

        @Test
        @DisplayName("Returns true when all tableau cards are face-up and stock and waste are empty")
        fun `returns true when all tableau cards are face-up and stock and waste are empty`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(aceSpades)
                        1 -> listOf(aceHearts)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))
            assertTrue(AutoCompleteResolver.canAutoComplete(state))
        }

        @Test
        @DisplayName("Returns true when stock is empty and waste contains playable card")
        fun `returns true when stock is empty and waste has playable card`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                tableau = List(7) { col -> if (col == 0) listOf(aceSpades) else emptyList() }
            )

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))
        }

        @Test
        @DisplayName("Returns true and resolves cards from waste even if multiple cards in waste")
        fun `returns true and resolves cards from waste even if multiple cards in waste`() {
            val fourSpades = Card(Suit.SPADES, Rank.FOUR, isFaceUp = true)
            val state = BoardState(
                waste = listOf(threeSpades, fourSpades),
                foundations = listOf(listOf(aceSpades, twoSpades), emptyList(), emptyList(), emptyList())
            )

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))
        }
    }

    @Nested
    @DisplayName("nextMove step-by-step foundation promotion generation")
    inner class NextMoveTests {

        @Test
        @DisplayName("Promotes Ace from tableau to empty foundation")
        fun `promotes Ace from tableau to empty foundation`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 2) listOf(aceHearts) else emptyList() }
            )

            val move = AutoCompleteResolver.nextMove(state)

            assertNotNull(move)
            assertEquals(aceHearts, move!!.card)
            assertEquals(CardLocation.Tableau(2, 0), move.from)
            assertEquals(CardLocation.Foundation(0), move.to)
            assertEquals(listOf(aceHearts), move.resultingState.foundations[0])
            assertTrue(move.resultingState.tableau[2].isEmpty())
            assertEquals(10, move.resultingState.score)
            assertEquals(1, move.resultingState.movesCount)
        }

        @Test
        @DisplayName("Promotes Ace from waste to empty foundation")
        fun `promotes Ace from waste to empty foundation`() {
            val state = BoardState(
                waste = listOf(aceSpades)
            )

            val move = AutoCompleteResolver.nextMove(state)

            assertNotNull(move)
            assertEquals(aceSpades, move!!.card)
            assertEquals(CardLocation.Waste, move.from)
            assertEquals(CardLocation.Foundation(0), move.to)
            assertEquals(listOf(aceSpades), move.resultingState.foundations[0])
            assertTrue(move.resultingState.waste.isEmpty())
            assertEquals(10, move.resultingState.score)
        }

        @Test
        @DisplayName("Promotes lower ranks first across different columns")
        fun `promotes lower ranks first across columns`() {
            // Column 0 has twoSpades (foundation 0 has aceSpades), Column 1 has aceHearts
            val state = BoardState(
                foundations = listOf(listOf(aceSpades), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(twoSpades)
                        1 -> listOf(aceHearts)
                        else -> emptyList()
                    }
                }
            )

            // Ace (rank 1) should be promoted before Two (rank 2)
            val move = AutoCompleteResolver.nextMove(state)

            assertNotNull(move)
            assertEquals(aceHearts, move!!.card)
            assertEquals(CardLocation.Tableau(1, 0), move.from)
        }

        @Test
        @DisplayName("Promotes sequential cards in same column")
        fun `promotes sequential cards in same column`() {
            // Column 0 has [twoSpades, aceSpades], where aceSpades is at the top (index 1)
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(twoSpades, aceSpades) else emptyList()
                }
            )

            // Step 1: Promotes aceSpades
            val step1 = AutoCompleteResolver.nextMove(state)
            assertNotNull(step1)
            assertEquals(aceSpades, step1!!.card)
            assertEquals(CardLocation.Tableau(0, 1), step1.from)

            // Step 2: Now twoSpades is top of column 0 and can promote onto aceSpades
            val step2 = AutoCompleteResolver.nextMove(step1.resultingState)
            assertNotNull(step2)
            assertEquals(twoSpades, step2!!.card)
            assertEquals(CardLocation.Tableau(0, 0), step2.from)
            assertEquals(listOf(aceSpades, twoSpades), step2.resultingState.foundations[0])
            assertTrue(step2.resultingState.tableau[0].isEmpty())
        }

        @Test
        @DisplayName("Returns null when no foundation promotions are available")
        fun `returns null when no foundation moves available`() {
            // Only threeSpades is available, but foundations are empty
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(threeSpades) else emptyList() }
            )

            assertNull(AutoCompleteResolver.nextMove(state))
        }
    }

    @Nested
    @DisplayName("resolveAllMoves end-to-end cascade")
    inner class ResolveAllMovesTests {

        @Test
        @DisplayName("Resolves full 52-card cascade to victory")
        fun `resolves full 52-card cascade to victory`() {
            // Distribute all 52 cards across tableau columns in descending order, all face up
            // Col 0: Kings..Aces of Spades (13 cards)
            // Col 1: Kings..Aces of Hearts (13 cards)
            // Col 2: Kings..Aces of Clubs (13 cards)
            // Col 3: Kings..Aces of Diamonds (13 cards)
            val suits = listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
            val tableau = List(7) { col ->
                if (col < 4) {
                    val suit = suits[col]
                    // Descending from King down to Ace so Ace is at the top (last)
                    Rank.entries.reversed().map { rank -> Card(suit, rank, isFaceUp = true) }
                } else {
                    emptyList()
                }
            }
            val state = BoardState(tableau = tableau)

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))

            val allMoves = AutoCompleteResolver.resolveAllMoves(state)
            assertEquals(52, allMoves.size)

            val finalState = allMoves.last().resultingState
            assertTrue(KlondikeRules.isGameWon(finalState))
            assertTrue(finalState.tableau.all { it.isEmpty() })
            assertEquals(520, finalState.score) // 52 * 10
            assertEquals(52, finalState.movesCount)
        }

        @Test
        @DisplayName("Resolves partial board to victory")
        fun `resolves partial board to victory`() {
            // All suits already up to Queen on foundations; only 4 Kings remain on tableau
            val foundations = Suit.entries.map { suit ->
                Rank.entries.dropLast(1).map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val kings = Suit.entries.map { suit -> Card(suit, Rank.KING, isFaceUp = true) }
            val state = BoardState(
                foundations = foundations,
                tableau = List(7) { col -> if (col < 4) listOf(kings[col]) else emptyList() }
            )

            assertTrue(AutoCompleteResolver.isAutoCompleteReady(state))

            val allMoves = AutoCompleteResolver.resolveAllMoves(state)
            assertEquals(4, allMoves.size)
            assertTrue(KlondikeRules.isGameWon(allMoves.last().resultingState))
        }

        @Test
        @DisplayName("Resolves mixed tableau, waste, and stock to victory")
        fun `resolves mixed tableau, waste, and stock to victory`() {
            // Ace of Spades in stock (face down), Two of Spades in waste, Three of Spades in tableau
            val state = BoardState(
                stock = listOf(aceSpades.copy(isFaceUp = false)),
                waste = listOf(twoSpades),
                tableau = List(7) { col -> if (col == 0) listOf(threeSpades) else emptyList() }
            )

            val moves = AutoCompleteResolver.resolveAllMoves(state)
            assertEquals(3, moves.size)

            assertEquals(CardLocation.Stock, moves[0].from)
            assertEquals(aceSpades, moves[0].card)

            assertEquals(CardLocation.Waste, moves[1].from)
            assertEquals(twoSpades, moves[1].card)

            assertEquals(CardLocation.Tableau(0, 0), moves[2].from)
            assertEquals(threeSpades, moves[2].card)

            assertTrue(moves[2].resultingState.stock.isEmpty())
            assertTrue(moves[2].resultingState.waste.isEmpty())
            assertTrue(moves[2].resultingState.tableau[0].isEmpty())
            assertEquals(listOf(aceSpades, twoSpades, threeSpades), moves[2].resultingState.foundations[0])
        }

        @Test
        @DisplayName("Resolves mixed tableau and waste to victory")
        fun `resolves mixed tableau and waste to victory`() {
            // Ace of Spades in waste, Two of Spades in tableau
            val state = BoardState(
                waste = listOf(aceSpades),
                tableau = List(7) { col -> if (col == 0) listOf(twoSpades) else emptyList() }
            )

            val moves = AutoCompleteResolver.resolveAllMoves(state)
            assertEquals(2, moves.size)

            assertEquals(CardLocation.Waste, moves[0].from)
            assertEquals(aceSpades, moves[0].card)

            assertEquals(CardLocation.Tableau(0, 0), moves[1].from)
            assertEquals(twoSpades, moves[1].card)

            assertTrue(moves[1].resultingState.waste.isEmpty())
            assertTrue(moves[1].resultingState.tableau[0].isEmpty())
            assertEquals(listOf(aceSpades, twoSpades), moves[1].resultingState.foundations[0])
        }

        @Test
        @DisplayName("Returns empty list when auto-complete conditions are not met due to face-down card")
        fun `returns empty list when not ready`() {
            val stateWithFaceDown = BoardState(
                stock = listOf(aceSpades),
                tableau = List(7) { col -> if (col == 0) listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = false), twoSpades) else emptyList() }
            )

            assertTrue(AutoCompleteResolver.resolveAllMoves(stateWithFaceDown).isEmpty())
        }
    }

    @Nested
    @DisplayName("AutoCompleteMove properties and aliases")
    inner class MovePropertyTests {

        @Test
        @DisplayName("Verifies source and destination aliases match from and to")
        fun `verifies source and destination aliases`() {
            val state = BoardState()
            val move = AutoCompleteMove(
                card = aceSpades,
                from = CardLocation.Waste,
                to = CardLocation.Foundation(1),
                resultingState = state
            )

            assertEquals(move.from, move.source)
            assertEquals(move.to, move.destination)
            assertEquals(aceSpades, move.card)
            assertEquals(state, move.resultingState)
        }
    }
}
