package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a discrete game move between card locations on a Solitaire board.
 *
 * @property source Starting [CardLocation] of the moved cards.
 * @property destination Target [CardLocation] where the cards are moved.
 * @property cards The cards that were moved.
 * @property scoreDelta Points awarded or deducted by this move.
 */
@Serializable
data class Move(
    val source: CardLocation,
    val destination: CardLocation,
    val cards: List<Card>,
    val scoreDelta: Int = 0
) {
    init {
        require(cards.isNotEmpty()) { "A move must involve at least one card." }
    }
}
