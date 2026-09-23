package io.github.qdiaps.solitaire.domain.deck

import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DeckTest {

    @Test
    @DisplayName("Verify standard deck creation has 52 unique face-down cards")
    fun `standard deck contains 52 unique face-down cards`() {
        val deck = Deck.createStandard52()

        assertEquals(52, deck.size)
        assertTrue(deck.all { !it.isFaceUp }, "All cards must initially be face down")

        val uniqueIds = deck.map { it.id }.toSet()
        assertEquals(52, uniqueIds.size, "Deck must contain 52 distinct cards")

        for (suit in Suit.entries) {
            val suitCards = deck.filter { it.suit == suit }
            assertEquals(13, suitCards.size, "Each suit must have 13 cards")
        }

        for (rank in Rank.entries) {
            val rankCards = deck.filter { it.rank == rank }
            assertEquals(4, rankCards.size, "Each rank must have 4 cards across suits")
        }
    }

    @Test
    @DisplayName("Verify shuffling with same seed produces identical order")
    fun `shuffling is deterministic with same seed`() {
        val shuffled1 = Deck.shuffle(random = Random(42))
        val shuffled2 = Deck.shuffle(random = Random(42))

        assertEquals(shuffled1, shuffled2)
        assertEquals(52, shuffled1.size)
    }

    @Test
    @DisplayName("Verify shuffling with different seeds produces different orders")
    fun `shuffling with different seeds produces distinct orders`() {
        val shuffled1 = Deck.shuffle(random = Random(42))
        val shuffled2 = Deck.shuffle(random = Random(9999))

        assertNotEquals(shuffled1, shuffled2)
    }

    @Test
    @DisplayName("Verify shuffling preserves all original cards")
    fun `shuffling preserves complete card set`() {
        val original = Deck.createStandard52()
        val shuffled = Deck.shuffle(original, Random(123))

        assertEquals(original.toSet(), shuffled.toSet())
    }
}
