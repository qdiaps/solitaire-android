package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Standard playing card ranks from Ace (1) to King (13).
 *
 * @property value Numeric value of the rank according to standard Klondike rules.
 */
@Serializable
enum class Rank(val value: Int) {
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13);

    companion object {
        fun fromValue(value: Int): Rank =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid rank value: $value. Must be between 1 and 13.")
    }
}
