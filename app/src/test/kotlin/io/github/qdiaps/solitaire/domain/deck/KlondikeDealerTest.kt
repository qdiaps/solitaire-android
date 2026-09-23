package io.github.qdiaps.solitaire.domain.deck

import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KlondikeDealerTest {

    @Test
    @DisplayName("Verify deal layout for tableau and stock")
    fun `deal sets up standard 7-column tableau and 24-card stock`() {
        val deck = Deck.createStandard52()
        val board = KlondikeDealer.deal(deck)

        // Tableau check
        assertEquals(7, board.tableau.size)
        var totalTableauCards = 0
        for (col in 0 until 7) {
            val columnCards = board.tableau[col]
            assertEquals(col + 1, columnCards.size, "Column $col should have ${col + 1} cards")
            totalTableauCards += columnCards.size

            // Cards before the last one must be face down
            for (i in 0 until columnCards.size - 1) {
                assertFalse(columnCards[i].isFaceUp, "Card at column $col, index $i should be face down")
            }
            // Top (last) card must be face up
            assertTrue(columnCards.last().isFaceUp, "Top card at column $col should be face up")
        }
        assertEquals(28, totalTableauCards, "Tableau should contain 28 cards total")

        // Stock check
        assertEquals(24, board.stock.size, "Stock should contain 24 cards")
        assertTrue(board.stock.all { !it.isFaceUp }, "All stock cards must be face down")

        // Waste and Foundations check
        assertTrue(board.waste.isEmpty(), "Waste must initially be empty")
        assertEquals(4, board.foundations.size)
        assertTrue(board.foundations.all { it.isEmpty() }, "All foundations must initially be empty")

        // Scores and moves count
        assertEquals(0, board.score)
        assertEquals(0, board.movesCount)

        // Total cards balance
        val allDealtCards = board.tableau.flatten() + board.stock
        assertEquals(52, allDealtCards.size)
        assertEquals(52, allDealtCards.map { it.id }.toSet().size, "All 52 cards must be distinct")
    }

    @Test
    @DisplayName("Verify dealShuffled produces deterministic board with same seed")
    fun `dealShuffled with same seed is deterministic`() {
        val board1 = KlondikeDealer.dealShuffled(Random(100))
        val board2 = KlondikeDealer.dealShuffled(Random(100))

        assertEquals(board1, board2)
    }

    @Test
    @DisplayName("Verify deal throws exception if deck size is invalid")
    fun `deal throws IllegalArgumentException when deck size is not 52`() {
        val partialDeck = Deck.createStandard52().drop(1)
        val overflowDeck = Deck.createStandard52() + Card(Suit.HEARTS, Rank.ACE)

        assertThrows(IllegalArgumentException::class.java) {
            KlondikeDealer.deal(partialDeck)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeDealer.deal(overflowDeck)
        }
    }

    @Test
    @DisplayName("Verify deal throws exception if deck has duplicate cards")
    fun `deal throws IllegalArgumentException when deck contains duplicates`() {
        val deckWithDuplicate = Deck.createStandard52().toMutableList().apply {
            this[1] = this[0] // duplicate first card
        }

        assertThrows(IllegalArgumentException::class.java) {
            KlondikeDealer.deal(deckWithDuplicate)
        }
    }
}
