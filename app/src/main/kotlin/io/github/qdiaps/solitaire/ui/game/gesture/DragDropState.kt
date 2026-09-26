package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation

/**
 * CompositionLocal providing access to the screen's [DragDropState] across the Compose hierarchy.
 */
val LocalDragDropState: ProvidableCompositionLocal<DragDropState?> =
    compositionLocalOf { null }

/**
 * Default animation specification for fast and snappy card snap-back returning to its origin.
 * Uses a smooth 160ms curve without oscillation or bounce delay.
 */
val DefaultSnapBackSpec: AnimationSpec<Offset> = tween(
    durationMillis = 160,
    easing = FastOutSlowInEasing
)

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
     * Whether a snap-back animation returning cards to origin is currently in progress.
     */
    var isSnappingBack: Boolean by mutableStateOf(false)
        private set

    /**
     * Whether cards are actively being dragged by user gesture or animating back to origin.
     */
    val isActive: Boolean
        get() = isDragging || isSnappingBack

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
     * If an existing snap-back animation is underway, it is immediately canceled
     * and overridden with the new card stack.
     *
     * @param source Board location from which cards are lifted.
     * @param cards Non-empty list of cards being moved.
     * @param originPosition Root-relative screen position of the lifted card stack.
     */
    fun startDrag(
        source: CardLocation,
        cards: List<Card>,
        originPosition: Offset = Offset.Zero,
        hapticFeedback: HapticFeedback? = null
    ) {
        require(cards.isNotEmpty()) { "Cannot start drag with empty card list" }
        this.isSnappingBack = false
        this.isDragging = true
        this.sourceLocation = source
        this.draggedCards = cards
        this.originPosition = originPosition
        this.dragPosition = originPosition
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
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
        isSnappingBack = false
        sourceLocation = null
        draggedCards = emptyList()
        originPosition = Offset.Zero
        dragPosition = Offset.Zero
    }

    /**
     * Animates the lifted card stack from current [dragPosition] back to [originPosition]
     * using snappy interpolation, then resets all drag state back to idle.
     *
     * @param animationSpec The [AnimationSpec] controlling motion (defaults to [DefaultSnapBackSpec]).
     */
    suspend fun snapBack(
        animationSpec: AnimationSpec<Offset> = DefaultSnapBackSpec
    ) {
        if (!isDragging && !isSnappingBack) return
        isDragging = false
        isSnappingBack = true
        try {
            val animatable = Animatable(dragPosition, Offset.VectorConverter)
            animatable.animateTo(
                targetValue = originPosition,
                animationSpec = animationSpec
            ) {
                // If a new drag started while animating, break out cleanly
                if (!isSnappingBack) return@animateTo
                dragPosition = value
            }
        } finally {
            if (isSnappingBack) {
                reset()
            }
        }
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
        originPosition: Offset = Offset.Zero,
        hapticFeedback: HapticFeedback? = null
    ): Boolean {
        val cardsToDrag = sliceTableauStack(columnCards, cardIndex)
        if (cardsToDrag.isEmpty()) return false

        startDrag(
            source = CardLocation.Tableau(columnIndex, cardIndex),
            cards = cardsToDrag,
            originPosition = originPosition,
            hapticFeedback = hapticFeedback
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
        originPosition: Offset = Offset.Zero,
        hapticFeedback: HapticFeedback? = null
    ): Boolean {
        val topCard = wasteCards.lastOrNull() ?: return false
        if (!topCard.isFaceUp) return false

        startDrag(
            source = CardLocation.Waste,
            cards = listOf(topCard),
            originPosition = originPosition,
            hapticFeedback = hapticFeedback
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
        originPosition: Offset = Offset.Zero,
        hapticFeedback: HapticFeedback? = null
    ): Boolean {
        val topCard = foundationCards.lastOrNull() ?: return false
        if (!topCard.isFaceUp) return false

        startDrag(
            source = CardLocation.Foundation(foundationIndex),
            cards = listOf(topCard),
            originPosition = originPosition,
            hapticFeedback = hapticFeedback
        )
        return true
    }

    /**
     * Returns whether the given [card] is currently part of the active drag stack.
     */
    fun isCardDragged(card: Card): Boolean {
        return (isDragging || isSnappingBack) && draggedCards.any { it.id == card.id }
    }

    /**
     * Returns whether a card at [location] and [cardIndex] should be hidden on the board
     * while being rendered in the floating drag overlay.
     */
    fun isCardHidden(location: CardLocation, cardIndex: Int): Boolean {
        if (!isDragging && !isSnappingBack) return false
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

    /**
     * Completes a drop gesture at current coordinates.
     *
     * Evaluates candidate targets from [registry] matching the stack bounding box [draggedBounds].
     * If a valid target is found according to [boardState] and [io.github.qdiaps.solitaire.domain.rules.KlondikeRules.canMoveCards]:
     * - Resets drag state.
     * - Triggers [hapticFeedback]?.performHapticFeedback(HapticFeedbackType.LongPress).
     * - Invokes [onValidDrop] callback with dragged cards, source, and target destination.
     * If invalid or outside any drop target:
     * - Animates the lifted card stack smoothly back to [originPosition] via [snapBack].
     *
     * @param boardState Current game board state snapshot.
     * @param registry Active [DropTargetRegistry] storing screen hitboxes.
     * @param draggedBounds Current screen bounding box of the moving card stack.
     * @param hapticFeedback Optional [HapticFeedback] instance for haptic snap.
     * @param onValidDrop Callback invoked when destination is legal.
     * @return `true` if drop was valid and applied, `false` if snapped back.
     */
    suspend fun onDropRelease(
        boardState: BoardState,
        registry: DropTargetRegistry,
        draggedBounds: Rect,
        hapticFeedback: HapticFeedback? = null,
        onValidDrop: (cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit
    ): Boolean {
        if (!isActive || draggedCards.isEmpty()) return false
        val source = sourceLocation ?: run {
            snapBack()
            return false
        }

        val validTarget = registry.findValidDropTarget(
            boardState = boardState,
            cards = draggedCards,
            source = source,
            draggedBounds = draggedBounds
        )

        return if (validTarget != null) {
            val cardsToMove = draggedCards
            reset()
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
            onValidDrop(cardsToMove, source, validTarget)
            true
        } else {
            snapBack()
            false
        }
    }
}

/**
 * Creates and remembers a [DragDropState] instance across recompositions.
 */
@Composable
fun rememberDragDropState(): DragDropState = remember { DragDropState() }
