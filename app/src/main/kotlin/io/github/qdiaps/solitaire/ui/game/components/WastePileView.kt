package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.cardDragTarget

/**
 * Renders the Waste (discard pile) slot.
 *
 * If [topCard] is present, displays that face-up card.
 * If [topCard] is null (waste pile is empty), displays an empty [CardSlotPlaceholder].
 *
 * @param topCard The topmost face-up card in the waste pile, or `null` if waste is empty.
 * @param modifier Compose [Modifier] applied to this component.
 * @param isHighlighted Whether to render an active hint border around the waste card.
 * @param boardState Optional board state provider for drag drop validation.
 * @param onClick Optional callback invoked when the top waste card is tapped.
 * @param onCardDropped Optional callback invoked when the waste card is dropped onto a valid target.
 */
@Composable
fun WastePileView(
    topCard: Card?,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    boardState: (() -> BoardState)? = null,
    onClick: (() -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    if (topCard != null) {
        key(topCard.id) {
            val dragDropState = LocalDragDropState.current
            val isCardDragged = dragDropState != null && dragDropState.isCardDragged(topCard)
            val dragModifier = if (topCard.isFaceUp && boardState != null && onCardDropped != null) {
                Modifier.cardDragTarget(
                    isEnabled = true,
                    boardState = boardState,
                    onStartDrag = { origin ->
                        dragDropState?.startWasteDrag(
                            wasteCards = listOf(topCard),
                            originPosition = origin
                        ) == true
                    },
                    onValidDrop = onCardDropped
                )
            } else {
                Modifier
            }

            CardView(
                card = topCard,
                modifier = modifier
                    .then(dragModifier)
                    .graphicsLayer {
                        if (isCardDragged) {
                            alpha = 0f
                        }
                    },
                isHighlighted = isHighlighted,
                onClick = onClick
            )
        }
    } else {
        CardSlotPlaceholder(
            modifier = modifier,
            watermark = SlotWatermark.None
        )
    }
}
