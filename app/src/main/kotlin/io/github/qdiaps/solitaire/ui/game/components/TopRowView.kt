package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Top row of the Solitaire board containing the Stock pile, Waste pile,
 * a single-column spacer, and the four Foundation piles.
 *
 * Supports left-handed mode mirroring:
 * - Standard: `[Stock] [Waste] [Spacer] [Foundation 0..3]`
 * - Left-handed: `[Foundation 0..3] [Spacer] [Waste] [Stock]`
 *
 * @param stockCount Number of cards remaining in the stock pile.
 * @param wasteTopCard The topmost card in the waste pile, or `null` if empty.
 * @param wasteUnderCard The card directly under the top waste card, or `null` if <= 1 card in waste.
 * @param foundations List of 4 foundation card piles.
 * @param modifier Compose [Modifier] applied to the top row.
 * @param isLeftHanded Whether left-handed mode layout mirroring is active.
 * @param canRecycle Whether waste can be recycled into stock when stock is empty.
 * @param isStockHighlighted Whether stock slot has active hint highlight.
 * @param isWasteHighlighted Whether waste card has active hint highlight.
 * @param highlightedFoundationIndex Optional index (0..3) of foundation with active hint highlight.
 * @param boardState Optional board state provider for drag drop validation.
 * @param wasteCards All cards currently in the waste pile.
 * @param drawMode Current [DrawMode] (Draw 1 or Draw 3).
 * @param onStockClick Callback when stock pile is tapped.
 * @param onWasteClick Callback when waste pile is tapped.
 * @param onFoundationClick Callback when a foundation pile is tapped with its 0-based index.
 * @param onCardDropped Optional callback invoked when a card is dropped onto a valid target.
 */
@Composable
fun TopRowView(
    stockCount: Int,
    wasteTopCard: Card?,
    wasteUnderCard: Card? = null,
    foundations: List<List<Card>>,
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false,
    canRecycle: Boolean = true,
    isStockHighlighted: Boolean = false,
    isWasteHighlighted: Boolean = false,
    highlightedFoundationIndex: Int? = null,
    boardState: (() -> BoardState)? = null,
    wasteCards: List<Card> = listOfNotNull(wasteUnderCard, wasteTopCard),
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions

    Row(
        modifier = modifier.padding(horizontal = dimensions.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLeftHanded) {
            StockPileView(
                cardsCount = stockCount,
                canRecycle = canRecycle,
                isHighlighted = isStockHighlighted,
                onClick = onStockClick
            )
            WastePileView(
                wasteCards = wasteCards,
                drawMode = drawMode,
                isLeftHanded = isLeftHanded,
                isHighlighted = isWasteHighlighted,
                boardState = boardState,
                onClick = onWasteClick,
                onCardDropped = onCardDropped
            )
            Spacer(modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight))
            FoundationRowView(
                foundations = foundations,
                highlightedFoundationIndex = highlightedFoundationIndex,
                boardState = boardState,
                onFoundationClick = onFoundationClick,
                onCardDropped = onCardDropped
            )
        } else {
            FoundationRowView(
                foundations = foundations,
                highlightedFoundationIndex = highlightedFoundationIndex,
                boardState = boardState,
                onFoundationClick = onFoundationClick,
                onCardDropped = onCardDropped
            )
            Spacer(modifier = Modifier.size(dimensions.cardWidth, dimensions.cardHeight))
            WastePileView(
                wasteCards = wasteCards,
                drawMode = drawMode,
                isLeftHanded = isLeftHanded,
                isHighlighted = isWasteHighlighted,
                boardState = boardState,
                onClick = onWasteClick,
                onCardDropped = onCardDropped
            )
            StockPileView(
                cardsCount = stockCount,
                canRecycle = canRecycle,
                isHighlighted = isStockHighlighted,
                onClick = onStockClick
            )
        }
    }
}

/**
 * Convenience overload of [TopRowView] taking a domain [BoardState] snapshot directly.
 */
@Composable
fun TopRowView(
    boardState: BoardState,
    modifier: Modifier = Modifier,
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    isLeftHanded: Boolean = false,
    isStockHighlighted: Boolean = false,
    isWasteHighlighted: Boolean = false,
    highlightedFoundationIndex: Int? = null,
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null
) {
    TopRowView(
        stockCount = boardState.stock.size,
        wasteTopCard = boardState.waste.lastOrNull(),
        wasteUnderCard = if (boardState.waste.size >= 2) boardState.waste[boardState.waste.size - 2] else null,
        foundations = boardState.foundations,
        canRecycle = boardState.stock.isEmpty() && boardState.waste.isNotEmpty(),
        modifier = modifier,
        isLeftHanded = isLeftHanded,
        isStockHighlighted = isStockHighlighted,
        isWasteHighlighted = isWasteHighlighted,
        highlightedFoundationIndex = highlightedFoundationIndex,
        boardState = { boardState },
        wasteCards = boardState.waste,
        drawMode = drawMode,
        onStockClick = onStockClick,
        onWasteClick = onWasteClick,
        onFoundationClick = onFoundationClick,
        onCardDropped = onCardDropped
    )
}
