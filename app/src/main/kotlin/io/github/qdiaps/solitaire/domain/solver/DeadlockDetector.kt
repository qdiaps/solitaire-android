package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules

/**
 * Identified reason for a game deadlock.
 */
enum class DeadlockReason {
    /**
     * Stock and waste piles are completely empty, and no legal productive moves
     * remain on the tableau or foundations.
     */
    EXHAUSTED_STOCK,

    /**
     * Stock and/or waste contain cards, but cycling through the stock yields no cards
     * that can be legally placed on the tableau or foundations, and no productive
     * tableau moves remain.
     */
    STOCK_CYCLE_EXHAUSTED,

    /**
     * Tableau columns are locked and cannot produce any legal moves.
     */
    LOCKED_TABLEAU,

    /**
     * General deadlock condition where no legal productive moves can be made.
     */
    NO_LEGAL_MOVES
}

/**
 * Result returned by [DeadlockDetector] representing whether the game is still playable.
 */
sealed interface DeadlockStatus {
    /** Indicates whether the board state is currently deadlocked. */
    val isDeadlocked: Boolean

    /**
     * The game is active and has legal, productive moves available.
     */
    data object ActiveGame : DeadlockStatus {
        override val isDeadlocked: Boolean = false
    }

    /**
     * The game is deadlocked with no legal productive moves remaining.
     *
     * @property reason The identified cause of the deadlock.
     */
    data class Deadlock(val reason: DeadlockReason) : DeadlockStatus {
        override val isDeadlocked: Boolean = true
    }
}

/**
 * Real-time board analyzer for detecting unplayable deadlock states in Klondike Solitaire.
 *
 * Checks available legal actions across Stock, Waste, Tableau, and Foundations:
 * 1. Checks if the game is already won (returns [DeadlockStatus.ActiveGame]).
 * 2. Checks for immediate moves from Tableau to Foundation or Waste to Foundation.
 * 3. Checks for immediate moves from Waste to Tableau.
 * 4. Checks for productive Tableau to Tableau sequence transfers (filtering out useless
 *    King lateral shifts between empty columns and equivalent parent rank/color shifts).
 * 5. Optionally checks Foundation to Tableau demotions if enabled.
 * 6. Simulates stock draws and recycling to determine whether any card in the stock/waste
 *    cycle can legally be placed onto the tableau or foundations under the current [DrawMode].
 */
object DeadlockDetector {

    /**
     * Analyzes [state] to detect if any legal productive moves remain.
     *
     * @param state The board state to evaluate.
     * @param drawMode The active stock draw mode ([DrawMode.DRAW_ONE] or [DrawMode.DRAW_THREE]).
     * @param includeFoundationToTableau Whether returning cards from foundation to tableau is considered.
     * @return [DeadlockStatus.ActiveGame] if legal productive moves exist, or [DeadlockStatus.Deadlock].
     */
    fun detect(
        state: BoardState,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        includeFoundationToTableau: Boolean = false
    ): DeadlockStatus {
        // 1. If the game is won, it is victory, not a deadlock.
        if (KlondikeRules.isGameWon(state)) {
            return DeadlockStatus.ActiveGame
        }

        // 2. Check for any legal move from Tableau to Foundation.
        for (col in state.tableau) {
            if (col.isEmpty()) continue
            val topCard = col.last()
            if (!topCard.isFaceUp) continue
            if (KlondikeRules.findTargetFoundationIndex(state, topCard) != null) {
                return DeadlockStatus.ActiveGame
            }
        }

        // 3. Check for any legal move from Waste to Foundation.
        if (state.waste.isNotEmpty()) {
            val wasteCard = state.waste.last()
            if (KlondikeRules.findTargetFoundationIndex(state, wasteCard) != null) {
                return DeadlockStatus.ActiveGame
            }
        }

        // 4. Check for any legal move from Waste to Tableau.
        if (state.waste.isNotEmpty()) {
            for (colIndex in state.tableau.indices) {
                if (KlondikeRules.canMoveWasteToTableau(state, colIndex)) {
                    return DeadlockStatus.ActiveGame
                }
            }
        }

        // 5. Check for any productive Tableau to Tableau move.
        if (hasProductiveTableauMove(state)) {
            return DeadlockStatus.ActiveGame
        }

        // 6. Optional: Check Foundation to Tableau demotions if enabled.
        if (includeFoundationToTableau) {
            for (fIndex in state.foundations.indices) {
                val fPile = state.foundations[fIndex]
                if (fPile.isEmpty()) continue
                for (colIndex in state.tableau.indices) {
                    if (KlondikeRules.canMoveFoundationToTableau(state, fIndex, colIndex)) {
                        return DeadlockStatus.ActiveGame
                    }
                }
            }
        }

        // 7. Check Stock & Waste cycle for any accessible card that can be played.
        if (state.stock.isEmpty() && state.waste.isEmpty()) {
            return DeadlockStatus.Deadlock(DeadlockReason.EXHAUSTED_STOCK)
        }

        if (hasPlayableStockMove(state, drawMode)) {
            return DeadlockStatus.ActiveGame
        }

        return DeadlockStatus.Deadlock(DeadlockReason.STOCK_CYCLE_EXHAUSTED)
    }

