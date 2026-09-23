package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
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

    /**
     * Checks whether [card] can be legally placed on top of [targetColumn].
     *
     * According to Klondike rules:
     * - An empty column can only receive a [Rank.KING].
     * - A non-empty column can receive [card] only if its current topmost card is face-up,
     *   has the opposite color (red vs black), and has a rank exactly one value higher.
     *
     * @param card The [Card] to be placed.
     * @param targetColumn The current list of cards in the target tableau column.
     * @return `true` if placement is legal, `false` otherwise.
     */
    fun canPlaceOnTableau(card: Card, targetColumn: List<Card>): Boolean {
        if (targetColumn.isEmpty()) {
            return card.rank == Rank.KING
        }
        val topCard = targetColumn.last()
        return topCard.isFaceUp &&
            card.suit.isRed != topCard.suit.isRed &&
            card.rank.value == topCard.rank.value - 1
    }

    /**
     * Checks whether the given list of [cards] forms a valid descending, alternating-color
     * tableau sequence where every card is face-up.
     *
     * @param cards The list of cards to validate.
     * @return `true` if [cards] is non-empty and forms a valid face-up sequence, `false` otherwise.
     */
    fun isValidTableauSequence(cards: List<Card>): Boolean {
        if (cards.isEmpty()) return false
        if (cards.any { !it.isFaceUp }) return false

        for (i in 0 until cards.size - 1) {
            val current = cards[i]
            val next = cards[i + 1]
            if (next.suit.isRed == current.suit.isRed || next.rank.value != current.rank.value - 1) {
                return false
            }
        }
        return true
    }

    /**
     * Checks whether the topmost card from the waste pile can be moved to the specified tableau column.
     *
     * @param state Current [BoardState].
     * @param targetColumnIndex 0-based index of the target tableau column (0..6).
     * @return `true` if the move is legal, `false` otherwise.
     */
    fun canMoveWasteToTableau(state: BoardState, targetColumnIndex: Int): Boolean {
        if (targetColumnIndex !in 0 until state.tableau.size) return false
        if (state.waste.isEmpty()) return false
        return canPlaceOnTableau(state.waste.last(), state.tableau[targetColumnIndex])
    }

    /**
     * Moves the topmost card from the waste pile to the specified tableau column.
     *
     * @param state Current [BoardState].
     * @param targetColumnIndex 0-based index of the target tableau column (0..6).
     * @return New [BoardState] reflecting the moved card and incremented moves count.
     * @throws IllegalArgumentException if [targetColumnIndex] is out of range.
     * @throws IllegalStateException if waste is empty or move is illegal.
     */
    fun moveWasteToTableau(state: BoardState, targetColumnIndex: Int): BoardState {
        require(targetColumnIndex in 0 until state.tableau.size) {
            "Invalid target tableau column index: $targetColumnIndex"
        }
        check(state.waste.isNotEmpty()) { "Cannot move from an empty waste pile." }

        val cardToMove = state.waste.last()
        check(canPlaceOnTableau(cardToMove, state.tableau[targetColumnIndex])) {
            "Cannot move $cardToMove to tableau column $targetColumnIndex."
        }

        val newWaste = state.waste.dropLast(1)
        val newTableau = state.tableau.toMutableList().apply {
            this[targetColumnIndex] = this[targetColumnIndex] + cardToMove
        }

        return state.copy(
            waste = newWaste,
            tableau = newTableau,
            movesCount = state.movesCount + 1
        )
    }

    /**
     * Checks whether a card (or stack of cards) starting at [cardIndex] in [fromColumnIndex]
     * can be moved to [toColumnIndex].
     *
     * @param state Current [BoardState].
     * @param fromColumnIndex Source column index (0..6).
     * @param cardIndex Index of the base card within source column to move with all cards above it.
     * @param toColumnIndex Target column index (0..6).
     * @return `true` if the move is legal, `false` otherwise.
     */
    fun canMoveTableauToTableau(
        state: BoardState,
        fromColumnIndex: Int,
        cardIndex: Int,
        toColumnIndex: Int
    ): Boolean {
        if (fromColumnIndex !in 0 until state.tableau.size) return false
        if (toColumnIndex !in 0 until state.tableau.size) return false
        if (fromColumnIndex == toColumnIndex) return false

        val sourceColumn = state.tableau[fromColumnIndex]
        if (cardIndex !in sourceColumn.indices) return false

        val movingStack = sourceColumn.subList(cardIndex, sourceColumn.size)
        if (!isValidTableauSequence(movingStack)) return false

        return canPlaceOnTableau(movingStack.first(), state.tableau[toColumnIndex])
    }

    /**
     * Moves a card (or stack of cards) starting at [cardIndex] from [fromColumnIndex] to [toColumnIndex].
     *
     * @param state Current [BoardState].
     * @param fromColumnIndex Source column index (0..6).
     * @param cardIndex Index of the base card within source column to move with all cards above it.
     * @param toColumnIndex Target column index (0..6).
     * @return New [BoardState] reflecting the moved cards and incremented moves count.
     * @throws IllegalArgumentException if column indices or cardIndex are out of bounds, or if moving within same column.
     * @throws IllegalStateException if cards are face-down, sequence is invalid, or target cannot accept the base card.
     */
    fun moveTableauToTableau(
        state: BoardState,
        fromColumnIndex: Int,
        cardIndex: Int,
        toColumnIndex: Int
    ): BoardState {
        require(fromColumnIndex in 0 until state.tableau.size) {
            "Invalid source column index: $fromColumnIndex"
        }
        require(toColumnIndex in 0 until state.tableau.size) {
            "Invalid target column index: $toColumnIndex"
        }
        require(fromColumnIndex != toColumnIndex) {
            "Cannot move tableau cards within the same column: $fromColumnIndex"
        }

        val sourceColumn = state.tableau[fromColumnIndex]
        require(cardIndex in sourceColumn.indices) {
            "Card index $cardIndex out of bounds for column $fromColumnIndex (size: ${sourceColumn.size})"
        }

        val movingStack = sourceColumn.subList(cardIndex, sourceColumn.size)
        check(movingStack.first().isFaceUp) { "Cannot move face-down cards from tableau." }
        check(isValidTableauSequence(movingStack)) { "Moving cards do not form a valid tableau sequence." }
        check(canPlaceOnTableau(movingStack.first(), state.tableau[toColumnIndex])) {
            "Cannot place card ${movingStack.first()} on column $toColumnIndex."
        }

        val newSourceColumn = sourceColumn.take(cardIndex)
        val newTargetColumn = state.tableau[toColumnIndex] + movingStack

        val newTableau = state.tableau.toMutableList().apply {
            this[fromColumnIndex] = newSourceColumn
            this[toColumnIndex] = newTargetColumn
        }

        return state.copy(
            tableau = newTableau,
            movesCount = state.movesCount + 1
        )
    }

    /**
     * Checks whether [card] in [fromColumnIndex] (along with all cards on top of it) can be moved to [toColumnIndex].
     */
    fun canMoveTableauToTableau(
        state: BoardState,
        fromColumnIndex: Int,
        card: Card,
        toColumnIndex: Int
    ): Boolean {
        if (fromColumnIndex !in 0 until state.tableau.size) return false
        val cardIndex = state.tableau[fromColumnIndex].indexOf(card)
        if (cardIndex == -1) return false
        return canMoveTableauToTableau(state, fromColumnIndex, cardIndex, toColumnIndex)
    }

    /**
     * Moves [card] from [fromColumnIndex] (along with all cards on top of it) to [toColumnIndex].
     */
    fun moveTableauToTableau(
        state: BoardState,
        fromColumnIndex: Int,
        card: Card,
        toColumnIndex: Int
    ): BoardState {
        require(fromColumnIndex in 0 until state.tableau.size) {
            "Invalid source column index: $fromColumnIndex"
        }
        val cardIndex = state.tableau[fromColumnIndex].indexOf(card)
        check(cardIndex != -1) { "Card $card not found in column $fromColumnIndex." }
        return moveTableauToTableau(state, fromColumnIndex, cardIndex, toColumnIndex)
    }
}
