package io.github.qdiaps.solitaire.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.game.components.BottomActionBarView
import io.github.qdiaps.solitaire.ui.game.components.TableauAreaView
import io.github.qdiaps.solitaire.ui.game.components.TopRowView
import io.github.qdiaps.solitaire.ui.game.components.TopStatusBarView
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Root game screen component for Klondike Solitaire.
 *
 * Responsively measures container width with [BoxWithConstraints] and calculates
 * exact card and column proportions via [CardDimensions.calculate].
 *
 * Vertically arranges:
 * 1. [TopStatusBarView] - Score, Moves, and Timer.
 * 2. [TopRowView] - Stock, Waste, and 4 Foundations (supports [isLeftHanded] mirroring).
 * 3. [TableauAreaView] - 7-column playing area with cascading vertical stacks.
 * 4. [BottomActionBarView] - Action controls (Undo, Hint, New Game, Settings).
 *
 * @param boardState Immutable snapshot of the playing cards and counters.
 * @param modifier Compose [Modifier] applied to root container.
 * @param timeSeconds Elapsed play time in seconds.
 * @param canUndo Whether undo action is currently available.
 * @param isLeftHanded When true, mirrors top row placing Foundations on the left and Stock on the right.
 * @param feltTheme Surface cloth theme for the table felt.
 * @param highlightedCard Optional card with active hint highlight.
 * @param isHintActive Whether a hint is currently being displayed.
 * @param onStockClick Callback when Stock draw pile is tapped.
 * @param onWasteClick Callback when Waste card is tapped.
 * @param onFoundationClick Callback when a Foundation pile is tapped with its 0-based index.
 * @param onTableauCardClick Callback when a Tableau card is tapped with its column index and [Card].
 * @param onTableauEmptyClick Callback when an empty Tableau slot is tapped with its column index.
 * @param onUndoClick Callback when Undo button is tapped.
 * @param onHintClick Callback when Hint button is tapped.
 * @param onNewGameClick Callback when New Game button is tapped.
 * @param onSettingsClick Callback when Settings button is tapped.
 */
@Composable
fun SolitaireGameScreen(
    boardState: BoardState,
    modifier: Modifier = Modifier,
    timeSeconds: Long = 0L,
    canUndo: Boolean = false,
    isLeftHanded: Boolean = false,
    feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    highlightedCard: Card? = null,
    isHintActive: Boolean = false,
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onTableauCardClick: (columnIndex: Int, card: Card) -> Unit = { _, _ -> },
    onTableauEmptyClick: (columnIndex: Int) -> Unit = {},
    onUndoClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onNewGameClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SolitaireColors(feltTheme = feltTheme).tableBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val dimensions = remember(maxWidth) {
            CardDimensions.calculate(availableWidth = maxWidth)
        }

        SolitaireTheme(feltTheme = feltTheme, cardDimensions = dimensions) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SolitaireTheme.colors.tableBackground),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TopStatusBarView(
                        boardState = boardState,
                        timeSeconds = timeSeconds
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val isWasteHighlighted = highlightedCard != null && boardState.waste.lastOrNull() == highlightedCard
                    val highlightedFoundationIndex = highlightedCard?.let { card ->
                        boardState.foundations.indexOfFirst { it.lastOrNull() == card }.takeIf { it >= 0 }
                    }

                    TopRowView(
                        boardState = boardState,
                        isLeftHanded = isLeftHanded,
                        isWasteHighlighted = isWasteHighlighted,
                        highlightedFoundationIndex = highlightedFoundationIndex,
                        onStockClick = onStockClick,
                        onWasteClick = onWasteClick,
                        onFoundationClick = onFoundationClick
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TableauAreaView(
                        boardState = boardState,
                        highlightedCard = highlightedCard,
                        onCardClick = onTableauCardClick,
                        onEmptyColumnClick = onTableauEmptyClick
                    )
                }

                BottomActionBarView(
                    canUndo = canUndo,
                    onUndoClick = onUndoClick,
                    onHintClick = onHintClick,
                    onNewGameClick = onNewGameClick,
                    onSettingsClick = onSettingsClick,
                    isHintActive = isHintActive,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Standard alias for [SolitaireGameScreen].
 */
@Composable
fun GameScreen(
    boardState: BoardState,
    modifier: Modifier = Modifier,
    timeSeconds: Long = 0L,
    canUndo: Boolean = false,
    isLeftHanded: Boolean = false,
    feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    highlightedCard: Card? = null,
    isHintActive: Boolean = false,
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onTableauCardClick: (columnIndex: Int, card: Card) -> Unit = { _, _ -> },
    onTableauEmptyClick: (columnIndex: Int) -> Unit = {},
    onUndoClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onNewGameClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    SolitaireGameScreen(
        boardState = boardState,
        modifier = modifier,
        timeSeconds = timeSeconds,
        canUndo = canUndo,
        isLeftHanded = isLeftHanded,
        feltTheme = feltTheme,
        highlightedCard = highlightedCard,
        isHintActive = isHintActive,
        onStockClick = onStockClick,
        onWasteClick = onWasteClick,
        onFoundationClick = onFoundationClick,
        onTableauCardClick = onTableauCardClick,
        onTableauEmptyClick = onTableauEmptyClick,
        onUndoClick = onUndoClick,
        onHintClick = onHintClick,
        onNewGameClick = onNewGameClick,
        onSettingsClick = onSettingsClick
    )
}
