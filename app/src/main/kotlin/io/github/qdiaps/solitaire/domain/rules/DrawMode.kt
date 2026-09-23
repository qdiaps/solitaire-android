package io.github.qdiaps.solitaire.domain.rules

import kotlinx.serialization.Serializable

/**
 * Draw mode configuration for the stock pile in Klondike Solitaire.
 *
 * @property cardsCount Number of cards drawn per tap from the stock pile.
 */
@Serializable
enum class DrawMode(val cardsCount: Int) {
    /** Casual mode: 1 card drawn at a time. */
    DRAW_ONE(1),

    /** Classic tournament mode: 3 cards drawn at a time. */
    DRAW_THREE(3);

    init {
        require(cardsCount > 0) { "DrawMode cardsCount must be greater than zero, got: $cardsCount" }
    }
}
