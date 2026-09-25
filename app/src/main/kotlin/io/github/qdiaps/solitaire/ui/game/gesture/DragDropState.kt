package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation

/**
 * CompositionLocal providing access to the screen's [DragDropState] across the Compose hierarchy.
 */
val LocalDragDropState: ProvidableCompositionLocal<DragDropState?> =
    compositionLocalOf { null }

/**
 * Manages the active drag-and-drop gesture lifecycle, dragged cards stack,
 * root coordinate offsets, and source board visibility masking.
 */
class DragDropState {

    /**
     * Whether a drag gesture is currently active.
     */
    var isDragging: Boolean by mutableStateOf(false)
        private set

    /**
     * The board location from which cards were lifted.
     */
    var sourceLocation: CardLocation? by mutableStateOf(null)
        private set

    /**
     * The list of cards currently being dragged (ordered from bottom to top of the lifted stack).
     */
    var draggedCards: List<Card> by mutableStateOf(emptyList())
        private set

    /**
     * The root-relative screen coordinates where the drag began.
     */
    var originPosition: Offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * The current root-relative screen coordinates of the moving card stack.
     */
    var dragPosition: Offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * The displacement vector between current [dragPosition] and [originPosition].
     */
    val dragOffset: Offset
        get() = dragPosition - originPosition

    /**
     * Initiates a drag operation with the given [cards] from [source].
     *
     * @param source Board location from which cards are lifted.
     * @param cards Non-empty list of cards being moved.
     * @param originPosition Root-relative screen position of the lifted card stack.
     */
    fun startDrag(
        source: CardLocation,
        cards: List<Card>,
        originPosition: Offset = Offset.Zero
    ) {
        require(cards.isNotEmpty()) { "Cannot start drag with empty card list" }
        this.isDragging = true
        this.sourceLocation = source
        this.draggedCards = cards
        this.originPosition = originPosition
        this.dragPosition = originPosition
    }

    /**
     * Updates the current drag position by applying a relative [delta].
     */
    fun onDragDelta(delta: Offset) {
        if (!isDragging) return
        dragPosition += delta
    }

    /**
     * Updates the absolute [dragPosition] directly.
     */
    fun updateDragPosition(position: Offset) {
        if (!isDragging) return
        dragPosition = position
    }

    /**
     * Terminates the active drag gesture and resets all fields back to idle state.
     */
    fun reset() {
        isDragging = false
        sourceLocation = null
        draggedCards = emptyList()
        originPosition = Offset.Zero
        dragPosition = Offset.Zero
    }

    /**
     * Slices a tableau column to extract a sub-stack starting at [fromIndex].
     *
     * In Klondike rules:
     * - [fromIndex] must be within column bounds.
     * - The card at [fromIndex] must be face-up.
     *
     * @param columnCards The full list of cards in the column.
     * @param fromIndex The 0-based index of the card being dragged.
     * @return Sub-stack of cards from [fromIndex] to end of column, or empty list if invalid.
     */
    fun sliceTableauStack(columnCards: List<Card>, fromIndex: Int): List<Card> {
        if (fromIndex !in columnCards.indices) return emptyList()
        val targetCard = columnCards[fromIndex]
        if (!targetCard.isFaceUp) return emptyList()
        return columnCards.subList(fromIndex, columnCards.size)
    }

    /**
     * Attempts to initiate a drag from a tableau column.
     *
     * @param columnIndex The 0-based column index (0..6).
     * @param cardIndex The 0-based index of the tapped/dragged card in that column.
     * @param columnCards The current list of cards in that column.
     * @param originPosition The root-relative position of the card.
     * @return `true` if drag was successfully started, `false` otherwise.
     */
    fun startTableauDrag(
        columnIndex: Int,
        cardIndex: Int,
        columnCards: List<Card>,
        originPosition: Offset = Offset.Zero
    ): Boolean {
        val cardsToDrag = sliceTableauStack(columnCards, cardIndex)
        if (cardsToDrag.isEmpty()) return false

        startDrag(
            source = CardLocation.Tableau(columnIndex, cardIndex),
            cards = cardsToDrag,
            originPosition = originPosition
        )
        return true
    }

    /**
     * Attempts to initiate a drag from the waste pile.
     *
     * @param wasteCards The list of cards currently in the waste pile.
     * @param originPosition The root-relative position of the top waste card.
     * @return `true` if drag was successfully started, `false` otherwise.
     */
    fun startWasteDrag(
        wasteCards: List<Card>,
        originPosition: Offset = Offset.Zero
    ): Boolean {
        val topCard = wasteCards.lastOrNull() ?: return false
        if (!topCard.isFaceUp) return false

        startDrag(
            source = CardLocation.Waste,
            cards = listOf(topCard),
            originPosition = originPosition
        )
        return true
    }

    /**
     * Attempts to initiate a drag from a foundation pile.
     *
     * @param foundationIndex The 0-based foundation index (0..3).
     * @param foundationCards The cards currently in that foundation pile.
     * @param originPosition The root-relative position of the foundation slot.
     * @return `true` if drag was successfully started, `false` otherwise.
     */
    fun startFoundationDrag(
        foundationIndex: Int,
        foundationCards: List<Card>,
        originPosition: Offset = Offset.Zero
    ): Boolean {
        val topCard = foundationCards.lastOrNull() ?: return false
        if (!topCard.isFaceUp) return false

        startDrag(
            source = CardLocation.Foundation(foundationIndex),
            cards = listOf(topCard),
            originPosition = originPosition
        )
        return true
    }

    /**
     * Returns whether the given [card] is currently part of the active drag stack.
     */
    fun isCardDragged(card: Card): Boolean {
        return isDragging && draggedCards.any { it.id == card.id }
    }

    /**
     * Returns whether a card at [location] and [cardIndex] should be hidden on the board
     * while being rendered in the floating drag overlay.
     */
    fun isCardHidden(location: CardLocation, cardIndex: Int): Boolean {
        if (!isDragging) return false
        val source = sourceLocation ?: return false

        return when {
            source is CardLocation.Tableau && location is CardLocation.Tableau -> {
                source.columnIndex == location.columnIndex && cardIndex >= source.cardIndex
            }
            source is CardLocation.Waste && location is CardLocation.Waste -> true
            source is CardLocation.Foundation && location is CardLocation.Foundation -> {
                source.index == location.index
            }
            else -> false
        }
    }
}

/**
 * Creates and remembers a [DragDropState] instance across recompositions.
 */
@Composable
fun rememberDragDropState(): DragDropState = remember { DragDropState() }
