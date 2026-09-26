package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.gesture.dropTarget

private val STOCK_FACE_DOWN_CARD = Card(
    suit = Suit.SPADES,
    rank = Rank.KING,
    isFaceUp = false,
    id = "STOCK_PILE_TOP"
)

/**
 * Renders the Stock (draw pile) slot.
 *
 * If [cardsCount] > 0, renders a face-down card indicating cards are available to draw.
 * If [cardsCount] == 0, renders a card slot placeholder with a circular recycle watermark
 * if [canRecycle] is true (cards in waste can be recycled), or an empty slot if recycling
 * is not possible.
 *
 * @param cardsCount Number of cards currently remaining in the stock pile.
 * @param modifier Compose [Modifier] applied to this component.
 * @param canRecycle Whether recycling the waste pile back into the stock is permitted.
 * @param isHighlighted Whether to render an active pulsing hint border around the stock slot.
 * @param onClick Optional callback invoked when the stock pile is tapped.
 */
@Composable
fun StockPileView(
    cardsCount: Int,
    modifier: Modifier = Modifier,
    canRecycle: Boolean = true,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val boundsModifier = modifier.dropTarget(CardLocation.Stock)

    if (cardsCount > 0) {
        CardView(
            card = STOCK_FACE_DOWN_CARD,
            modifier = boundsModifier,
            animateFlip = false,
            isHighlighted = isHighlighted,
            onClick = onClick
        )
    } else {
        CardSlotPlaceholder(
            modifier = boundsModifier,
            watermark = if (canRecycle) SlotWatermark.StockRecycle else SlotWatermark.None,
            isHighlighted = isHighlighted,
            onClick = if (canRecycle) onClick else null
        )
    }
}
