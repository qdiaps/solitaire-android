package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot representing the entire board state at a point in time.
 *
 * @property stock List of face-down cards remaining in the stock pile.
 * @property waste List of face-up cards drawn from the stock pile.
 * @property foundations Four suit foundation piles (indices 0..3).
 * @property tableau Seven tableau columns (indices 0..6).
 * @property score Current game score.
 * @property movesCount Total number of valid moves performed so far.
 */
@Serializable
data class BoardState(
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val foundations: List<List<Card>> = List(4) { emptyList() },
    val tableau: List<List<Card>> = List(7) { emptyList() },
    val score: Int = 0,
    val movesCount: Int = 0
) {
    init {
        require(foundations.size == 4) { "Foundations must contain exactly 4 piles, got: ${foundations.size}" }
        require(tableau.size == 7) { "Tableau must contain exactly 7 columns, got: ${tableau.size}" }
    }
}
