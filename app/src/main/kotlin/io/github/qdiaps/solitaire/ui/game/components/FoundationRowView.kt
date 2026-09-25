package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Suit
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
 * If [topCard] is present, displays that face-up card.
 * If [topCard] is null (foundation is empty), displays an empty [CardSlotPlaceholder]
 * with [defaultSuit] watermark.
 *
 * @param topCard Topmost banked card in this foundation pile, or `null` if empty.
 * @param defaultSuit The suit watermark shown when this foundation slot is empty.
 * @param modifier Compose [Modifier] applied to this slot.
 * @param isHighlighted Whether to render an active hint border around this slot.
 * @param onClick Optional callback invoked when this foundation pile is tapped.
 */
@Composable
fun FoundationPileView(
    topCard: Card?,
    defaultSuit: Suit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    if (topCard != null) {
        CardView(
            card = topCard,
            modifier = modifier,
            isHighlighted = isHighlighted,
            onClick = onClick
        )
    } else {
        CardSlotPlaceholder(
            modifier = modifier,
            watermark = SlotWatermark.FoundationSuit(defaultSuit),
            onClick = onClick
        )
    }
}

/**
 * Renders the row of four Foundation piles (0..3) side-by-side.
 *
 * @param foundations List of 4 card piles representing the foundations.
 * @param modifier Compose [Modifier] applied to this row.
 * @param defaultSuits Suits assigned to empty foundation slots (defaults to [DEFAULT_FOUNDATION_SUITS]).
 * @param highlightedFoundationIndex Optional index (0..3) of foundation pile highlighted by a hint.
 * @param onFoundationClick Optional callback invoked with the index of the tapped foundation pile.
 */
@Composable
fun FoundationRowView(
    foundations: List<List<Card>>,
    modifier: Modifier = Modifier,
    defaultSuits: List<Suit> = DEFAULT_FOUNDATION_SUITS,
    highlightedFoundationIndex: Int? = null,
    onFoundationClick: ((foundationIndex: Int) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)
    ) {
        for (index in 0 until 4) {
            val pile = foundations.getOrNull(index).orEmpty()
            val topCard = pile.lastOrNull()
            val defaultSuit = defaultSuits.getOrElse(index) { Suit.HEARTS }

            FoundationPileView(
                topCard = topCard,
                defaultSuit = defaultSuit,
                isHighlighted = highlightedFoundationIndex == index,
                onClick = onFoundationClick?.let { { it(index) } }
            )
        }
    }
}
