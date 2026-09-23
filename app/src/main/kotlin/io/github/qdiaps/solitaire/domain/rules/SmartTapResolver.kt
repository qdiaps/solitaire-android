package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank

/**
 * Encapsulates a proposed smart tap movement with its source, destination, and resulting [BoardState].
 */
data class SmartTapMove(
    val from: CardLocation,
    val to: CardLocation,
    val resultingState: BoardState
)

/**
 * Resolver for smart tap card auto-movements.
 *
 * Implements optimal destination resolution per Klondike specification:
 * - Priority 1: Move to Foundation (if valid).
 * - Priority 2: Move to a Tableau column that reveals a hidden face-down card.
 * - Priority 3: Move to the first valid Tableau column from left to right.
 */
object SmartTapResolver {

    /**
     * Resolves the optimal destination for a card at [source] location, or `null` if no valid move exists.
     *
     * @param state Current [BoardState].
     * @param source Source [CardLocation] of the tapped card.
     * @return Destination [CardLocation] (Foundation or Tableau), or `null`.
     */
    fun resolveDestination(state: BoardState, source: CardLocation): CardLocation? {
        return resolveMove(state, source)?.to
    }

    /**
     * Resolves the move for [source] and applies it to [state], returning the updated [BoardState],
     * or `null` if no valid move exists.
     *
     * @param state Current [BoardState].
     * @param source Source [CardLocation] of the tapped card.
     * @param autoExpose Whether to auto-expose the newly uncovered top card on tableau. Default `true`.
     * @return Updated [BoardState], or `null` if move cannot be resolved.
     */
    fun resolveAndApply(
        state: BoardState,
        source: CardLocation,
        autoExpose: Boolean = true
    ): BoardState? {
        val move = resolveMove(state, source, autoExpose) ?: return null
        return move.resultingState
    }

    /**
     * Finds the location of [card] on the board and resolves its destination.
     *
     * @param state Current [BoardState].
     * @param card [Card] to locate and resolve.
     * @return Destination [CardLocation], or `null` if card not found or has no valid moves.
     */
    fun resolveDestination(state: BoardState, card: Card): CardLocation? {
        val location = findCardLocation(state, card) ?: return null
        return resolveDestination(state, location)
    }

    /**
     * Finds the location of [card] on the board, resolves its destination and applies the move.
     *
     * @param state Current [BoardState].
     * @param card [Card] to locate and move.
     * @param autoExpose Whether to auto-expose the newly uncovered top card on tableau. Default `true`.
     * @return Updated [BoardState], or `null` if card not found or has no valid moves.
     */
    fun resolveAndApply(
        state: BoardState,
        card: Card,
        autoExpose: Boolean = true
    ): BoardState? {
        val location = findCardLocation(state, card) ?: return null
        return resolveAndApply(state, location, autoExpose)
    }

    /**
     * Resolves the full [SmartTapMove] (from, to, resultingState) for [source], or `null`.
     *
     * @param state Current [BoardState].
     * @param source Source [CardLocation] of the tapped card.
     * @param autoExpose Whether to auto-expose the newly uncovered top card on tableau. Default `true`.
     * @return Resolved [SmartTapMove], or `null`.
     */
    fun resolveMove(
        state: BoardState,
        source: CardLocation,
        autoExpose: Boolean = true
    ): SmartTapMove? {
        return when (source) {
            is CardLocation.Waste -> resolveWasteMove(state, autoExpose)
            is CardLocation.Tableau -> resolveTableauMove(state, source, autoExpose)
            else -> null // Stock and Foundation tapping do not auto-move
        }
    }

