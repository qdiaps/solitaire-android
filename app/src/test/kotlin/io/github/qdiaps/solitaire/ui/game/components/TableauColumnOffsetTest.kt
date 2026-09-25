package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TableauColumnOffsetTest {

    private val downPeek = 14.dp
    private val upPeek = 25.dp

    @Nested
    @DisplayName("Empty and Single Card Cases")
    inner class EdgeCases {

        @Test
        fun `empty cards list returns empty offsets list`() {
            val offsets = calculateTableauOffsets(
                cards = emptyList(),
                faceDownPeek = downPeek,
                faceUpPeek = upPeek
            )
            assertTrue(offsets.isEmpty())
        }

        @Test
        fun `single card returns zero offset`() {
            val card = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
            val offsets = calculateTableauOffsets(
                cards = listOf(card),
                faceDownPeek = downPeek,
                faceUpPeek = upPeek
            )
            assertEquals(listOf(0.dp), offsets)
        }
    }

    @Nested
    @DisplayName("Face-Down Cascade Cases")
    inner class FaceDownCases {

        @Test
        fun `multiple face-down cards advance by faceDownPeek`() {
            val cards = listOf(
                Card(Suit.SPADES, Rank.ACE, isFaceUp = false),
                Card(Suit.HEARTS, Rank.TWO, isFaceUp = false),
                Card(Suit.CLUBS, Rank.THREE, isFaceUp = false)
            )
            val offsets = calculateTableauOffsets(
                cards = cards,
                faceDownPeek = downPeek,
                faceUpPeek = upPeek
            )
            assertEquals(listOf(0.dp, 14.dp, 28.dp), offsets)
        }
    }

    @Nested
    @DisplayName("Face-Up and Mixed Cascade Cases")
    inner class MixedCases {

        @Test
        fun `multiple face-up cards advance by faceUpPeek`() {
            val cards = listOf(
                Card(Suit.HEARTS, Rank.KING, isFaceUp = true),
                Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true),
                Card(Suit.DIAMONDS, Rank.JACK, isFaceUp = true)
            )
            val offsets = calculateTableauOffsets(
                cards = cards,
                faceDownPeek = downPeek,
                faceUpPeek = upPeek
            )
            assertEquals(listOf(0.dp, 25.dp, 50.dp), offsets)
        }

        @Test
        fun `mixed face-down and face-up cards advance according to each card state`() {
            val cards = listOf(
                Card(Suit.CLUBS, Rank.TEN, isFaceUp = false),
                Card(Suit.DIAMONDS, Rank.NINE, isFaceUp = false),
                Card(Suit.HEARTS, Rank.EIGHT, isFaceUp = true),
                Card(Suit.SPADES, Rank.SEVEN, isFaceUp = true)
            )
            val offsets = calculateTableauOffsets(
                cards = cards,
                faceDownPeek = downPeek,
                faceUpPeek = upPeek
            )
            assertEquals(listOf(0.dp, 14.dp, 28.dp, 53.dp), offsets)
        }
    }
}
