package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.game.animation.LocalCardFlightState
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.cardDragTarget
import io.github.qdiaps.solitaire.ui.game.gesture.dropTarget
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Calculates horizontal offsets for cards visible in the Waste pile.
 *
 * In [DrawMode.DRAW_ONE], at most 1 card is displayed at 0.dp.
 * In [DrawMode.DRAW_THREE], up to 3 cards are fanned horizontally.
 * When [isLeftHanded] is true, cards fan to the left (negative offsets).
 * Otherwise, cards fan to the right (positive offsets).
 *
 * @param visibleCardsCount The number of cards currently visible in the waste fan (0..3).
 * @param isLeftHanded Whether left-handed layout mirroring is active.
 * @param fanOffset The distance between adjacent fanned cards.
 * @return List of horizontal [Dp] offsets for each visible card from bottom to top.
 */
fun calculateWasteOffsets(
    visibleCardsCount: Int,
    isLeftHanded: Boolean = false,
    fanOffset: Dp
): List<Dp> {
    if (visibleCardsCount <= 0) return emptyList()
    val sign = if (isLeftHanded) -1 else 1
    return List(visibleCardsCount) { index ->
        if (index == 0) 0.dp else fanOffset * (index * sign)
    }
}

/**
 * Returns the cards from the waste pile that should be displayed on screen.
 * In [DrawMode.DRAW_THREE], up to 3 cards are displayed fanned out horizontally.
 * In [DrawMode.DRAW_ONE], only the topmost card is displayed.
 */
fun getVisibleWasteCards(
    wasteCards: List<Card>,
    drawMode: DrawMode = DrawMode.DRAW_ONE
): List<Card> {
    val maxVisible = if (drawMode == DrawMode.DRAW_THREE) 3 else 1
    return wasteCards.takeLast(maxVisible)
}

/**
 * Renders the Waste (discard pile) slot.
 *
 * In [DrawMode.DRAW_ONE], renders the topmost card with an optional undercard.
 * In [DrawMode.DRAW_THREE], renders up to 3 cards fanned horizontally.
 * When [isLeftHanded] is true, cards fan towards the left into the center spacer.
 *
 * Always mounts an empty [CardSlotPlaceholder] at the base.
 * If cards exist beneath the visible fan, renders the top undercard at offset 0
 * to prevent visual pop when cards are lifted or dragged.
 *
 * @param wasteCards All cards currently in the waste pile.
 * @param modifier Compose [Modifier] applied to this component.
 * @param drawMode Current [DrawMode] (Draw 1 or Draw 3).
 * @param isLeftHanded Whether left-handed mirroring is enabled.
 * @param isHighlighted Whether to render an active hint border around the playable waste card.
 * @param boardState Optional board state provider for drag drop validation.
 * @param onClick Optional callback invoked when the waste pile or top card is tapped.
 * @param onCardDropped Optional callback invoked when the top waste card is dropped onto a valid target.
 */
@Composable
fun WastePileView(
    wasteCards: List<Card>,
    modifier: Modifier = Modifier,
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    isLeftHanded: Boolean = false,
    isHighlighted: Boolean = false,
    boardState: (() -> BoardState)? = null,
    onClick: (() -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val flightState = LocalCardFlightState.current
    val boundsModifier = modifier.dropTarget(CardLocation.Waste)

    val visibleCards = getVisibleWasteCards(wasteCards, drawMode)
    val fanOffset = (dimensions.cardWidth * 0.32f).coerceIn(14.dp, 20.dp)
    val offsets = calculateWasteOffsets(
        visibleCardsCount = visibleCards.size,
        isLeftHanded = isLeftHanded,
        fanOffset = fanOffset
    )

    val underCard = if (wasteCards.size > visibleCards.size) {
        wasteCards[wasteCards.size - visibleCards.size - 1]
    } else {
        null
    }

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
                modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight),
                animateFlip = false
            )
        }

        visibleCards.forEachIndexed { index, card ->
            val isTopCard = index == visibleCards.lastIndex
            val cardOffset = offsets.getOrElse(index) { 0.dp }

            key(card.id) {
                val dragDropState = LocalDragDropState.current
                val isCardDragged = dragDropState != null && dragDropState.isCardDragged(card)
                val isCardFlying = flightState != null && flightState.isCardFlying(card)
                val isCardHidden = isCardDragged || isCardFlying

                val dragModifier = if (isTopCard && card.isFaceUp && boardState != null && onCardDropped != null) {
                    Modifier.cardDragTarget(
                        isEnabled = true,
                        boardState = boardState,
                        onStartDrag = { origin ->
                            dragDropState?.startWasteDrag(
                                wasteCards = listOf(card),
                                originPosition = origin
                            ) == true
                        },
                        onValidDrop = onCardDropped
                    )
                } else {
                    Modifier
                }

                CardView(
                    card = card,
                    modifier = Modifier
                        .offset(x = cardOffset)
                        .size(dimensions.cardWidth, dimensions.cardHeight)
                        .then(dragModifier)
                        .graphicsLayer {
                            if (isCardHidden) {
                                alpha = 0f
                            }
                        },
                    animateFlip = false,
                    isHighlighted = isTopCard && isHighlighted,
                    onClick = onClick
                )
            }
        }
    }
}

/**
 * Backward-compatible overload accepting [topCard] and [underCard] directly.
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
    val cards = listOfNotNull(underCard, topCard)
    WastePileView(
        wasteCards = cards,
        modifier = modifier,
        drawMode = DrawMode.DRAW_ONE,
        isLeftHanded = false,
        isHighlighted = isHighlighted,
        boardState = boardState,
        onClick = onClick,
        onCardDropped = onCardDropped
    )
}
