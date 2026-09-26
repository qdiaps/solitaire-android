package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.ui.game.animation.LocalCardFlightState
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.cardDragTarget
import io.github.qdiaps.solitaire.ui.game.gesture.dropTarget
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Renders the Waste (discard pile) slot.
 *
 * Always mounts an empty [CardSlotPlaceholder] at the base.
 * If [underCard] exists, renders it behind [topCard], so when [topCard] is lifted or dragged,
 * either [underCard] or the placeholder is immediately visible without popping.
 *
 * @param topCard The topmost face-up card in the waste pile, or `null` if waste is empty.
 * @param underCard The card directly beneath the top waste card (if any).
 * @param modifier Compose [Modifier] applied to this component.
 * @param isHighlighted Whether to render an active hint border around the waste card.
 * @param boardState Optional board state provider for drag drop validation.
 * @param onClick Optional callback invoked when the top waste card is tapped.
 * @param onCardDropped Optional callback invoked when the waste card is dropped onto a valid target.
 */
@Composable
fun WastePileView(
    topCard: Card?,
    underCard: Card? = null,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    boardState: (() -> BoardState)? = null,
    onClick: (() -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val flightState = LocalCardFlightState.current
    val boundsModifier = modifier.dropTarget(CardLocation.Waste)

    Box(
        modifier = boundsModifier.size(dimensions.cardWidth, dimensions.cardHeight)
    ) {
        CardSlotPlaceholder(
            modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight),
            watermark = SlotWatermark.None
        )

        if (underCard != null) {
            CardView(
                card = underCard,
                modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight)
            )
        }

        if (topCard != null) {
            key(topCard.id) {
                val dragDropState = LocalDragDropState.current
                val isCardDragged = dragDropState != null && dragDropState.isCardDragged(topCard)
                val isCardFlying = flightState != null && flightState.isCardFlying(topCard)
                val isCardHidden = isCardDragged || isCardFlying

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
                    modifier = Modifier
                        .size(dimensions.cardWidth, dimensions.cardHeight)
                        .then(dragModifier)
                        .graphicsLayer {
                            if (isCardHidden) {
                                alpha = 0f
                            }
                        },
                    isHighlighted = isHighlighted,
                    onClick = onClick
                )
            }
        }
    }
}
