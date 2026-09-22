package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ModelsTest {

    @Nested
    @DisplayName("Suit tests")
    inner class SuitTests {
        @Test
        fun `verify suit colors`() {
            assertTrue(Suit.HEARTS.isRed)
            assertFalse(Suit.HEARTS.isBlack)

            assertTrue(Suit.DIAMONDS.isRed)
            assertFalse(Suit.DIAMONDS.isBlack)

            assertTrue(Suit.CLUBS.isBlack)
            assertFalse(Suit.CLUBS.isRed)

            assertTrue(Suit.SPADES.isBlack)
            assertFalse(Suit.SPADES.isRed)
        }

        @Test
        fun `verify all 4 suits exist`() {
            assertEquals(4, Suit.entries.size)
        }
    }

    @Nested
    @DisplayName("Rank tests")
    inner class RankTests {
        @Test
        fun `verify rank values range from 1 to 13`() {
            assertEquals(1, Rank.ACE.value)
            assertEquals(13, Rank.KING.value)
            assertEquals(13, Rank.entries.size)
        }

        @Test
        fun `verify fromValue returns matching rank`() {
            assertEquals(Rank.ACE, Rank.fromValue(1))
            assertEquals(Rank.SEVEN, Rank.fromValue(7))
            assertEquals(Rank.KING, Rank.fromValue(13))
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, 0, 14, 100])
        fun `verify fromValue throws exception for invalid values`(invalidValue: Int) {
            assertThrows(IllegalArgumentException::class.java) {
                Rank.fromValue(invalidValue)
            }
        }
    }

    @Nested
    @DisplayName("Card tests")
    inner class CardTests {
        @Test
        fun `verify default card properties`() {
            val card = Card(suit = Suit.HEARTS, rank = Rank.ACE)
            assertFalse(card.isFaceUp)
            assertEquals("ACE_HEARTS", card.id)
            assertTrue(card.isRed)
            assertFalse(card.isBlack)
        }

        @Test
        fun `verify card copying with flipped face`() {
            val card = Card(suit = Suit.SPADES, rank = Rank.KING)
            val flipped = card.copy(isFaceUp = true)

            assertTrue(flipped.isFaceUp)
            assertEquals("KING_SPADES", flipped.id)
            assertTrue(flipped.isBlack)
            assertFalse(flipped.isRed)
        }
    }

    @Nested
    @DisplayName("PileType tests")
    inner class PileTypeTests {
        @Test
        fun `verify all pile types exist`() {
            val types = PileType.entries
            assertEquals(4, types.size)
            assertTrue(types.contains(PileType.STOCK))
            assertTrue(types.contains(PileType.WASTE))
            assertTrue(types.contains(PileType.FOUNDATION))
            assertTrue(types.contains(PileType.TABLEAU))
        }
    }

    @Nested
    @DisplayName("CardLocation tests")
    inner class CardLocationTests {
        @Test
        fun `verify stock and waste pile types`() {
            assertEquals(PileType.STOCK, CardLocation.Stock.pileType)
            assertEquals(PileType.WASTE, CardLocation.Waste.pileType)
        }

        @Test
        fun `verify foundation locations with valid indices`() {
            for (i in 0..3) {
                val loc = CardLocation.Foundation(i)
                assertEquals(PileType.FOUNDATION, loc.pileType)
                assertEquals(i, loc.index)
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, 4, 5])
        fun `verify foundation throws exception for invalid index`(invalidIndex: Int) {
            assertThrows(IllegalArgumentException::class.java) {
                CardLocation.Foundation(invalidIndex)
            }
        }

        @Test
        fun `verify tableau locations with valid indices`() {
            for (col in 0..6) {
                val loc = CardLocation.Tableau(columnIndex = col, cardIndex = 3)
                assertEquals(PileType.TABLEAU, loc.pileType)
                assertEquals(col, loc.columnIndex)
                assertEquals(3, loc.cardIndex)
            }
        }

        @Test
        fun `verify tableau default cardIndex is zero`() {
            val loc = CardLocation.Tableau(columnIndex = 2)
            assertEquals(0, loc.cardIndex)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, 7, 10])
        fun `verify tableau throws exception for invalid column index`(invalidCol: Int) {
            assertThrows(IllegalArgumentException::class.java) {
                CardLocation.Tableau(columnIndex = invalidCol)
            }
        }

        @Test
        fun `verify tableau throws exception for negative card index`() {
            assertThrows(IllegalArgumentException::class.java) {
                CardLocation.Tableau(columnIndex = 0, cardIndex = -1)
            }
        }
    }

    @Nested
    @DisplayName("BoardState tests")
    inner class BoardStateTests {
        @Test
        fun `verify default board state dimensions`() {
            val board = BoardState()
            assertTrue(board.stock.isEmpty())
            assertTrue(board.waste.isEmpty())
            assertEquals(4, board.foundations.size)
            assertTrue(board.foundations.all { it.isEmpty() })
            assertEquals(7, board.tableau.size)
            assertTrue(board.tableau.all { it.isEmpty() })
            assertEquals(0, board.score)
            assertEquals(0, board.movesCount)
        }

        @Test
        fun `verify board state throws exception if foundation size is invalid`() {
            assertThrows(IllegalArgumentException::class.java) {
                BoardState(foundations = List(3) { emptyList() })
            }
            assertThrows(IllegalArgumentException::class.java) {
                BoardState(foundations = List(5) { emptyList() })
            }
        }

        @Test
        fun `verify board state throws exception if tableau size is invalid`() {
            assertThrows(IllegalArgumentException::class.java) {
                BoardState(tableau = List(6) { emptyList() })
            }
            assertThrows(IllegalArgumentException::class.java) {
                BoardState(tableau = List(8) { emptyList() })
            }
        }
    }

    @Nested
    @DisplayName("Serialization tests")
    inner class SerializationTests {
        private val json = Json { prettyPrint = false }

        @Test
        fun `roundtrip serialization for Card`() {
            val card = Card(suit = Suit.DIAMONDS, rank = Rank.QUEEN, isFaceUp = true)
            val encoded = json.encodeToString(card)
            val decoded = json.decodeFromString<Card>(encoded)
            assertEquals(card, decoded)
        }

        @Test
        fun `roundtrip serialization for CardLocation`() {
            val locations: List<CardLocation> = listOf(
                CardLocation.Stock,
                CardLocation.Waste,
                CardLocation.Foundation(2),
                CardLocation.Tableau(columnIndex = 4, cardIndex = 1)
            )

            for (loc in locations) {
                val encoded = json.encodeToString(loc)
                val decoded = json.decodeFromString<CardLocation>(encoded)
                assertEquals(loc, decoded)
            }
        }

        @Test
        fun `roundtrip serialization for BoardState`() {
            val board = BoardState(
                stock = listOf(Card(Suit.CLUBS, Rank.TWO)),
                waste = listOf(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)),
                score = 15,
                movesCount = 3
            )
            val encoded = json.encodeToString(board)
            val decoded = json.decodeFromString<BoardState>(encoded)
            assertEquals(board, decoded)
        }
    }
}
