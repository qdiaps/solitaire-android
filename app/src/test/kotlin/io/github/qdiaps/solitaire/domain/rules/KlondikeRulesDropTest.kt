package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlondikeRulesDropTest {

    private val blackKing = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val redKing = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
    private val redQueen = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val blackQueen = Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true)
    private val blackJack = Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
    private val redTen = Card(Suit.HEARTS, Rank.TEN, isFaceUp = true)
    private val redAce = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val redTwo = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val blackAce = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)

    @Nested
    @DisplayName("canMoveCards from Waste")
    inner class WasteDropValidationTests {

        @Test
        @DisplayName("Valid waste to tableau move returns true")
        fun `canMoveCards waste to tableau valid move returns true`() {
            val state = BoardState(
                waste = listOf(redQueen),
                tableau = List(7) { col -> if (col == 0) listOf(blackKing) else emptyList() }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Valid King from waste to empty tableau column returns true")
        fun `canMoveCards King from waste to empty tableau returns true`() {
            val state = BoardState(
                waste = listOf(redKing),
                tableau = List(7) { emptyList() }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redKing),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(3)
                )
            )
        }

        @Test
        @DisplayName("Invalid waste to tableau move returns false")
        fun `canMoveCards waste to tableau invalid move returns false`() {
            val state = BoardState(
                waste = listOf(blackQueen),
                tableau = List(7) { col -> if (col == 0) listOf(blackKing) else emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(blackQueen),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Valid waste to foundation Ace returns true")
        fun `canMoveCards waste to foundation Ace returns true`() {
            val state = BoardState(
                waste = listOf(redAce),
                foundations = List(4) { emptyList() }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redAce),
                    source = CardLocation.Waste,
                    target = CardLocation.Foundation(0)
                )
            )
        }

        @Test
        @DisplayName("Waste drop with card not matching top waste returns false")
        fun `canMoveCards waste drop with non top card returns false`() {
            val state = BoardState(
                waste = listOf(redAce, blackKing),
                tableau = List(7) { emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redAce),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Waste drop with multiple cards returns false")
        fun `canMoveCards waste drop with multiple cards returns false`() {
            val state = BoardState(
                waste = listOf(redQueen, blackKing),
                tableau = List(7) { emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen, blackKing),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
        }
    }

    @Nested
    @DisplayName("canMoveCards from Tableau")
    inner class TableauDropValidationTests {

        @Test
        @DisplayName("Valid single card between tableau columns returns true")
        fun `canMoveCards tableau single card valid sequence returns true`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(blackKing)
                        1 -> listOf(redQueen)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen),
                    source = CardLocation.Tableau(1),
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Valid multi-card stack between tableau columns returns true")
        fun `canMoveCards tableau multi card stack returns true`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(blackKing)
                        1 -> listOf(hiddenCard, redQueen, blackJack, redTen)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen, blackJack, redTen),
                    source = CardLocation.Tableau(1),
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Tableau move to same column returns false")
        fun `canMoveCards tableau move to same column returns false`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(blackKing, redQueen) else emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen),
                    source = CardLocation.Tableau(0),
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Tableau single top card to foundation returns true")
        fun `canMoveCards tableau top Ace to foundation returns true`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 2) listOf(redAce) else emptyList() },
                foundations = List(4) { emptyList() }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redAce),
                    source = CardLocation.Tableau(2),
                    target = CardLocation.Foundation(1)
                )
            )
        }

        @Test
        @DisplayName("Tableau multi card stack to foundation returns false")
        fun `canMoveCards tableau multi card to foundation returns false`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 2) listOf(redAce, redTwo) else emptyList() },
                foundations = List(4) { emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redAce, redTwo),
                    source = CardLocation.Tableau(2),
                    target = CardLocation.Foundation(0)
                )
            )
        }

        @Test
        @DisplayName("Tableau move with cards not matching column tail returns false")
        fun `canMoveCards tableau cards not matching column tail returns false`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 1) listOf(redQueen, blackJack) else emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen), // Not the tail of column 1
                    source = CardLocation.Tableau(1),
                    target = CardLocation.Tableau(0)
                )
            )
        }
    }

    @Nested
    @DisplayName("canMoveCards from Foundation")
    inner class FoundationDropValidationTests {

        @Test
        @DisplayName("Foundation top card can be moved back to tableau")
        fun `canMoveCards foundation top card to tableau returns true`() {
            val state = BoardState(
                foundations = listOf(listOf(redAce, redTwo), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    if (col == 4) listOf(Card(Suit.CLUBS, Rank.THREE, isFaceUp = true)) else emptyList()
                }
            )

            assertTrue(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redTwo),
                    source = CardLocation.Foundation(0),
                    target = CardLocation.Tableau(4)
                )
            )
        }

        @Test
        @DisplayName("Foundation card cannot be moved to another foundation")
        fun `canMoveCards foundation to foundation returns false`() {
            val state = BoardState(
                foundations = listOf(listOf(redAce), emptyList(), emptyList(), emptyList())
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redAce),
                    source = CardLocation.Foundation(0),
                    target = CardLocation.Foundation(1)
                )
            )
        }
    }

    @Nested
    @DisplayName("canMoveCards illegal sources, targets and boundaries")
    inner class IllegalSourcesAndTargetsTests {

        @Test
        @DisplayName("Stock cannot be dragged")
        fun `canMoveCards stock source returns false`() {
            val state = BoardState(stock = listOf(blackAce))

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(blackAce),
                    source = CardLocation.Stock,
                    target = CardLocation.Tableau(0)
                )
            )
        }

        @Test
        @DisplayName("Cards cannot be dropped on Stock or Waste")
        fun `canMoveCards dropping on Stock or Waste returns false`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(redQueen) else emptyList() }
            )

            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen),
                    source = CardLocation.Tableau(0),
                    target = CardLocation.Stock
                )
            )
            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = listOf(redQueen),
                    source = CardLocation.Tableau(0),
                    target = CardLocation.Waste
                )
            )
        }

        @Test
        @DisplayName("Empty cards list returns false")
        fun `canMoveCards empty cards list returns false`() {
            val state = BoardState()
            assertFalse(
                KlondikeRules.canMoveCards(
                    state = state,
                    cards = emptyList(),
                    source = CardLocation.Tableau(0),
                    target = CardLocation.Tableau(1)
                )
            )
        }
    }

    @Nested
    @DisplayName("moveCards execution")
    inner class MoveCardsExecutionTests {

        @Test
        @DisplayName("moveCards waste to tableau applies move and scores 5 points")
        fun `moveCards waste to tableau applies move correctly`() {
            val state = BoardState(
                waste = listOf(redQueen),
                tableau = List(7) { col -> if (col == 0) listOf(blackKing) else emptyList() }
            )

            val next = KlondikeRules.moveCards(
                state = state,
                cards = listOf(redQueen),
                source = CardLocation.Waste,
                target = CardLocation.Tableau(0)
            )

            assertTrue(next.waste.isEmpty())
            assertEquals(listOf(blackKing, redQueen), next.tableau[0])
            assertEquals(5, next.score)
            assertEquals(1, next.movesCount)
        }

        @Test
        @DisplayName("moveCards tableau to tableau moves stack and auto-exposes top hidden card")
        fun `moveCards tableau to tableau moves stack and exposes hidden card`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(blackKing)
                        1 -> listOf(hiddenCard, redQueen, blackJack)
                        else -> emptyList()
                    }
                }
            )

            val next = KlondikeRules.moveCards(
                state = state,
                cards = listOf(redQueen, blackJack),
                source = CardLocation.Tableau(1),
                target = CardLocation.Tableau(0),
                autoExpose = true
            )

            assertEquals(listOf(hiddenCard.copy(isFaceUp = true)), next.tableau[1])
            assertEquals(listOf(blackKing, redQueen, blackJack), next.tableau[0])
            assertEquals(5, next.score)
            assertEquals(1, next.movesCount)
        }

        @Test
        @DisplayName("moveCards tableau to foundation scores 10 points")
        fun `moveCards tableau to foundation scores 10 points`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(redAce) else emptyList() },
                foundations = List(4) { emptyList() }
            )

            val next = KlondikeRules.moveCards(
                state = state,
                cards = listOf(redAce),
                source = CardLocation.Tableau(0),
                target = CardLocation.Foundation(2)
            )

            assertTrue(next.tableau[0].isEmpty())
            assertEquals(listOf(redAce), next.foundations[2])
            assertEquals(10, next.score)
            assertEquals(1, next.movesCount)
        }

        @Test
        @DisplayName("moveCards foundation to tableau deducts 15 points")
        fun `moveCards foundation to tableau deducts 15 points`() {
            val state = BoardState(
                foundations = listOf(listOf(redAce, redTwo), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    if (col == 4) listOf(Card(Suit.CLUBS, Rank.THREE, isFaceUp = true)) else emptyList()
                },
                score = 30
            )

            val next = KlondikeRules.moveCards(
                state = state,
                cards = listOf(redTwo),
                source = CardLocation.Foundation(0),
                target = CardLocation.Tableau(4)
            )

            assertEquals(listOf(redAce), next.foundations[0])
            assertEquals(2, next.tableau[4].size)
            assertEquals(15, next.score)
            assertEquals(1, next.movesCount)
        }

        @Test
        @DisplayName("moveCards with invalid move throws IllegalArgumentException")
        fun `moveCards with invalid move throws IllegalArgumentException`() {
            val state = BoardState(
                waste = listOf(blackQueen),
                tableau = List(7) { col -> if (col == 0) listOf(blackKing) else emptyList() }
            )

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveCards(
                    state = state,
                    cards = listOf(blackQueen),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            }
        }
    }
}
