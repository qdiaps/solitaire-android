package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WastePileOffsetTest {

    private val fanOffset = 18.dp

    @Nested
    @DisplayName("calculateWasteOffsets")
    inner class CalculateWasteOffsetsTests {

        @Test
        fun `zero or negative count returns empty list`() {
            assertTrue(calculateWasteOffsets(0, isLeftHanded = false, fanOffset = fanOffset).isEmpty())
            assertTrue(calculateWasteOffsets(-1, isLeftHanded = false, fanOffset = fanOffset).isEmpty())
        }

        @Test
        fun `single card returns zero offset regardless of handedness`() {
            assertEquals(listOf(0.dp), calculateWasteOffsets(1, isLeftHanded = false, fanOffset = fanOffset))
            assertEquals(listOf(0.dp), calculateWasteOffsets(1, isLeftHanded = true, fanOffset = fanOffset))
        }

        @Test
        fun `two cards in standard mode fan rightward`() {
            assertEquals(listOf(0.dp, 18.dp), calculateWasteOffsets(2, isLeftHanded = false, fanOffset = fanOffset))
        }

        @Test
        fun `three cards in standard mode fan rightward`() {
            assertEquals(listOf(0.dp, 18.dp, 36.dp), calculateWasteOffsets(3, isLeftHanded = false, fanOffset = fanOffset))
        }

        @Test
        fun `two cards in left-handed mode fan leftward`() {
            assertEquals(listOf(0.dp, (-18).dp), calculateWasteOffsets(2, isLeftHanded = true, fanOffset = fanOffset))
        }

        @Test
        fun `three cards in left-handed mode fan leftward`() {
            assertEquals(listOf(0.dp, (-18).dp, (-36).dp), calculateWasteOffsets(3, isLeftHanded = true, fanOffset = fanOffset))
        }
    }

    @Nested
    @DisplayName("getVisibleWasteCards")
    inner class GetVisibleWasteCardsTests {

        private val card1 = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "h1")
        private val card2 = Card(Suit.SPADES, Rank.TWO, isFaceUp = true, id = "s2")
        private val card3 = Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = true, id = "d3")
        private val card4 = Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true, id = "c4")
        private val card5 = Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true, id = "h5")

        @Test
        fun `empty waste returns empty list`() {
            assertTrue(getVisibleWasteCards(emptyList(), DrawMode.DRAW_ONE).isEmpty())
            assertTrue(getVisibleWasteCards(emptyList(), DrawMode.DRAW_THREE).isEmpty())
        }

        @Test
        fun `DrawMode DRAW_ONE returns only the topmost card`() {
            assertEquals(listOf(card1), getVisibleWasteCards(listOf(card1), DrawMode.DRAW_ONE))
            assertEquals(listOf(card5), getVisibleWasteCards(listOf(card1, card2, card3, card4, card5), DrawMode.DRAW_ONE))
        }

        @Test
        fun `DrawMode DRAW_THREE returns up to 3 topmost cards`() {
            assertEquals(listOf(card1), getVisibleWasteCards(listOf(card1), DrawMode.DRAW_THREE))
            assertEquals(listOf(card1, card2), getVisibleWasteCards(listOf(card1, card2), DrawMode.DRAW_THREE))
            assertEquals(listOf(card1, card2, card3), getVisibleWasteCards(listOf(card1, card2, card3), DrawMode.DRAW_THREE))
            assertEquals(listOf(card3, card4, card5), getVisibleWasteCards(listOf(card1, card2, card3, card4, card5), DrawMode.DRAW_THREE))
        }
    }
}
