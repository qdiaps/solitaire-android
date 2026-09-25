package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState

/**
 * Renders the Waste (discard pile) slot.
 *
 * If [topCard] is present, displays that face-up card.
 * If [topCard] is null (waste pile is empty), displays an empty [CardSlotPlaceholder].
 *
 * @param topCard The topmost face-up card in the waste pile, or `null` if waste is empty.
 * @param modifier Compose [Modifier] applied to this component.
 * @param isHighlighted Whether to render an active hint border around the waste card.
 * @param onClick Optional callback invoked when the top waste card is tapped.
 */
@Composable
fun WastePileView(
    topCard: Card?,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    if (topCard != null) {
        val dragDropState = LocalDragDropState.current
        val isCardDragged = dragDropState != null && dragDropState.isCardDragged(topCard)

        CardView(
            card = topCard,
            modifier = modifier.graphicsLayer {
                if (isCardDragged) {
                    alpha = 0f
                }
            },
            isHighlighted = isHighlighted,
            onClick = onClick
        )
    } else {
        CardSlotPlaceholder(
            modifier = modifier,
            watermark = SlotWatermark.None
        )
    }
}
