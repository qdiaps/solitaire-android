package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.animation.LocalCardFlightState
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.cardDragTarget
import io.github.qdiaps.solitaire.ui.game.gesture.dropTarget
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Default order of French suits assigned to foundation pile slots (0..3).
 */
val DEFAULT_FOUNDATION_SUITS: List<Suit> = listOf(
    Suit.HEARTS,
    Suit.DIAMONDS,
    Suit.CLUBS,
    Suit.SPADES
)

/**
 * Renders an individual Foundation pile slot.
 *
 * Always mounts an empty [CardSlotPlaceholder] with [defaultSuit] watermark at the base.
 * If [underCard] exists, renders it behind [topCard], so when [topCard] is lifted or dragged,
 * either [underCard] or the suit placeholder is immediately visible without popping.
 *
 * @param topCard Topmost banked card in this foundation pile, or `null` if empty.
 * @param underCard Card immediately beneath the top card in this foundation pile (if any).
 * @param defaultSuit The suit watermark shown when this foundation slot is empty.
 * @param modifier Compose [Modifier] applied to this slot.
 * @param isHighlighted Whether to render an active hint border around this slot.
 * @param foundationIndex 0-based index of this foundation pile (0..3).
 * @param boardState Optional board state provider for drag drop validation.
 * @param onClick Optional callback invoked when this foundation pile is tapped.
 * @param onCardDropped Optional callback invoked when this foundation card is dropped onto a valid target.
 */
@Composable
fun FoundationPileView(
    topCard: Card?,
    underCard: Card? = null,
    defaultSuit: Suit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    foundationIndex: Int = 0,
    boardState: (() -> BoardState)? = null,
    onClick: (() -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val flightState = LocalCardFlightState.current

    Box(
        modifier = modifier.size(dimensions.cardWidth, dimensions.cardHeight)
    ) {
        CardSlotPlaceholder(
            modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight),
            watermark = SlotWatermark.FoundationSuit(defaultSuit),
            isHighlighted = if (topCard == null) isHighlighted else false,
            onClick = if (topCard == null) onClick else null
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
                            dragDropState?.startFoundationDrag(
                                foundationIndex = foundationIndex,
                                foundationCards = listOf(topCard),
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

/**
 * Renders the row of four Foundation piles (0..3) side-by-side.
 *
 * @param foundations List of 4 card piles representing the foundations.
 * @param modifier Compose [Modifier] applied to this row.
 * @param defaultSuits Suits assigned to empty foundation slots (defaults to [DEFAULT_FOUNDATION_SUITS]).
 * @param highlightedFoundationIndex Optional index (0..3) of foundation pile highlighted by a hint.
 * @param boardState Optional board state provider for drag drop validation.
 * @param onFoundationClick Optional callback invoked with the index of the tapped foundation pile.
 * @param onCardDropped Optional callback invoked when a foundation card is dropped onto a valid target.
 */
@Composable
fun FoundationRowView(
    foundations: List<List<Card>>,
    modifier: Modifier = Modifier,
    defaultSuits: List<Suit> = DEFAULT_FOUNDATION_SUITS,
    highlightedFoundationIndex: Int? = null,
    boardState: (() -> BoardState)? = null,
    onFoundationClick: ((foundationIndex: Int) -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)
    ) {
        for (index in 0 until 4) {
            val pile = foundations.getOrNull(index).orEmpty()
            val topCard = pile.lastOrNull()
            val underCard = if (pile.size >= 2) pile[pile.size - 2] else null
            val defaultSuit = defaultSuits.getOrElse(index) { Suit.HEARTS }

            FoundationPileView(
                topCard = topCard,
                underCard = underCard,
                defaultSuit = defaultSuit,
                modifier = Modifier.dropTarget(CardLocation.Foundation(index)),
                isHighlighted = highlightedFoundationIndex == index,
                foundationIndex = index,
                boardState = boardState,
                onClick = onFoundationClick?.let { { it(index) } },
                onCardDropped = onCardDropped
            )
        }
    }
}
