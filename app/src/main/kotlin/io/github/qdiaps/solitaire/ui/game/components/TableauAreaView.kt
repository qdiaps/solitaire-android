package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.ui.game.gesture.dropTarget
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Total number of tableau columns in standard Klondike Solitaire.
 */
const val NUM_TABLEAU_COLUMNS: Int = KlondikeDealer.TABLEAU_COLUMNS_COUNT

/**
 * Renders the 7 tableau columns side-by-side.
 *
 * Each column is aligned with the 7-column grid geometry defined in [SolitaireTheme.cardDimensions].
 * Columns are separated by [dimensions.columnSpacing] and bounded on the left and right by
 * [dimensions.horizontalPadding], matching the horizontal alignment of the top row.
 *
 * @param tableau List of 7 card columns representing the tableau.
 * @param modifier Compose [Modifier] applied to this container.
 * @param highlightedCard Optional source card with active hint highlight.
 * @param destinationCard Optional destination card with active hint highlight.
 * @param highlightedEmptyColumnIndex Optional 0-based column index of an empty column highlighted as a hint destination.
 * @param boardState Optional board state provider for drag drop validation.
 * @param onCardClick Optional callback invoked when a card in a tableau column is tapped,
 * providing the 0-based column index and the tapped [Card].
 * @param onEmptyColumnClick Optional callback invoked when an empty tableau column slot is tapped,
 * providing the 0-based column index.
 * @param onCardDropped Optional callback invoked when a card stack is dropped onto a valid target.
 */
@Composable
fun TableauAreaView(
    tableau: List<List<Card>>,
    modifier: Modifier = Modifier,
    highlightedCard: Card? = null,
    highlightedCards: List<Card> = emptyList(),
    destinationCard: Card? = null,
    highlightedEmptyColumnIndex: Int? = null,
    boardState: (() -> BoardState)? = null,
    onCardClick: ((columnIndex: Int, card: Card) -> Unit)? = null,
    onEmptyColumnClick: ((columnIndex: Int) -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions

    Row(
        modifier = modifier.padding(horizontal = dimensions.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing),
        verticalAlignment = Alignment.Top
    ) {
        for (columnIndex in 0 until NUM_TABLEAU_COLUMNS) {
            val columnCards = tableau.getOrNull(columnIndex).orEmpty()
            TableauColumnView(
                cards = columnCards,
                modifier = Modifier.dropTarget(CardLocation.Tableau(columnIndex)),
                highlightedCard = highlightedCard,
                highlightedCards = highlightedCards,
                destinationCard = destinationCard,
                isSlotHighlighted = columnIndex == highlightedEmptyColumnIndex,
                columnIndex = columnIndex,
                boardState = boardState,
                onCardClick = onCardClick?.let { callback ->
                    { card -> callback(columnIndex, card) }
                },
                onEmptySlotClick = onEmptyColumnClick?.let { callback ->
                    { callback(columnIndex) }
                },
                onCardDropped = onCardDropped
            )
        }
    }
}

/**
 * Convenience overload of [TableauAreaView] taking a domain [BoardState] snapshot directly.
 */
@Composable
fun TableauAreaView(
    boardState: BoardState,
    modifier: Modifier = Modifier,
    highlightedCard: Card? = null,
    highlightedCards: List<Card> = emptyList(),
    destinationCard: Card? = null,
    highlightedEmptyColumnIndex: Int? = null,
    onCardClick: ((columnIndex: Int, card: Card) -> Unit)? = null,
    onEmptyColumnClick: ((columnIndex: Int) -> Unit)? = null,
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    TableauAreaView(
        tableau = boardState.tableau,
        modifier = modifier,
        highlightedCard = highlightedCard,
        highlightedCards = highlightedCards,
        destinationCard = destinationCard,
        highlightedEmptyColumnIndex = highlightedEmptyColumnIndex,
        boardState = { boardState },
        onCardClick = onCardClick,
        onEmptyColumnClick = onEmptyColumnClick,
        onCardDropped = onCardDropped
    )
}
