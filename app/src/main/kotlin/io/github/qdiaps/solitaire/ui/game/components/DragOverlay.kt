package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.gesture.DragDropState
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.math.roundToInt

/**
 * Elevation applied to cards actively floating in the drag overlay.
 */
val DRAG_OVERLAY_ELEVATION: Dp = 12.dp

/**
 * Calculates the total height of a lifted card stack in the drag overlay.
 *
 * @param cardCount Number of cards in the dragged stack.
 * @param cardHeight Height of an individual card.
 * @param faceUpPeek Vertical peek distance between consecutive face-up cards.
 * @return Total bounding height of the stack.
 */
fun calculateDragOverlayStackHeight(
    cardCount: Int,
    cardHeight: Dp,
    faceUpPeek: Dp
): Dp {
    if (cardCount <= 0) return 0.dp
    return cardHeight + faceUpPeek * (cardCount - 1)
}

/**
 * Calculates the vertical offset list for all cards in a lifted stack.
 *
 * @param cardCount Number of cards in the dragged stack.
 * @param faceUpPeek Vertical peek distance between consecutive face-up cards.
 * @return List of Dp offsets for each card starting at 0.dp.
 */
fun calculateDragStackOffsets(
    cardCount: Int,
    faceUpPeek: Dp
): List<Dp> {
    if (cardCount <= 0) return emptyList()
    return List(cardCount) { index -> faceUpPeek * index }
}

/**
 * Top-level overlay rendering dragged cards above all board elements.
 *
 * Implements ADR 003: Dedicated Drag Overlay Layer.
 * Renders the moving card stack at [dragDropState.dragPosition] with [DRAG_OVERLAY_ELEVATION] shadow
 * and vertical cascade offsets matching [SolitaireTheme.cardDimensions.faceUpPeek].
 *
 * Uses offset lambda during layout/draw phase to guarantee 60/120 FPS performance
 * without triggering recomposition during drag gestures.
 *
 * @param dragDropState The active [DragDropState] managing gesture coordinates and lifted cards.
 * @param modifier Compose [Modifier] applied to the root overlay [Box].
 */
@Composable
fun DragOverlay(
    dragDropState: DragDropState,
    modifier: Modifier = Modifier
) {
    if (!dragDropState.isDragging || dragDropState.draggedCards.isEmpty()) {
        return
    }

    DragOverlayContent(
        cards = dragDropState.draggedCards,
        dragPosition = {
            IntOffset(
                dragDropState.dragPosition.x.roundToInt(),
                dragDropState.dragPosition.y.roundToInt()
            )
        },
        modifier = modifier
    )
}

/**
 * Pure presentation component for the floating card stack in [DragOverlay].
 *
 * @param cards List of cards being moved (ordered from bottom to top of the lifted stack).
 * @param dragPosition Lambda providing the top-left screen offset of the stack root.
 * @param modifier Compose [Modifier] applied to the overlay container.
 */
@Composable
fun DragOverlayContent(
    cards: List<Card>,
    dragPosition: () -> IntOffset,
    modifier: Modifier = Modifier
) {
    val dimensions = SolitaireTheme.cardDimensions
    val faceUpPeek = dimensions.faceUpPeek
    val totalHeight = calculateDragOverlayStackHeight(cards.size, dimensions.cardHeight, faceUpPeek)
    val offsets = calculateDragStackOffsets(cards.size, faceUpPeek)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { dragPosition() }
                .size(dimensions.cardWidth, totalHeight)
        ) {
            cards.forEachIndexed { index, card ->
                CardView(
                    card = card,
                    elevation = DRAG_OVERLAY_ELEVATION,
                    modifier = Modifier.offset(y = offsets[index])
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Compose Previews
// ---------------------------------------------------------------------------

@Preview(name = "Drag Overlay - Single Floating Card", widthDp = 360, heightDp = 640)
@Composable
private fun DragOverlaySingleCardPreview() {
    SolitaireTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground)
        ) {
            DragOverlayContent(
                cards = listOf(
                    Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
                ),
                dragPosition = { IntOffset(180, 260) }
            )
        }
    }
}

@Preview(name = "Drag Overlay - Cascading Multi-Card Stack", widthDp = 360, heightDp = 640)
@Composable
private fun DragOverlayCardStackPreview() {
    SolitaireTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground)
        ) {
            DragOverlayContent(
                cards = listOf(
                    Card(Suit.SPADES, Rank.TEN, isFaceUp = true),
                    Card(Suit.HEARTS, Rank.NINE, isFaceUp = true),
                    Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true)
                ),
                dragPosition = { IntOffset(140, 320) }
            )
        }
    }
}
