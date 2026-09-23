package io.github.qdiaps.solitaire.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the specific board location of a card or target slot.
 */
@Serializable
sealed interface CardLocation {
    val pileType: PileType

    @Serializable
    data object Stock : CardLocation {
        override val pileType: PileType = PileType.STOCK
    }

    @Serializable
    data object Waste : CardLocation {
        override val pileType: PileType = PileType.WASTE
    }

    @Serializable
    data class Foundation(val index: Int) : CardLocation {
        override val pileType: PileType = PileType.FOUNDATION

        init {
            require(index in 0..3) { "Foundation index must be between 0 and 3, got: $index" }
        }
    }

    @Serializable
    data class Tableau(val columnIndex: Int, val cardIndex: Int = 0) : CardLocation {
        override val pileType: PileType = PileType.TABLEAU

        init {
            require(columnIndex in 0..6) { "Tableau columnIndex must be between 0 and 6, got: $columnIndex" }
            require(cardIndex >= 0) { "Tableau cardIndex must be non-negative, got: $cardIndex" }
        }
    }
}
