package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlondikeRulesFoundationTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val threeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val kingHearts = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)

    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
    private val twoSpades = Card(Suit.SPADES, Rank.TWO, isFaceUp = true)

    private val aceDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val twoDiamonds = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true)

    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)

    @Nested
    @DisplayName("canPlaceOnFoundation validation")
    inner class CanPlaceOnFoundationTests {

        @Test
        @DisplayName("Only Ace can be placed on an empty foundation pile")
        fun `only Ace can be placed on empty foundation pile`() {
            assertTrue(KlondikeRules.canPlaceOnFoundation(aceHearts, emptyList<Card>()))
            assertTrue(KlondikeRules.canPlaceOnFoundation(aceSpades, emptyList<Card>()))
            assertTrue(KlondikeRules.canPlaceOnFoundation(aceDiamonds, emptyList<Card>()))
            assertTrue(KlondikeRules.canPlaceOnFoundation(aceClubs, emptyList<Card>()))

            assertFalse(KlondikeRules.canPlaceOnFoundation(twoHearts, emptyList<Card>()))
            assertFalse(KlondikeRules.canPlaceOnFoundation(kingHearts, emptyList<Card>()))
        }

        @Test
        @DisplayName("Next sequential card of the same suit can be placed")
        fun `next sequential card of same suit can be placed`() {
            val foundationHearts = listOf(aceHearts)

            assertTrue(KlondikeRules.canPlaceOnFoundation(twoHearts, foundationHearts))
            assertFalse(KlondikeRules.canPlaceOnFoundation(threeHearts, foundationHearts), "Cannot skip rank")
            assertFalse(KlondikeRules.canPlaceOnFoundation(twoSpades, foundationHearts), "Different suit must be rejected")
            assertFalse(KlondikeRules.canPlaceOnFoundation(twoDiamonds, foundationHearts), "Same color different suit must be rejected")
            assertFalse(KlondikeRules.canPlaceOnFoundation(aceHearts, foundationHearts), "Same rank must be rejected")
        }

        @Test
        @DisplayName("Face-down card cannot be placed on foundation")
        fun `face-down card cannot be placed on foundation`() {
            val faceDownAce = aceHearts.copy(isFaceUp = false)
            assertFalse(KlondikeRules.canPlaceOnFoundation(faceDownAce, emptyList<Card>()))
        }

        @Test
        @DisplayName("Nothing can be placed on a completed King foundation")
        fun `nothing can be placed on completed King foundation`() {
            val fullHeartsPile = Rank.entries.map { rank -> Card(Suit.HEARTS, rank, isFaceUp = true) }
            assertEquals(kingHearts, fullHeartsPile.last())

            assertFalse(KlondikeRules.canPlaceOnFoundation(aceHearts, fullHeartsPile))
            assertFalse(KlondikeRules.canPlaceOnFoundation(twoHearts, fullHeartsPile))
        }
    }

    @Nested
    @DisplayName("findTargetFoundationIndex validation")
    inner class FindTargetFoundationIndexTests {

        @Test
        @DisplayName("Finds first empty foundation slot for an Ace")
        fun `finds first empty foundation slot for Ace`() {
            val state = BoardState(
                foundations = listOf(
                    listOf(aceHearts),
                    emptyList<Card>(),
                    emptyList<Card>(),
                    emptyList<Card>()
                )
            )

            assertEquals(1, KlondikeRules.findTargetFoundationIndex(state, aceSpades))
        }

        @Test
        @DisplayName("Finds matching foundation slot for subsequent sequential card")
        fun `finds matching foundation slot for subsequent card`() {
            val state = BoardState(
                foundations = listOf(
                    listOf(aceHearts),
                    listOf(aceSpades),
                    emptyList<Card>(),
                    emptyList<Card>()
                )
            )

            assertEquals(0, KlondikeRules.findTargetFoundationIndex(state, twoHearts))
            assertEquals(1, KlondikeRules.findTargetFoundationIndex(state, twoSpades))
            assertNull(KlondikeRules.findTargetFoundationIndex(state, threeHearts))
            assertEquals(2, KlondikeRules.findTargetFoundationIndex(state, aceDiamonds))
        }

        @Test
        @DisplayName("Returns null if no foundation can accept the card")
        fun `returns null when no foundation can accept card`() {
            val state = BoardState(
                foundations = listOf(
                    listOf(aceHearts),
                    listOf(aceSpades),
                    listOf(aceDiamonds),
                    listOf(aceClubs)
                )
            )

            assertNull(KlondikeRules.findTargetFoundationIndex(state, threeHearts))
            assertNull(KlondikeRules.findTargetFoundationIndex(state, kingHearts))
        }
    }

    @Nested
    @DisplayName("Waste to Foundation movement")
    inner class WasteToFoundationTests {

        @Test
        @DisplayName("Cannot move from empty waste")
        fun `cannot move from empty waste`() {
            val state = BoardState(waste = emptyList<Card>())
            assertFalse(KlondikeRules.canMoveWasteToFoundation(state, foundationIndex = 0))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveWasteToFoundation(state, foundationIndex = 0)
            }
        }

        @Test
        @DisplayName("Cannot move with out of bounds foundation index")
        fun `cannot move with out of bounds foundation index`() {
            val state = BoardState(waste = listOf(aceHearts))
            assertFalse(KlondikeRules.canMoveWasteToFoundation(state, foundationIndex = -1))
            assertFalse(KlondikeRules.canMoveWasteToFoundation(state, foundationIndex = 4))

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveWasteToFoundation(state, foundationIndex = -1)
            }
            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveWasteToFoundation(state, foundationIndex = 4)
            }
        }

        @Test
        @DisplayName("Move Ace from waste to empty foundation succeeds")
        fun `move Ace from waste to empty foundation updates state correctly`() {
            val state = BoardState(
                waste = listOf(twoSpades, aceHearts),
                movesCount = 5
            )

            assertTrue(KlondikeRules.canMoveWasteToFoundation(state, foundationIndex = 0))

            val newState = KlondikeRules.moveWasteToFoundation(state, foundationIndex = 0)

            assertEquals(listOf(twoSpades), newState.waste)
            assertEquals(listOf(aceHearts), newState.foundations[0])
            assertEquals(6, newState.movesCount)
            assertEquals(10, newState.score)
            assertNotSame(state, newState)
        }

        @Test
        @DisplayName("Move invalid card from waste to foundation throws IllegalStateException")
        fun `move invalid card from waste to foundation throws IllegalStateException`() {
            val state = BoardState(
                waste = listOf(twoHearts),
                foundations = listOf(emptyList<Card>(), emptyList<Card>(), emptyList<Card>(), emptyList<Card>())
            )

            assertFalse(KlondikeRules.canMoveWasteToFoundation(state, foundationIndex = 0))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveWasteToFoundation(state, foundationIndex = 0)
            }
        }
    }

    @Nested
    @DisplayName("Tableau to Foundation movement")
    inner class TableauToFoundationTests {

        @Test
        @DisplayName("Cannot move from empty tableau column")
        fun `cannot move from empty tableau column`() {
            val state = BoardState(tableau = List(7) { emptyList<Card>() })
            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)
            }
        }

        @Test
        @DisplayName("Cannot move with invalid column or foundation index")
        fun `cannot move with invalid indices`() {
            val state = BoardState(tableau = List(7) { listOf(aceHearts) })

            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = -1, foundationIndex = 0))
            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 7, foundationIndex = 0))
            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 0, foundationIndex = -1))
            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 4))

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveTableauToFoundation(state, tableauIndex = -1, foundationIndex = 0)
            }
            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 4)
            }
        }

        @Test
        @DisplayName("Move top card from tableau to foundation succeeds")
        fun `move top card from tableau to foundation updates state correctly`() {
            val hiddenCard = Card(Suit.SPADES, Rank.KING, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    if (colIndex == 0) listOf(hiddenCard, aceHearts) else emptyList<Card>()
                },
                movesCount = 10
            )

            assertTrue(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0))

            val newState = KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)

            // When aceHearts is moved, hiddenCard is auto-exposed and awards 10 + 5 points
            assertEquals(listOf(hiddenCard.copy(isFaceUp = true)), newState.tableau[0])
            assertEquals(listOf(aceHearts), newState.foundations[0])
            assertEquals(11, newState.movesCount)
            assertEquals(15, newState.score)
        }

        @Test
        @DisplayName("Cannot move face-down card to foundation")
        fun `cannot move face-down card to foundation`() {
            val faceDownAce = aceHearts.copy(isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { colIndex ->
                    if (colIndex == 0) listOf(faceDownAce) else emptyList<Card>()
                }
            )

            assertFalse(KlondikeRules.canMoveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0))
            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)
            }
        }
    }

    @Nested
    @DisplayName("Foundation to Tableau movement")
    inner class FoundationToTableauTests {

        @Test
        @DisplayName("Cannot move from empty foundation")
        fun `cannot move from empty foundation`() {
            val state = BoardState()
            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 0))

            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 0)
            }
        }

        @Test
        @DisplayName("Cannot move with invalid foundation or tableau index")
        fun `cannot move with invalid indices in foundation to tableau`() {
            val state = BoardState(
                foundations = listOf(listOf(aceHearts), emptyList<Card>(), emptyList<Card>(), emptyList<Card>())
            )

            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = -1, tableauIndex = 0))
            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 4, tableauIndex = 0))
            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 0, tableauIndex = -1))
            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 7))

            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveFoundationToTableau(state, foundationIndex = -1, tableauIndex = 0)
            }
            assertThrows(IllegalArgumentException::class.java) {
                KlondikeRules.moveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 7)
            }
        }

        @Test
        @DisplayName("Move card from foundation to valid tableau column succeeds")
        fun `move card from foundation to valid tableau column succeeds`() {
            val blackThree = Card(Suit.SPADES, Rank.THREE, isFaceUp = true)
            val state = BoardState(
                foundations = listOf(
                    listOf(aceHearts, twoHearts),
                    emptyList<Card>(),
                    emptyList<Card>(),
                    emptyList<Card>()
                ),
                tableau = List(7) { colIndex ->
                    if (colIndex == 3) listOf(blackThree) else emptyList<Card>()
                },
                score = 30,
                movesCount = 20
            )

            // twoHearts can go onto blackThree (alternating color, rank 3 -> 2)
            assertTrue(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 3))

            val newState = KlondikeRules.moveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 3)

            assertEquals(listOf(aceHearts), newState.foundations[0])
            assertEquals(listOf(blackThree, twoHearts), newState.tableau[3])
            assertEquals(21, newState.movesCount)
            assertEquals(15, newState.score) // 30 - 15 = 15
        }

        @Test
        @DisplayName("Move card from foundation to invalid tableau column fails")
        fun `move card from foundation to invalid tableau column fails`() {
            val redThree = Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = true)
            val state = BoardState(
                foundations = listOf(
                    listOf(aceHearts, twoHearts),
                    emptyList<Card>(),
                    emptyList<Card>(),
                    emptyList<Card>()
                ),
                tableau = List(7) { colIndex ->
                    if (colIndex == 3) listOf(redThree) else emptyList<Card>()
                }
            )

            // twoHearts cannot go onto redThree (same color)
            assertFalse(KlondikeRules.canMoveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 3))
            assertThrows(IllegalStateException::class.java) {
                KlondikeRules.moveFoundationToTableau(state, foundationIndex = 0, tableauIndex = 3)
            }
        }
    }

    @Nested
    @DisplayName("isGameWon validation")
    inner class GameWonTests {

        @Test
        @DisplayName("Initial empty board is not won")
        fun `initial empty board is not won`() {
            val state = BoardState()
            assertFalse(KlondikeRules.isGameWon(state))
        }

        @Test
        @DisplayName("Partially filled foundations do not trigger win")
        fun `partially filled foundations do not trigger win`() {
            val partialFoundations = listOf(
                Rank.entries.map { rank -> Card(Suit.HEARTS, rank, isFaceUp = true) },
                Rank.entries.map { rank -> Card(Suit.SPADES, rank, isFaceUp = true) },
                Rank.entries.map { rank -> Card(Suit.DIAMONDS, rank, isFaceUp = true) },
                listOf(aceClubs, Card(Suit.CLUBS, Rank.TWO, isFaceUp = true))
            )
            val state = BoardState(foundations = partialFoundations)

            assertFalse(KlondikeRules.isGameWon(state))
        }

        @Test
        @DisplayName("All 4 foundations with 13 cards triggers win")
        fun `all 4 foundations completed triggers win`() {
            val completeFoundations = listOf(
                Rank.entries.map { rank -> Card(Suit.HEARTS, rank, isFaceUp = true) },
                Rank.entries.map { rank -> Card(Suit.SPADES, rank, isFaceUp = true) },
                Rank.entries.map { rank -> Card(Suit.DIAMONDS, rank, isFaceUp = true) },
                Rank.entries.map { rank -> Card(Suit.CLUBS, rank, isFaceUp = true) }
            )
            val state = BoardState(foundations = completeFoundations)

            assertTrue(KlondikeRules.isGameWon(state))
        }
    }
}
