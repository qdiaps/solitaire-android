package io.github.qdiaps.solitaire.domain.deck

import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import kotlin.random.Random

/**
 * Generates and shuffles standard decks of playing cards.
 */
object Deck {
    const val TOTAL_CARDS: Int = 52

    /**
     * Generates an unshuffled standard French 52-card deck with all cards face-down.
     */
    fun createStandard52(): List<Card> {
        val cards = ArrayList<Card>(TOTAL_CARDS)
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(Card(suit = suit, rank = rank, isFaceUp = false))
            }
        }
        return cards
    }

    /**
     * Returns a shuffled copy of the card list using the specified [random] generator.
     */
    fun shuffle(
        cards: List<Card> = createStandard52(),
        random: Random = Random.Default
    ): List<Card> = cards.shuffled(random)
}
