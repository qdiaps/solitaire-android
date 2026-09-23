package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SafePromotionTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val threeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val fourHearts = Card(Suit.HEARTS, Rank.FOUR, isFaceUp = true)
    private val fiveHearts = Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true)

    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
    private val twoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)
    private val threeClubs = Card(Suit.CLUBS, Rank.THREE, isFaceUp = true)
    private val fourClubs = Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)

    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
    private val twoSpades = Card(Suit.SPADES, Rank.TWO, isFaceUp = true)
    private val threeSpades = Card(Suit.SPADES, Rank.THREE, isFaceUp = true)
    private val fourSpades = Card(Suit.SPADES, Rank.FOUR, isFaceUp = true)

    private val aceDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val twoDiamonds = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true)
    private val threeDiamonds = Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = true)

    @Nested
    @DisplayName("Single card safety rules")
    inner class SingleCardSafetyTests {

        @Test
        @DisplayName("Aces are always safe to promote regardless of foundation state")
        fun `aces are unconditionally safe`() {
            val emptyFoundations = List(4) { emptyList<Card>() }

            assertTrue(SafePromotion.isSafe(aceHearts, emptyFoundations))
            assertTrue(SafePromotion.isSafe(aceDiamonds, emptyFoundations))
            assertTrue(SafePromotion.isSafe(aceClubs, emptyFoundations))
            assertTrue(SafePromotion.isSafe(aceSpades, emptyFoundations))
        }

        @Test
        @DisplayName("Twos are always safe to promote regardless of foundation state")
        fun `twos are unconditionally safe`() {
            val emptyFoundations = List(4) { emptyList<Card>() }

            assertTrue(SafePromotion.isSafe(twoHearts, emptyFoundations))
            assertTrue(SafePromotion.isSafe(twoDiamonds, emptyFoundations))
            assertTrue(SafePromotion.isSafe(twoClubs, emptyFoundations))
            assertTrue(SafePromotion.isSafe(twoSpades, emptyFoundations))
        }

        @Test
        @DisplayName("Rank 3 is safe when both opposite-color foundations have reached rank 2")
        fun `rank 3 is safe when both opposite foundations reached rank 2`() {
            // Three of Hearts (Red). Opposite suits are Clubs and Spades (Black).
            val foundations = listOf(
                listOf(aceClubs, twoClubs),
                listOf(aceSpades, twoSpades),
                emptyList(),
                emptyList()
            )

            assertTrue(SafePromotion.isSafe(threeHearts, foundations))
        }

        @Test
        @DisplayName("Rank 3 is unsafe when one opposite-color foundation has not reached rank 2")
        fun `rank 3 is unsafe when one opposite foundation is below rank 2`() {
            // Three of Hearts (Red). Clubs reached 2, but Spades only reached Ace (1).
            val foundations = listOf(
                listOf(aceClubs, twoClubs),
                listOf(aceSpades),
                emptyList(),
                emptyList()
            )

            assertFalse(SafePromotion.isSafe(threeHearts, foundations))
        }

        @Test
        @DisplayName("Rank 4 is safe when both opposite-color foundations have reached rank 3")
        fun `rank 4 is safe when both opposite foundations reached rank 3`() {
            val foundations = listOf(
                listOf(aceClubs, twoClubs, threeClubs),
                listOf(aceSpades, twoSpades, threeSpades),
                emptyList(),
                emptyList()
            )

            assertTrue(SafePromotion.isSafe(fourHearts, foundations))
        }

        @Test
        @DisplayName("Rank 4 is unsafe when both opposite-color foundations are only at rank 2")
        fun `rank 4 is unsafe when opposite foundations are at rank 2`() {
            val foundations = listOf(
                listOf(aceClubs, twoClubs),
                listOf(aceSpades, twoSpades),
                emptyList(),
                emptyList()
            )

            assertFalse(SafePromotion.isSafe(fourHearts, foundations))
        }

        @Test
        @DisplayName("Opposite suits helper correctly pairs colors")
        fun `opposite suits helper returns correct suits`() {
            assertEquals(Pair(Suit.CLUBS, Suit.SPADES), SafePromotion.getOppositeSuits(Suit.HEARTS))
            assertEquals(Pair(Suit.CLUBS, Suit.SPADES), SafePromotion.getOppositeSuits(Suit.DIAMONDS))
            assertEquals(Pair(Suit.HEARTS, Suit.DIAMONDS), SafePromotion.getOppositeSuits(Suit.CLUBS))
            assertEquals(Pair(Suit.HEARTS, Suit.DIAMONDS), SafePromotion.getOppositeSuits(Suit.SPADES))
        }
    }

    @Nested
    @DisplayName("findSafeTransitions tests")
    inner class FindSafeTransitionsTests {

        @Test
        @DisplayName("Identifies safe Ace and Two promotions from tableau and waste")
        fun `identifies safe promotions from tableau and waste`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                tableau = listOf(
                    listOf(twoClubs),
                    listOf(fourHearts), // Unsafe
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                ),
                foundations = listOf(listOf(aceClubs), emptyList(), emptyList(), emptyList())
            )

            val safeMoves = SafePromotion.findSafeTransitions(state)

            assertEquals(2, safeMoves.size)
            assertTrue(safeMoves.any { it.move.cards.first() == aceHearts })
            assertTrue(safeMoves.any { it.move.cards.first() == twoClubs })
            assertFalse(safeMoves.any { it.move.cards.first() == fourHearts })
        }

        @Test
        @DisplayName("Returns empty list when no promotions are legal or safe")
        fun `returns empty list when no moves are safe`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(fourHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                ),
                foundations = List(4) { emptyList() }
            )

            val safeMoves = SafePromotion.findSafeTransitions(state)
            assertTrue(safeMoves.isEmpty())
        }
    }

    @Nested
    @DisplayName("applyAllSafePromotions fixed-point cascade")
    inner class ApplyAllSafePromotionsTests {

        @Test
        @DisplayName("Cascades safe promotions uncovering subsequent cards")
        fun `cascades safe promotions until fixed point`() {
            // Column 0 has: hiddenCard, then aceHearts on top.
            // When aceHearts promotes, hiddenCard is revealed. If hiddenCard is twoHearts, it also promotes!
            val hiddenTwoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = false)
            val state = BoardState(
                tableau = listOf(
                    listOf(hiddenTwoHearts, aceHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                ),
                foundations = List(4) { emptyList() }
            )

            val reducedState = SafePromotion.applyAllSafePromotions(state)

            // Both Ace and Two of Hearts should have automatically promoted to foundation!
            val heartsFoundation = reducedState.foundations.find { pile ->
                pile.any { it.suit == Suit.HEARTS }
            }
            assertEquals(2, heartsFoundation?.size)
            assertEquals(twoHearts, heartsFoundation?.last())
            assertTrue(reducedState.tableau[0].isEmpty())
        }

        @Test
        @DisplayName("Stops cascade when reaching an unsafe card")
        fun `stops cascade when encountering unsafe card`() {
            val threeHeartsHidden = Card(Suit.HEARTS, Rank.THREE, isFaceUp = false)
            val state = BoardState(
                tableau = listOf(
                    listOf(threeHeartsHidden, twoHearts, aceHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                ),
                foundations = List(4) { emptyList() }
            )

            val reducedState = SafePromotion.applyAllSafePromotions(state)

            // Ace and Two promote. Three is revealed face-up, but NOT promoted (because opposite black foundations are 0)
            assertEquals(1, reducedState.tableau[0].size)
            assertTrue(reducedState.tableau[0].first().isFaceUp)
            assertEquals(threeHearts, reducedState.tableau[0].first())
        }

        @Test
        @DisplayName("Preserves already fully reduced board")
        fun `preserves already reduced board`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true)),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val result = SafePromotion.applyAllSafePromotions(state)
            assertEquals(state, result)
        }
    }
}
