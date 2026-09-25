package io.github.qdiaps.solitaire.ui.game.components

import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit

/**
 * Clean short label representation of playing card rank (e.g. "A", "10", "K").
 */
val Rank.displayLabel: String
    get() = when (this) {
        Rank.ACE -> "A"
        Rank.TWO -> "2"
        Rank.THREE -> "3"
        Rank.FOUR -> "4"
        Rank.FIVE -> "5"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
    }

/**
 * Unicode symbol representation of card suit.
 */
val Suit.symbol: String
    get() = when (this) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }
