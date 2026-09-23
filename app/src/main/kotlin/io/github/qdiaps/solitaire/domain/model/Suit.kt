package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the four standard French playing card suits.
 *
 * @property isRed Whether the suit has a red color (Hearts, Diamonds) or black (Clubs, Spades).
 */
@Serializable
enum class Suit(val isRed: Boolean) {
    HEARTS(isRed = true),
    DIAMONDS(isRed = true),
    CLUBS(isRed = false),
    SPADES(isRed = false);

    val isBlack: Boolean
        get() = !isRed
}
