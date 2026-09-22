package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable representation of a playing card.
 *
 * @property suit Suit of the card.
 * @property rank Rank of the card.
 * @property isFaceUp Whether the card is face up (visible to player) or face down (hidden).
 * @property id Unique identifier for the card instance.
 */
@Serializable
data class Card(
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false,
    val id: String = "${rank.name}_${suit.name}"
) {
    val isRed: Boolean
        get() = suit.isRed

    val isBlack: Boolean
        get() = suit.isBlack
}
