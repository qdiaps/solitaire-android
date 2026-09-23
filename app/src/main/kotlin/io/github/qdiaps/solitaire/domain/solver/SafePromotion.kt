package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Suit

/**
 * Heuristic analyzer and engine for provably safe Foundation promotions in Klondike Solitaire.
 *
 * ### Mathematical Guarantee (Safe Play Rule):
 * A card of rank $R$ and suit $S$ can be promoted to a Foundation without any risk of deadlocking
 * or preventing future optimal moves if:
 * 1. $R = 1$ (Ace): Unconditionally safe, as cards never build onto an Ace in the tableau.
 * 2. $R = 2$ (Two): Unconditionally safe, because the only card that could build onto a Two is an
 *    opposite-color Ace, and Aces always promote directly to foundations.
 * 3. $R \ge 3$: Safe if both opposite-color foundation piles have already reached at least rank $R - 1$.
 *    Because both opposite-color cards of rank $R - 1$ are already safely banked in the foundations,
 *    no card currently exists in tableau, stock, or waste that could ever be placed on this card.
 *    Therefore, promoting it can never block any future tableau placement.
 *
 * Applying safe promotions immediately collapses combinatorial search branches without losing
 * solvability.
 */
object SafePromotion {

    /**
     * Determines whether promoting [card] to a Foundation is mathematically safe given [foundations].
     *
     * @param card The card proposed for promotion.
     * @param foundations Current 4 foundation piles.
     * @return `true` if promotion is provably safe, `false` otherwise.
     */
    fun isSafe(card: Card, foundations: List<List<Card>>): Boolean {
        val rank = card.rank.value
        if (rank <= 2) return true

        val (opp1, opp2) = getOppositeSuits(card.suit)
        val r1 = getFoundationRank(opp1, foundations)
        val r2 = getFoundationRank(opp2, foundations)

        return r1 >= rank - 1 && r2 >= rank - 1
    }

    /**
     * Determines whether promoting [card] to a Foundation is mathematically safe in [state].
     */
    fun isSafe(card: Card, state: BoardState): Boolean = isSafe(card, state.foundations)

    /**
     * Returns the two suits of opposite color to the given [suit].
     */
    fun getOppositeSuits(suit: Suit): Pair<Suit, Suit> {
        return if (suit.isRed) {
            Pair(Suit.CLUBS, Suit.SPADES)
        } else {
            Pair(Suit.HEARTS, Suit.DIAMONDS)
        }
    }

    /**
     * Finds the highest rank (0..13) banked for [suit] across the four [foundations] piles.
     */
    fun getFoundationRank(suit: Suit, foundations: List<List<Card>>): Int {
        for (pile in foundations) {
            if (pile.isNotEmpty() && pile.first().suit == suit) {
                return pile.last().rank.value
            }
        }
        return 0
    }

    /**
     * Returns all legal and provably safe foundation promotion transitions currently available
     * in [state] from tableau columns and waste pile.
     */
    fun findSafeTransitions(state: BoardState): List<SolverTransition> {
        val allTransitions = SolverMoveGenerator.generateTransitions(state)
        return allTransitions.filter { transition ->
            transition.move.destination is CardLocation.Foundation &&
                isSafe(transition.move.cards.first(), state)
        }
    }

    /**
     * Iteratively applies all provably safe promotions in [state] until a fixed point is reached.
     * Uncovered face-down cards on the tableau are automatically exposed in each step.
     *
     * @param state Initial board state.
     * @return Board state with all unconditionally safe cards promoted to foundations.
     */
    fun applyAllSafePromotions(state: BoardState): BoardState {
        var current = state
        while (true) {
            val safeMoves = findSafeTransitions(current)
            if (safeMoves.isEmpty()) break
            current = safeMoves.first().state
        }
        return current
    }
}
