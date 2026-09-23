package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Identifies the logical piles on a Klondike Solitaire board.
 */
@Serializable
enum class PileType {
    STOCK,
    WASTE,
    FOUNDATION,
    TABLEAU
}