    /**
     * Convenience method returning whether [state] is deadlocked.
     *
     * @param state The board state to evaluate.
     * @param drawMode The active stock draw mode.
     * @param includeFoundationToTableau Whether returning cards from foundation to tableau is considered.
     * @return `true` if deadlocked, `false` otherwise.
     */
    fun isDeadlocked(
        state: BoardState,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        includeFoundationToTableau: Boolean = false
    ): Boolean = detect(state, drawMode, includeFoundationToTableau).isDeadlocked

    /**
     * Evaluates whether any non-redundant, productive move between tableau columns exists.
     */
    private fun hasProductiveTableauMove(state: BoardState): Boolean {
        for (fromCol in state.tableau.indices) {
            val sourceCol = state.tableau[fromCol]
            if (sourceCol.isEmpty()) continue

            for (cardIndex in sourceCol.indices) {
                val baseCard = sourceCol[cardIndex]
                if (!baseCard.isFaceUp) continue

                val movingStack = sourceCol.subList(cardIndex, sourceCol.size)
                if (!KlondikeRules.isValidTableauSequence(movingStack)) continue

                for (toCol in state.tableau.indices) {
                    if (toCol == fromCol) continue
                    val targetCol = state.tableau[toCol]

                    if (KlondikeRules.canMoveTableauToTableau(state, fromCol, cardIndex, toCol)) {
                        if (targetCol.isEmpty()) {
                            // Moving a King from the base of a column (cardIndex == 0) to an empty column
                            // is an unproductive lateral hop that uncovers no cards.
                            if (cardIndex == 0) {
                                continue
                            }
                            return true
                        } else {
                            // Moving to an equivalent parent rank and color uncovers nothing and changes nothing.
                            if (cardIndex > 0 && sourceCol[cardIndex - 1].isFaceUp) {
                                val parentCard = sourceCol[cardIndex - 1]
                                val targetTop = targetCol.last()
                                if (parentCard.rank == targetTop.rank && parentCard.suit.isRed == targetTop.suit.isRed) {
                                    continue
                                }
                            }
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Simulates stock drawing and recycling to determine whether any card that ever
     * surfaces at the top of the waste pile can be placed onto the tableau or foundations.
     */
    private fun hasPlayableStockMove(state: BoardState, drawMode: DrawMode): Boolean {
        var sim = state
        val visitedStockStates = HashSet<Pair<List<Card>, List<Card>>>()

        while (KlondikeRules.canDraw(sim) || KlondikeRules.canRecycle(sim)) {
            val stateKey = Pair(sim.stock, sim.waste)
            if (!visitedStockStates.add(stateKey)) {
                break
            }

            sim = KlondikeRules.drawOrRecycle(sim, drawMode)

            if (sim.waste.isNotEmpty()) {
                val topWaste = sim.waste.last()
                // Can topWaste move to Foundation?
                if (KlondikeRules.findTargetFoundationIndex(state, topWaste) != null) {
                    return true
                }
                // Can topWaste move to any Tableau column?
                for (col in state.tableau) {
                    if (KlondikeRules.canPlaceOnTableau(topWaste, col)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
