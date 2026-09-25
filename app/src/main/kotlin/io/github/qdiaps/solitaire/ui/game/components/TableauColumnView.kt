package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Calculates vertical top offsets for each card in a tableau column cascade.
 *
 * Each card is offset by [faceDownPeek] if the card below it is face-down,
 * or [faceUpPeek] if the card below it is face-up.
 *
 * @param cards List of cards from bottom (index 0) to top (last index).
 * @param faceDownPeek Vertical visible strip for face-down cards.
 * @param faceUpPeek Vertical visible strip for face-up cards.
 * @return List of vertical [Dp] offsets matching the indices of [cards].
 */
fun calculateTableauOffsets(
    cards: List<Card>,
    faceDownPeek: Dp,
    faceUpPeek: Dp
): List<Dp> {
    if (cards.isEmpty()) return emptyList()

    val offsets = ArrayList<Dp>(cards.size)
    var currentY = 0.dp

    for (index in cards.indices) {
        offsets.add(currentY)
        if (index < cards.size - 1) {
            val card = cards[index]
            currentY += if (card.isFaceUp) faceUpPeek else faceDownPeek
        }
    }

    return offsets
}

/**
 * Renders a single vertical Tableau column.
 *
 * If [cards] is empty, renders an empty [CardSlotPlaceholder] with a King watermark (`TableauKing`).
 * If [cards] is non-empty, renders all cards vertically stacked with distinct peek offsets
 * for face-down and face-up cards.
 *
 * @param cards List of cards in this column from bottom (index 0) to top (last index).
 * @param modifier Compose [Modifier] applied to this column container.
 * @param highlightedCard Optional card within this column that has an active hint highlight.
 * @param onCardClick Optional callback invoked when a card in this column is tapped.
 * @param onEmptySlotClick Optional callback invoked when the empty column slot is tapped.
 */
@Composable
fun TableauColumnView(
    cards: List<Card>,
    modifier: Modifier = Modifier,
    highlightedCard: Card? = null,
    onCardClick: ((card: Card) -> Unit)? = null,
    onEmptySlotClick: (() -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions

    if (cards.isEmpty()) {
        CardSlotPlaceholder(
            modifier = modifier,
            watermark = SlotWatermark.TableauKing,
            onClick = onEmptySlotClick
        )
    } else {
        val yOffsets = remember(cards, dimensions.faceDownPeek, dimensions.faceUpPeek) {
            calculateTableauOffsets(
                cards = cards,
                faceDownPeek = dimensions.faceDownPeek,
                faceUpPeek = dimensions.faceUpPeek
            )
        }

        val totalHeight = yOffsets.last() + dimensions.cardHeight

        Box(
            modifier = modifier.size(dimensions.cardWidth, totalHeight)
        ) {
            cards.forEachIndexed { index, card ->
                CardView(
                    card = card,
                    modifier = Modifier.offset(y = yOffsets[index]),
                    isHighlighted = card == highlightedCard,
                    onClick = onCardClick?.let { { it(card) } }
                )
            }
        }
    }
}
