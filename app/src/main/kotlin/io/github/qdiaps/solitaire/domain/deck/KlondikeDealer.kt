package io.github.qdiaps.solitaire.domain.deck

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import kotlin.random.Random

/**
 * Deals cards into the standard initial Klondike Solitaire layout.
 */
object KlondikeDealer {
    const val TABLEAU_COLUMNS_COUNT: Int = 7
    const val TABLEAU_CARDS_COUNT: Int = 28
    const val STOCK_CARDS_COUNT: Int = 24

    /**
     * Deals a 52-card deck into the initial Klondike Solitaire [BoardState].
     *
     * Tableau structure:
     * - Column 0: 1 card (1 face-up)
     * - Column 1: 2 cards (1 face-down, 1 face-up)
     * - ...
     * - Column 6: 7 cards (6 face-down, 1 face-up)
     *
     * Remaining 24 cards are placed face-down in the stock pile.
     * Foundations and waste are initialized empty.
     *
     * @param deck Deck of exactly 52 cards without duplicates.
     * @return Initial [BoardState].
     * @throws IllegalArgumentException if the deck size is not 52 or contains duplicates.
     */
    fun deal(deck: List<Card>): BoardState {
        require(deck.size == Deck.TOTAL_CARDS) {
            "A standard Klondike deal requires exactly ${Deck.TOTAL_CARDS} cards, got: ${deck.size}"
        }
        val uniqueIdsCount = deck.map { it.id }.toSet().size
        require(uniqueIdsCount == Deck.TOTAL_CARDS) {
            "Deck contains duplicate cards. Expected ${Deck.TOTAL_CARDS} unique cards, found: $uniqueIdsCount"
        }

        val tableau = ArrayList<List<Card>>(TABLEAU_COLUMNS_COUNT)
        var cursor = 0

        for (col in 0 until TABLEAU_COLUMNS_COUNT) {
            val count = col + 1
            val columnCards = ArrayList<Card>(count)
            for (i in 0 until count) {
                val card = deck[cursor++]
                val isTopCard = (i == count - 1)
                columnCards.add(card.copy(isFaceUp = isTopCard))
            }
            tableau.add(columnCards)
        }

        val stock = ArrayList<Card>(STOCK_CARDS_COUNT)
        while (cursor < deck.size) {
            stock.add(deck[cursor++].copy(isFaceUp = false))
        }

        return BoardState(
            stock = stock,
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = tableau,
            score = 0,
            movesCount = 0
        )
    }

    /**
     * Deals a newly generated and shuffled deck into an initial [BoardState].
     *
     * @param random Generator for deterministic or randomized shuffling.
     * @return Initial [BoardState].
     */
    fun dealShuffled(random: Random = Random.Default): BoardState =
        deal(Deck.shuffle(random = random))
}
