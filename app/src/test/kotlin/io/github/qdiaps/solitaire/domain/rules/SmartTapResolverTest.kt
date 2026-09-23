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

class SmartTapResolverTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val threeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val blackKing = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val redKing = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
    private val redQueen = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val blackQueen = Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true)
    private val blackJack = Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
    private val redTen = Card(Suit.DIAMONDS, Rank.TEN, isFaceUp = true)

    @Nested
    @DisplayName("Priority 1: Move to Foundation")
    inner class Priority1FoundationTests {

        @Test
        @DisplayName("Tapping Ace on waste resolves to empty foundation slot")
        fun `tapping Ace on waste resolves to empty foundation`() {
            val state = BoardState(waste = listOf(aceHearts))

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Waste)

            assertEquals(CardLocation.Foundation(0), destination)

            val applied = SmartTapResolver.resolveAndApply(state, CardLocation.Waste)
            assertNotNull(applied)
            assertEquals(listOf(aceHearts), applied!!.foundations[0])
            assertTrue(applied.waste.isEmpty())
        }

        @Test
        @DisplayName("Tapping Ace on tableau resolves to empty foundation slot")
        fun `tapping Ace on tableau resolves to empty foundation`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 3) listOf(aceHearts) else emptyList() }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(3, 0))

            assertEquals(CardLocation.Foundation(0), destination)

            val applied = SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(3, 0))
            assertNotNull(applied)
            assertEquals(listOf(aceHearts), applied!!.foundations[0])
            assertTrue(applied.tableau[3].isEmpty())
        }

        @Test
        @DisplayName("Foundation move takes priority over valid tableau move")
        fun `foundation move takes priority over valid tableau move`() {
            // twoHearts can go to Foundation 0 (on aceHearts), OR to Tableau 2 (on threeSpades)
            val threeSpades = Card(Suit.SPADES, Rank.THREE, isFaceUp = true)
            val state = BoardState(
                foundations = listOf(listOf(aceHearts), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(twoHearts)
                        2 -> listOf(threeSpades)
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 0))

            // Must choose Foundation, NOT Tableau 2
            assertEquals(CardLocation.Foundation(0), destination)
        }

        @Test
        @DisplayName("Card beneath another card cannot move to Foundation")
        fun `card beneath another card cannot move to foundation`() {
            // aceHearts is covered by twoHearts in tableau column 0
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(aceHearts, twoHearts) else emptyList() }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 0))

            // aceHearts is not the topmost card, cannot move to foundation
            assertNull(destination)
        }
    }

    @Nested
    @DisplayName("Priority 2: Move to Tableau that reveals hidden card")
    inner class Priority2RevealHiddenCardTests {

        @Test
        @DisplayName("Tableau move that reveals hidden card is resolved")
        fun `tableau move that reveals hidden card is resolved`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(hiddenCard, redQueen)
                        3 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 1))

            assertEquals(CardLocation.Tableau(3), destination)

            val applied = SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(0, 1))
            assertNotNull(applied)
            assertEquals(listOf(blackKing, redQueen), applied!!.tableau[3])
            assertTrue(applied.tableau[0].last().isFaceUp)
            assertEquals(5, applied.score) // turnover bonus
        }

        @Test
        @DisplayName("Moving King with hidden card beneath to empty column is resolved")
        fun `moving King with hidden card beneath to empty column is resolved`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.TWO, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        1 -> listOf(hiddenCard, blackKing)
                        else -> emptyList()
                    }
                }
            )

            // Column 0 is the first empty column
            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(1, 1))

            assertEquals(CardLocation.Tableau(0), destination)

            val applied = SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(1, 1))
            assertNotNull(applied)
            assertEquals(listOf(blackKing), applied!!.tableau[0])
            assertTrue(applied.tableau[1].last().isFaceUp)
        }

        @Test
        @DisplayName("Sub-stack move that reveals hidden card is resolved")
        fun `sub-stack move that reveals hidden card is resolved`() {
            val hiddenCard = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(hiddenCard, redQueen, blackJack)
                        2 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 1))

            assertEquals(CardLocation.Tableau(2), destination)

            val applied = SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(0, 1))
            assertNotNull(applied)
            assertEquals(listOf(blackKing, redQueen, blackJack), applied!!.tableau[2])
            assertTrue(applied.tableau[0].last().isFaceUp)
        }
    }

    @Nested
    @DisplayName("Priority 3: Leftmost valid tableau column")
    inner class Priority3LeftmostTableauTests {

        @Test
        @DisplayName("Picks first valid tableau column from left to right when multiple targets match")
        fun `picks leftmost valid tableau column when multiple targets match`() {
            val kingClubs = Card(Suit.CLUBS, Rank.KING, isFaceUp = true)
            val state = BoardState(
                waste = listOf(redQueen),
                tableau = List(7) { col ->
                    when (col) {
                        1 -> listOf(blackKing) // matches redQueen
                        4 -> listOf(kingClubs)  // also matches redQueen
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Waste)

            // Must pick column 1 (leftmost), not column 4
            assertEquals(CardLocation.Tableau(1), destination)
        }

        @Test
        @DisplayName("Tableau move without hidden card beneath moves to leftmost valid column")
        fun `tableau move without hidden card beneath moves to leftmost column`() {
            val kingClubs = Card(Suit.CLUBS, Rank.KING, isFaceUp = true)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(redKing, blackQueen) // both face-up, no hidden card beneath blackQueen
                        2 -> listOf(kingClubs) // blackQueen cannot go here (same color)
                        5 -> listOf(redKing)   // blackQueen can go here!
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 1))

            assertEquals(CardLocation.Tableau(5), destination)
        }

        @Test
        @DisplayName("Waste King moves to first empty tableau column")
        fun `waste King moves to first empty tableau column`() {
            val state = BoardState(
                waste = listOf(blackKing),
                tableau = List(7) { col ->
                    if (col == 0 || col == 1) listOf(redKing) else emptyList()
                }
            )

            // Columns 2..6 are empty, column 2 is the leftmost empty column
            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Waste)

            assertEquals(CardLocation.Tableau(2), destination)
        }
    }

    @Nested
    @DisplayName("Prevent pointless / invalid moves")
    inner class PreventInvalidMovesTests {

        @Test
        @DisplayName("King already at base of column does NOT move to another empty column")
        fun `King already at base of column does not move to empty column`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(blackKing) else emptyList()
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 0))

            // Pointless lateral move: moving King from empty base to another empty column
            assertNull(destination)
            assertNull(SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(0, 0)))
        }

        @Test
        @DisplayName("King stack already at base of column does NOT move to another empty column")
        fun `King stack already at base of column does not move to empty column`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(blackKing, redQueen, blackJack) else emptyList()
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 0))

            assertNull(destination)
        }

        @Test
        @DisplayName("Tapping face-down card returns null")
        fun `tapping face-down card returns null`() {
            val hiddenCard = Card(Suit.SPADES, Rank.KING, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(hiddenCard) else emptyList() }
            )

            assertNull(SmartTapResolver.resolveDestination(state, CardLocation.Tableau(0, 0)))
            assertNull(SmartTapResolver.resolveAndApply(state, CardLocation.Tableau(0, 0)))
        }

        @Test
        @DisplayName("Tapping empty waste returns null")
        fun `tapping empty waste returns null`() {
            val state = BoardState(waste = emptyList())

            assertNull(SmartTapResolver.resolveDestination(state, CardLocation.Waste))
        }

        @Test
        @DisplayName("Tapping Foundation returns null")
        fun `tapping Foundation returns null`() {
            val state = BoardState(
                foundations = listOf(listOf(aceHearts), emptyList(), emptyList(), emptyList())
            )

            assertNull(SmartTapResolver.resolveDestination(state, CardLocation.Foundation(0)))
        }

        @Test
        @DisplayName("Tapping Stock returns null")
        fun `tapping Stock returns null`() {
            val state = BoardState(stock = listOf(aceHearts))

            assertNull(SmartTapResolver.resolveDestination(state, CardLocation.Stock))
        }

        @Test
        @DisplayName("Card with no legal destination returns null")
        fun `card with no legal destination returns null`() {
            val state = BoardState(
                waste = listOf(redTen),
                foundations = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    if (col == 0) listOf(redKing) else emptyList() // redTen cannot go on redKing or empty column
                }
            )

            assertNull(SmartTapResolver.resolveDestination(state, CardLocation.Waste))
            assertNull(SmartTapResolver.resolveAndApply(state, CardLocation.Waste))
        }
    }

    @Nested
    @DisplayName("Card lookup overloads")
    inner class CardLookupOverloadTests {

        @Test
        @DisplayName("resolveDestination by Card instance finds card in waste")
        fun `resolveDestination by Card finds card in waste`() {
            val state = BoardState(waste = listOf(aceHearts))

            val destination = SmartTapResolver.resolveDestination(state, aceHearts)

            assertEquals(CardLocation.Foundation(0), destination)
        }

        @Test
        @DisplayName("resolveDestination by Card instance finds card in tableau")
        fun `resolveDestination by Card finds card in tableau`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            val destination = SmartTapResolver.resolveDestination(state, redQueen)

            assertEquals(CardLocation.Tableau(1), destination)
        }

        @Test
        @DisplayName("resolveDestination returns null if card is not found on board")
        fun `resolveDestination returns null if card not on board`() {
            val state = BoardState()

            assertNull(SmartTapResolver.resolveDestination(state, aceHearts))
        }
    }

    @Nested
    @DisplayName("Global findBestMove")
    inner class FindBestMoveTests {

        @Test
        @DisplayName("findBestMove chooses Foundation move over Tableau move")
        fun `findBestMove chooses Foundation over Tableau`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(Card(Suit.CLUBS, Rank.TWO, isFaceUp = false), redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            val bestMove = SmartTapResolver.findBestMove(state)

            assertNotNull(bestMove)
            assertEquals(CardLocation.Waste, bestMove!!.from)
            assertEquals(CardLocation.Foundation(0), bestMove.to)
        }

        @Test
        @DisplayName("findBestMove chooses move that reveals hidden card over lateral tableau move")
        fun `findBestMove chooses move that reveals hidden card over lateral move`() {
            val hiddenCard = Card(Suit.SPADES, Rank.SEVEN, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        // Col 0 has no hidden card below redQueen
                        0 -> listOf(redKing, blackQueen)
                        // Col 2 has a hidden card below blackJack
                        2 -> listOf(hiddenCard, blackJack)
                        // Col 4 can receive redQueen
                        4 -> listOf(blackKing)
                        // Col 5 can receive blackJack
                        5 -> listOf(redQueen)
                        else -> emptyList()
                    }
                }
            )

            val bestMove = SmartTapResolver.findBestMove(state)

            assertNotNull(bestMove)
            // Should choose col 2 moving blackJack because it reveals hiddenCard
            assertEquals(CardLocation.Tableau(2, 1), bestMove!!.from)
            assertEquals(CardLocation.Tableau(5), bestMove.to)
        }

        @Test
        @DisplayName("findBestMove returns null when no moves are possible")
        fun `findBestMove returns null when no moves possible`() {
            val state = BoardState()

            assertNull(SmartTapResolver.findBestMove(state))
        }
    }
}
