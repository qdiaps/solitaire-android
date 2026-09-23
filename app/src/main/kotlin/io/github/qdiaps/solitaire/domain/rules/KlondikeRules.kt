package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import kotlin.math.min

/**
 * Pure domain rules engine for Klondike Solitaire.
 *
 * Implements validation and state transitions for game operations such as
 * stock drawing, recycling, card movements, and foundations building.
 */
object KlondikeRules {

    /**
     * Checks whether cards can be drawn from the stock pile.
     *
     * @param state Current [BoardState].
     * @return `true` if the stock contains at least one card, `false` otherwise.
     */
    fun canDraw(state: BoardState): Boolean = state.stock.isNotEmpty()

    /**
     * Checks whether the waste pile can be recycled back into the stock pile.
     *
     * In standard Klondike rules, recycling is permitted only when the stock pile is completely
     * depleted and the waste pile contains one or more cards.
     *
     * @param state Current [BoardState].
     * @return `true` if stock is empty and waste is non-empty, `false` otherwise.
     */
    fun canRecycle(state: BoardState): Boolean = state.stock.isEmpty() && state.waste.isNotEmpty()

    /**
     * Draws cards from the stock pile to the waste pile according to [drawMode].
     *
     * In Draw 1 mode, 1 card is drawn. In Draw 3 mode, up to 3 cards are drawn (or whatever
     * remains if fewer than 3 cards are left). Cards drawn from stock are turned face-up
     * and placed on top of the waste pile in sequential order (with the last drawn card on top).
     *
     * @param state Current [BoardState].
     * @param drawMode [DrawMode] specifying how many cards to draw (default: [DrawMode.DRAW_ONE]).
     * @return New [BoardState] with updated stock, waste, and incremented moves count.
     * @throws IllegalStateException if the stock pile is empty.
     */
    fun draw(state: BoardState, drawMode: DrawMode = DrawMode.DRAW_ONE): BoardState {
        check(canDraw(state)) { "Cannot draw from an empty stock pile." }

        val count = min(drawMode.cardsCount, state.stock.size)
        val drawnCards = state.stock.take(count).map { it.copy(isFaceUp = true) }
        val remainingStock = state.stock.drop(count)
        val newWaste = state.waste + drawnCards

        return state.copy(
            stock = remainingStock,
            waste = newWaste,
            movesCount = state.movesCount + 1
        )
    }

    /**
     * Recycles the entire waste pile back into the stock pile.
     *
     * All cards from the waste pile are turned face-down and returned to the stock pile
     * without changing their relative physical order (the bottom card of the waste pile
     * becomes the top card of the renewed stock pile). The waste pile becomes empty.
     *
     * @param state Current [BoardState].
     * @return New [BoardState] with restored stock, empty waste, and incremented moves count.
     * @throws IllegalStateException if stock is not empty or waste is empty.
     */
    fun recycle(state: BoardState): BoardState {
        check(state.stock.isEmpty()) { "Cannot recycle waste while stock pile is not empty." }
        check(state.waste.isNotEmpty()) { "Cannot recycle an empty waste pile." }

        val newStock = state.waste.map { it.copy(isFaceUp = false) }

        return state.copy(
            stock = newStock,
            waste = emptyList(),
            movesCount = state.movesCount + 1
        )
    }

    /**
     * Convenience method to handle a stock tap action.
     *
     * If the stock contains cards, draws cards according to [drawMode].
     * If the stock is empty and waste contains cards, recycles waste back into stock.
     * If both stock and waste are empty, returns the [state] unmodified.
     *
     * @param state Current [BoardState].
     * @param drawMode [DrawMode] to use if drawing cards.
     * @return Resulting [BoardState].
     */
    fun drawOrRecycle(state: BoardState, drawMode: DrawMode = DrawMode.DRAW_ONE): BoardState {
        return when {
            canDraw(state) -> draw(state, drawMode)
            canRecycle(state) -> recycle(state)
            else -> state
        }
    }
}
