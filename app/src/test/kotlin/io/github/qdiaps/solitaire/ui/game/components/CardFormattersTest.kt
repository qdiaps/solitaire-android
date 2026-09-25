package io.github.qdiaps.solitaire.ui.game.components

import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CardFormattersTest {

    @Nested
    @DisplayName("Rank display label tests")
    inner class RankLabelTests {

        @ParameterizedTest(name = "Rank {0} displays as {1}")
        @CsvSource(
            "ACE, A",
            "TWO, 2",
            "THREE, 3",
            "FOUR, 4",
            "FIVE, 5",
            "SIX, 6",
            "SEVEN, 7",
            "EIGHT, 8",
            "NINE, 9",
            "TEN, 10",
            "JACK, J",
            "QUEEN, Q",
            "KING, K"
        )
        fun `verify display label for all ranks`(rankName: String, expectedLabel: String) {
            val rank = Rank.valueOf(rankName)
            assertEquals(expectedLabel, rank.displayLabel)
        }
    }

    @Nested
    @DisplayName("Suit symbol tests")
    inner class SuitSymbolTests {

        @Test
        fun `verify standard unicode symbols for suits`() {
            assertEquals("♥", Suit.HEARTS.symbol)
            assertEquals("♦", Suit.DIAMONDS.symbol)
            assertEquals("♣", Suit.CLUBS.symbol)
            assertEquals("♠", Suit.SPADES.symbol)
        }
    }
}