    /**
     * Evaluates all possible smart tap moves across the board and returns the best candidate
     * according to priority:
     * - Priority 1: Move to Foundation
     * - Priority 2: Move to Tableau that reveals a hidden face-down card
     * - Priority 3: Move to Tableau (leftmost valid)
     *
     * @param state Current [BoardState].
     * @param autoExpose Whether to auto-expose the newly uncovered top card on tableau. Default `true`.
     * @return Best [SmartTapMove], or `null` if no valid moves exist.
     */
    fun findBestMove(state: BoardState, autoExpose: Boolean = true): SmartTapMove? {
        val candidates = mutableListOf<SmartTapCandidate>()

        // 1. From Waste
        if (state.waste.isNotEmpty()) {
            val wasteMove = resolveMove(state, CardLocation.Waste, autoExpose)
            if (wasteMove != null) {
                val priority = if (wasteMove.to is CardLocation.Foundation) 1 else 3
                candidates.add(SmartTapCandidate(priority, 10, wasteMove))
            }
        }

        // 2. From Tableau columns
        for (colIndex in state.tableau.indices) {
            val column = state.tableau[colIndex]
            if (column.isEmpty()) continue

            for (cardIndex in column.indices) {
                val card = column[cardIndex]
                if (!card.isFaceUp) continue

                val move = resolveTableauMove(state, CardLocation.Tableau(colIndex, cardIndex), autoExpose)
                if (move != null) {
                    val priority = when {
                        move.to is CardLocation.Foundation -> 1
                        cardIndex > 0 && !column[cardIndex - 1].isFaceUp -> 2
                        else -> 3
                    }
                    candidates.add(SmartTapCandidate(priority, colIndex, move))
                }
            }
        }

        return candidates.minWithOrNull(
            compareBy<SmartTapCandidate> { it.priority }.thenBy { it.colIndex }
        )?.move
    }

    private data class SmartTapCandidate(
        val priority: Int,
        val colIndex: Int,
        val move: SmartTapMove
    )

    private fun resolveWasteMove(state: BoardState, autoExpose: Boolean): SmartTapMove? {
        if (state.waste.isEmpty()) return null
        val card = state.waste.last()

        // Priority 1: Foundation
        val foundationIndex = KlondikeRules.findTargetFoundationIndex(state, card)
        if (foundationIndex != null) {
            val newState = KlondikeRules.moveWasteToFoundation(state, foundationIndex)
            return SmartTapMove(
                from = CardLocation.Waste,
                to = CardLocation.Foundation(foundationIndex),
                resultingState = newState
            )
        }

        // Priority 3: Leftmost valid tableau column (0..6)
        for (targetCol in state.tableau.indices) {
            if (KlondikeRules.canPlaceOnTableau(card, state.tableau[targetCol])) {
                val newState = KlondikeRules.moveWasteToTableau(state, targetCol)
                return SmartTapMove(
                    from = CardLocation.Waste,
                    to = CardLocation.Tableau(targetCol),
                    resultingState = newState
                )
            }
        }

        return null
    }

    private fun resolveTableauMove(
        state: BoardState,
        source: CardLocation.Tableau,
        autoExpose: Boolean
    ): SmartTapMove? {
        val colIndex = source.columnIndex
        if (colIndex !in state.tableau.indices) return null
        val column = state.tableau[colIndex]
        val cardIndex = source.cardIndex
        if (cardIndex !in column.indices) return null

        val card = column[cardIndex]
        if (!card.isFaceUp) return null

        // Priority 1: Foundation (only topmost card of column can go to foundation)
        if (cardIndex == column.lastIndex) {
            val foundationIndex = KlondikeRules.findTargetFoundationIndex(state, card)
            if (foundationIndex != null) {
                val newState = KlondikeRules.moveTableauToFoundation(
                    state = state,
                    tableauIndex = colIndex,
                    foundationIndex = foundationIndex,
                    autoExpose = autoExpose
                )
                return SmartTapMove(
                    from = source,
                    to = CardLocation.Foundation(foundationIndex),
                    resultingState = newState
                )
            }
        }

        // Priority 2 / 3: Leftmost valid tableau column (0..6)
        for (targetCol in state.tableau.indices) {
            if (targetCol == colIndex) continue

            // Meaningless lateral move prevention: King already at base of column
            if (card.rank == Rank.KING && cardIndex == 0 && state.tableau[targetCol].isEmpty()) {
                continue
            }

            if (KlondikeRules.canMoveTableauToTableau(state, colIndex, cardIndex, targetCol)) {
                val newState = KlondikeRules.moveTableauToTableau(
                    state = state,
                    fromColumnIndex = colIndex,
                    cardIndex = cardIndex,
                    toColumnIndex = targetCol,
                    autoExpose = autoExpose
                )
                return SmartTapMove(
                    from = source,
                    to = CardLocation.Tableau(targetCol),
                    resultingState = newState
                )
            }
        }

        return null
    }

    private fun findCardLocation(state: BoardState, card: Card): CardLocation? {
        if (state.waste.isNotEmpty() && state.waste.last().id == card.id) {
            return CardLocation.Waste
        }
        for (colIndex in state.tableau.indices) {
            val cardIndex = state.tableau[colIndex].indexOfFirst { it.id == card.id }
            if (cardIndex != -1) {
                return CardLocation.Tableau(colIndex, cardIndex)
            }
        }
        return null
    }
}
