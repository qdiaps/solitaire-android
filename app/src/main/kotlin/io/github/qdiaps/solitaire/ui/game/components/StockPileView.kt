package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

private val STOCK_FACE_DOWN_CARD = Card(
    suit = Suit.SPADES,
    rank = Rank.KING,
    isFaceUp = false
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
 * @param isHighlighted Whether to render an active hint border around the stock slot.
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
    if (cardsCount > 0) {
        CardView(
            card = STOCK_FACE_DOWN_CARD,
            modifier = modifier,
            isHighlighted = isHighlighted,
            onClick = onClick
        )
    } else {
        val dimensions = SolitaireTheme.cardDimensions
        val colors = SolitaireTheme.colors
        val highlightModifier = if (isHighlighted) {
            Modifier.border(
                width = 2.dp,
                color = colors.hintHighlight,
                shape = RoundedCornerShape(dimensions.cornerRadius)
            )
        } else {
            Modifier
        }

        CardSlotPlaceholder(
            modifier = modifier.then(highlightModifier),
            watermark = if (canRecycle) SlotWatermark.StockRecycle else SlotWatermark.None,
            onClick = if (canRecycle) onClick else null
        )
    }
}
