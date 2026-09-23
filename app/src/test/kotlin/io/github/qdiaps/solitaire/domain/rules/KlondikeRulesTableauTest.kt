package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlondikeRulesTableauTest {

    private val blackKing = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val redKing = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
    private val redQueen = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val blackQueen = Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true)
    private val blackJack = Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
    private val redJack = Card(Suit.DIAMONDS, Rank.JACK, isFaceUp = true)
    private val redTen = Card(Suit.HEARTS, Rank.TEN, isFaceUp = true)
    private val blackTen = Card(Suit.CLUBS, Rank.TEN, isFaceUp = true)
    private val blackNine = Card(Suit.SPADES, Rank.NINE, isFaceUp = true)
    private val redAce = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)

    @Nested
    @DisplayName("canPlaceOnTableau validation")
    inner class CanPlaceOnTableauTests {

        @Test
        @DisplayName("Only King can be placed on an empty tableau column")
        fun `only King can be placed on empty tableau column`() {
            assertTrue(KlondikeRules.canPlaceOnTableau(blackKing, emptyList<Card>()))
            assertTrue(KlondikeRules.canPlaceOnTableau(redKing, emptyList<Card>()))

            assertFalse(KlondikeRules.canPlaceOnTableau(redQueen, emptyList<Card>()))
            assertFalse(KlondikeRules.canPlaceOnTableau(blackJack, emptyList<Card>()))
            assertFalse(KlondikeRules.canPlaceOnTableau(redAce, emptyList<Card>()))
        }

        @Test
        @DisplayName("Card can be placed if rank is one lower and color is opposite")
        fun `card can be placed when rank is one lower and color is opposite`() {
            val targetColumn = listOf(blackKing)

            assertTrue(KlondikeRules.canPlaceOnTableau(redQueen, targetColumn))
            assertFalse(KlondikeRules.canPlaceOnTableau(blackQueen, targetColumn), "Same color Queen should be rejected")
            assertFalse(KlondikeRules.canPlaceOnTableau(redJack, targetColumn), "Two ranks lower should be rejected")
            assertFalse(KlondikeRules.canPlaceOnTableau(redKing, targetColumn), "Same rank should be rejected")
        }

        @Test
        @DisplayName("Cannot place card on a face-down top card")
        fun `cannot place card on face-down top card`() {
            val faceDownKing = blackKing.copy(isFaceUp = false)
            val targetColumn = listOf(faceDownKing)

            assertFalse(KlondikeRules.canPlaceOnTableau(redQueen, targetColumn))
        }

        @Test
        @DisplayName("Placement checks against the topmost (last) card of the column")
        fun `placement checks against topmost card of column`() {
            val column = listOf(
                Card(Suit.SPADES, Rank.KING, isFaceUp = false),
                Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
                blackJack
            )

            assertTrue(KlondikeRules.canPlaceOnTableau(redTen, column))
            assertFalse(KlondikeRules.canPlaceOnTableau(blackTen, column))
            assertFalse(KlondikeRules.canPlaceOnTableau(redQueen, column))
        }
    }

    @Nested
    @DisplayName("isValidTableauSequence validation")
    inner class IsValidTableauSequenceTests {

        @Test
        @DisplayName("Single face-up card is a valid sequence")
        fun `single face-up card is a valid sequence`() {
            assertTrue(KlondikeRules.isValidTableauSequence(listOf(redQueen)))
            assertTrue(KlondikeRules.isValidTableauSequence(listOf(blackKing)))
        }

        @Test
        @DisplayName("Single face-down card is NOT a valid sequence")
        fun `single face-down card is invalid sequence`() {
            assertFalse(KlondikeRules.isValidTableauSequence(listOf(redQueen.copy(isFaceUp = false))))
        }

        @Test
        @DisplayName("Empty list is NOT a valid sequence")
        fun `empty list is invalid sequence`() {
            assertFalse(KlondikeRules.isValidTableauSequence(emptyList<Card>()))
        }

        @Test
        @DisplayName("Valid alternating descending multi-card sequence")
        fun `valid alternating descending multi-card sequence returns true`() {
            val sequence = listOf(blackKing, redQueen, blackJack, redTen, blackNine)
            assertTrue(KlondikeRules.isValidTableauSequence(sequence))
        }

        @Test
        @DisplayName("Sequence with any face-down card is invalid")
        fun `sequence with any face-down card is invalid`() {
            val sequence = listOf(blackKing, redQueen.copy(isFaceUp = false), blackJack)
            assertFalse(KlondikeRules.isValidTableauSequence(sequence))
        }

        @Test
        @DisplayName("Sequence with same color adjacent cards is invalid")
        fun `sequence with same color adjacent cards is invalid`() {
            val sequence = listOf(blackKing, blackQueen, blackJack)
            assertFalse(KlondikeRules.isValidTableauSequence(sequence))
        }

        @Test
        @DisplayName("Sequence with skipped rank is invalid")
        fun `sequence with skipped rank is invalid`() {
            val sequence = listOf(blackKing, redJack)
            assertFalse(KlondikeRules.isValidTableauSequence(sequence))
        }

        @Test
        @DisplayName("Sequence with ascending rank is invalid")
        fun `sequence with ascending rank is invalid`() {
            val sequence = listOf(redQueen, blackKing)
            assertFalse(KlondikeRules.isValidTableauSequence(sequence))
        }
    }

    @Nested
    @DisplayName("Waste to Tableau movement")
    inner class WasteToTableauTests {

        @Test
        @DisplayName("Cannot move from empty waste")
        fun `cannot move from empty waste`() {
            val state = BoardState(waste = emptyList())
            assertFalse(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 0))

            val ex = assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 0)
            }
            assertTrue(ex.message!!.contains("empty waste", ignoreCase = true))
        }

        @Test
        @DisplayName("Cannot move to invalid column index")
        fun `cannot move to invalid column index`() {
            val state = BoardState(waste = listOf(blackKing))
            assertFalse(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = -1))
            assertFalse(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 7))

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveWasteToTableau(state, targetColumnIndex = -1)
            }
            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 7)
            }
        }

        @Test
        @DisplayName("Move King from waste to empty tableau column succeeds")
        fun `move King from waste to empty column updates state correctly`() {
            val state = BoardState(
                waste = listOf(redAce, blackKing),
                tableau = List(7) { emptyList() },
                movesCount = 3
            )

            assertTrue(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 0))

            val newState = KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 0)

            assertEquals(listOf(redAce), newState.waste)
            assertEquals(listOf(blackKing), newState.tableau[0])
            assertEquals(4, newState.movesCount)
            assertNotSame(state, newState)
        }

        @Test
        @DisplayName("Move valid card from waste to non-empty tableau column succeeds")
        fun `move valid card from waste to non-empty tableau column succeeds`() {
            val state = BoardState(
                waste = listOf(redQueen),
                tableau = List(7) { colIndex ->
                    if (colIndex == 2) listOf(blackKing) else emptyList()
                },
                movesCount = 0
            )

            assertTrue(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 2))

            val newState = KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 2)

            assertTrue(newState.waste.isEmpty())
            assertEquals(listOf(blackKing, redQueen), newState.tableau[2])
            assertEquals(1, newState.movesCount)
        }

        @Test
        @DisplayName("Move invalid card from waste to tableau fails")
        fun `move invalid card from waste throws IllegalStateException`() {
            val state = BoardState(
                waste = listOf(blackQueen),
                tableau = List(7) { colIndex ->
                    if (colIndex == 0) listOf(blackKing) else emptyList()
                }
            )

            assertFalse(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 0))
            assertFalse(KlondikeRules.canMoveWasteToTableau(state, targetColumnIndex = 1))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 0)
            }
        }
    }

    @Nested
    @DisplayName("Tableau to Tableau movement")
    inner class TableauToTableauTests {

        @Test
        @DisplayName("Cannot move between same column")
        fun `cannot move to same column`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    if (colIndex == 0) listOf(blackKing, redQueen) else emptyList()
                }
            )

            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 0))
            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 0)
            }
        }

        @Test
        @DisplayName("Cannot move with invalid column indices or card index")
        fun `invalid column or card indices return false and throw on move`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    if (colIndex == 0) listOf(blackKing) else emptyList()
                }
            )

            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = -1, cardIndex = 0, toColumnIndex = 1))
            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 7))
            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 5, toColumnIndex = 1))
            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = -1, toColumnIndex = 1))

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveTableauToTableau(state, fromColumnIndex = -1, cardIndex = 0, toColumnIndex = 1)
            }
        }

        @Test
        @DisplayName("Cannot move face-down card from tableau")
        fun `cannot move face-down card from tableau`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(redQueen.copy(isFaceUp = false))
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 1))
            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 1)
            }
        }

        @Test
        @DisplayName("Move single card between tableau columns successfully")
        fun `move single card between tableau columns updates state correctly`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(hiddenCard, redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                },
                movesCount = 5
            )

            assertTrue(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 1))

            val newState = KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 1)

            assertEquals(listOf(hiddenCard), newState.tableau[0])
            assertEquals(listOf(blackKing, redQueen), newState.tableau[1])
            assertEquals(6, newState.movesCount)
        }

        @Test
        @DisplayName("Move valid multi-card stack between tableau columns")
        fun `move valid multi-card stack between tableau columns succeeds`() {
            val hiddenCard = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(hiddenCard, redQueen, blackJack, redTen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 1))

            val newState = KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 1, toColumnIndex = 1)

            assertEquals(listOf(hiddenCard), newState.tableau[0])
            assertEquals(listOf(blackKing, redQueen, blackJack, redTen), newState.tableau[1])
            assertEquals(1, newState.movesCount)
        }

        @Test
        @DisplayName("Move King stack to empty column successfully")
        fun `move King stack to empty column succeeds`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(blackKing, redQueen, blackJack)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 3))

            val newState = KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 3)

            assertTrue(newState.tableau[0].isEmpty())
            assertEquals(listOf(blackKing, redQueen, blackJack), newState.tableau[3])
            assertEquals(1, newState.movesCount)
        }

        @Test
        @DisplayName("Overload with Card instance moves correctly")
        fun `overload with Card instance finds card and moves correctly`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            assertTrue(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, card = redQueen, toColumnIndex = 1))

            val newState = KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, card = redQueen, toColumnIndex = 1)

            assertTrue(newState.tableau[0].isEmpty())
            assertEquals(listOf(blackKing, redQueen), newState.tableau[1])
        }

        @Test
        @DisplayName("Move fails if stack contains an invalid internal sequence")
        fun `move fails if stack contains invalid internal sequence`() {
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    when (colIndex) {
                        0 -> listOf(redQueen, redJack)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                }
            )

            assertFalse(KlondikeRules.canMoveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 1))
            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveTableauToTableau(state, fromColumnIndex = 0, cardIndex = 0, toColumnIndex = 1)
            }
        }
    }
}
