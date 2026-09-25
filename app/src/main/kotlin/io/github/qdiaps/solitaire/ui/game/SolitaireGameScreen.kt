package io.github.qdiaps.solitaire.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.SmartTapResolver
import io.github.qdiaps.solitaire.ui.game.animation.AnimatedMoveOverlay
import io.github.qdiaps.solitaire.ui.game.animation.LocalCardFlightState
import io.github.qdiaps.solitaire.ui.game.animation.rememberCardFlightState
import io.github.qdiaps.solitaire.ui.game.components.BottomActionBarView
import io.github.qdiaps.solitaire.ui.game.components.DragOverlay
import io.github.qdiaps.solitaire.ui.game.components.TableauAreaView
import io.github.qdiaps.solitaire.ui.game.components.TopRowView
import io.github.qdiaps.solitaire.ui.game.components.TopStatusBarView
import io.github.qdiaps.solitaire.ui.game.components.calculateTableauOffsets
import io.github.qdiaps.solitaire.ui.game.gesture.DropTargetRegistry
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.LocalDropTargetRegistry
import io.github.qdiaps.solitaire.ui.game.gesture.LocalSolitaireHaptics
import io.github.qdiaps.solitaire.ui.game.gesture.rememberDragDropState
import io.github.qdiaps.solitaire.ui.game.gesture.rememberDropTargetRegistry
import io.github.qdiaps.solitaire.ui.game.gesture.rememberSolitaireHaptics
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlinx.coroutines.launch

/**
 * Root game screen component for Klondike Solitaire.
 *
 * Responsively measures container width with [BoxWithConstraints] and calculates
 * exact card and column proportions via [CardDimensions.calculate].
 *
 * Sets up drag-and-drop state via [LocalDragDropState] and [LocalDropTargetRegistry],
 * rendering the [DragOverlay] and [AnimatedMoveOverlay] floating layers directly in root coordinates
 * above all other board elements (ADR 003).
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
 * @param onCardDropped Optional callback invoked when a card or stack is legally dropped on a target.
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
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null,
    onUndoClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onNewGameClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val dragDropState = rememberDragDropState()
    val dropTargetRegistry = rememberDropTargetRegistry()
    val solitaireHaptics = rememberSolitaireHaptics()
    val cardFlightState = rememberCardFlightState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDragDropState provides dragDropState,
        LocalDropTargetRegistry provides dropTargetRegistry,
        LocalSolitaireHaptics provides solitaireHaptics,
        LocalCardFlightState provides cardFlightState
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(SolitaireColors(feltTheme = feltTheme).tableBackground)
        ) {
            val dimensions = remember(maxWidth) {
                CardDimensions.calculate(availableWidth = maxWidth)
            }

            // Animated Smart Tap and Stock Draw handlers
            val animatedStockClick: () -> Unit = {
                if (cardFlightState.activeFlight == null && boardState.stock.isNotEmpty()) {
                    val stockBounds = dropTargetRegistry.getBounds(CardLocation.Stock)
                    val wasteBounds = dropTargetRegistry.getBounds(CardLocation.Waste)

                    if (stockBounds != null && wasteBounds != null) {
                        val movingCard = boardState.stock.last()
                        coroutineScope.launch {
                            cardFlightState.startFlight(
                                cards = listOf(movingCard),
                                startOffset = stockBounds.topLeft,
                                targetOffset = wasteBounds.topLeft,
                                isStockFlip = true,
                                durationMillis = 180
                            ) {
                                onStockClick()
                            }
                        }
                    } else {
                        onStockClick()
                    }
                } else {
                    onStockClick()
                }
            }

            val animatedWasteClick: () -> Unit = {
                if (cardFlightState.activeFlight == null && boardState.waste.isNotEmpty()) {
                    val wasteCard = boardState.waste.last()
                    val move = SmartTapResolver.resolveMove(boardState, CardLocation.Waste)
                    val wasteBounds = dropTargetRegistry.getBounds(CardLocation.Waste)

                    if (move != null && wasteBounds != null) {
                        val targetOffset = calculateFlightTargetOffset(
                            target = move.to,
                            boardState = boardState,
                            registry = dropTargetRegistry,
                            dimensions = dimensions,
                            density = density
                        )

                        if (targetOffset != null) {
                            coroutineScope.launch {
                                cardFlightState.startFlight(
                                    cards = listOf(wasteCard),
                                    startOffset = wasteBounds.topLeft,
                                    targetOffset = targetOffset,
                                    durationMillis = 180
                                ) {
                                    onWasteClick()
                                }
                            }
                        } else {
                            onWasteClick()
                        }
                    } else {
                        onWasteClick()
                    }
                } else {
                    onWasteClick()
                }
            }

            val animatedTableauCardClick: (columnIndex: Int, card: Card) -> Unit = { columnIndex, card ->
                if (cardFlightState.activeFlight == null) {
                    val column = boardState.tableau.getOrNull(columnIndex).orEmpty()
                    val cardIndex = column.indexOf(card)

                    if (cardIndex >= 0 && card.isFaceUp) {
                        val source = CardLocation.Tableau(columnIndex, cardIndex)
                        val move = SmartTapResolver.resolveMove(boardState, source)
                        val colBounds = dropTargetRegistry.getBounds(CardLocation.Tableau(columnIndex))

                        if (move != null && colBounds != null) {
                            val movingCards = column.subList(cardIndex, column.size)
                            val yOffsets = calculateTableauOffsets(
                                cards = column,
                                faceDownPeek = dimensions.faceDownPeek,
                                faceUpPeek = dimensions.faceUpPeek
                            )
                            val cardTopPx = with(density) { yOffsets[cardIndex].toPx() }
                            val startOffset = Offset(colBounds.left, colBounds.top + cardTopPx)
                            val targetOffset = calculateFlightTargetOffset(
                                target = move.to,
                                boardState = boardState,
                                registry = dropTargetRegistry,
                                dimensions = dimensions,
                                density = density
                            )

                            if (targetOffset != null) {
                                coroutineScope.launch {
                                    cardFlightState.startFlight(
                                        cards = movingCards,
                                        startOffset = startOffset,
                                        targetOffset = targetOffset,
                                        durationMillis = 180
                                    ) {
                                        onTableauCardClick(columnIndex, card)
                                    }
                                }
                            } else {
                                onTableauCardClick(columnIndex, card)
                            }
                        } else {
                            onTableauCardClick(columnIndex, card)
                        }
                    } else {
                        onTableauCardClick(columnIndex, card)
                    }
                } else {
                    onTableauCardClick(columnIndex, card)
                }
            }

            SolitaireTheme(feltTheme = feltTheme, cardDimensions = dimensions) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SolitaireTheme.colors.tableBackground)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
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
                                onStockClick = animatedStockClick,
                                onWasteClick = animatedWasteClick,
                                onFoundationClick = onFoundationClick,
                                onCardDropped = onCardDropped
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            TableauAreaView(
                                boardState = boardState,
                                highlightedCard = highlightedCard,
                                onCardClick = animatedTableauCardClick,
                                onEmptyColumnClick = onTableauEmptyClick,
                                onCardDropped = onCardDropped
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

                    // Floating animated card flight overlay (Smart Tap & Stock Flip)
                    AnimatedMoveOverlay(flightState = cardFlightState)

                    // Floating drag-and-drop overlay layer in root window coordinates (ADR 003)
                    DragOverlay(dragDropState = dragDropState)
                }
            }
        }
    }
}

/**
 * Calculates absolute root screen coordinates for destination slot [target].
 */
private fun calculateFlightTargetOffset(
    target: CardLocation,
    boardState: BoardState,
    registry: DropTargetRegistry,
    dimensions: CardDimensions,
    density: Density
): Offset? {
    return when (target) {
        is CardLocation.Foundation -> {
            registry.getBounds(target)?.topLeft
        }
        is CardLocation.Tableau -> {
            val colBounds = registry.getBounds(CardLocation.Tableau(target.columnIndex)) ?: return null
            val colCards = boardState.tableau.getOrNull(target.columnIndex).orEmpty()
            if (colCards.isEmpty()) {
                colBounds.topLeft
            } else {
                val yOffsets = calculateTableauOffsets(
                    cards = colCards,
                    faceDownPeek = dimensions.faceDownPeek,
                    faceUpPeek = dimensions.faceUpPeek
                )
                val targetTopPx = with(density) {
                    (yOffsets.last() + dimensions.faceUpPeek).toPx()
                }
                Offset(colBounds.left, colBounds.top + targetTopPx)
            }
        }
        is CardLocation.Waste -> {
            registry.getBounds(CardLocation.Waste)?.topLeft
        }
        else -> null
    }
}

/**
 * Stateful entry point for [SolitaireGameScreen] connected directly to [GameViewModel].
 *
 * Collects [GameViewModel.uiState] and routes user actions as [GameIntent]s to the ViewModel.
 * Observes single-shot [GameEvent]s to trigger tactile haptic feedback.
 *
 * @param viewModel Presentation [GameViewModel] orchestrating game state.
 * @param modifier Compose [Modifier] applied to root container.
 */
@Composable
fun SolitaireGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val solitaireHaptics = rememberSolitaireHaptics()

    LaunchedEffect(viewModel, solitaireHaptics) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.PlayHapticTick -> {
                    solitaireHaptics.playTick()
                }
                is GameEvent.PlayHapticSnap -> {
                    solitaireHaptics.playSnap()
                }
                is GameEvent.TriggerWinCelebration -> {
                    solitaireHaptics.playSnap()
                }
                is GameEvent.ShowMessage -> {
                    // Message snackbar / banner
                }
            }
        }
    }

    SolitaireGameScreen(
        boardState = uiState.boardState,
        modifier = modifier,
        timeSeconds = uiState.elapsedTimeSeconds,
        canUndo = uiState.canUndo,
        isLeftHanded = uiState.isLeftHanded,
        feltTheme = uiState.feltTheme,
        highlightedCard = uiState.highlightedCard,
        isHintActive = uiState.isHintActive,
        onStockClick = { viewModel.onIntent(GameIntent.DrawStockCard) },
        onWasteClick = {
            uiState.boardState.waste.lastOrNull()?.let { card ->
                viewModel.onIntent(GameIntent.OnCardTapped(card, CardLocation.Waste))
            }
        },
        onFoundationClick = { foundationIndex ->
            uiState.boardState.foundations.getOrNull(foundationIndex)?.lastOrNull()?.let { card ->
                viewModel.onIntent(GameIntent.OnCardTapped(card, CardLocation.Foundation(foundationIndex)))
            }
        },
        onTableauCardClick = { columnIndex, card ->
            val cardIndex = uiState.boardState.tableau.getOrNull(columnIndex)?.indexOf(card) ?: -1
            if (cardIndex >= 0) {
                viewModel.onIntent(GameIntent.OnCardTapped(card, CardLocation.Tableau(columnIndex, cardIndex)))
            }
        },
        onTableauEmptyClick = { /* No-op or smart tap king if desired */ },
        onCardDropped = { cards, source, target ->
            viewModel.onIntent(GameIntent.OnCardDropped(cards, source, target))
        },
        onUndoClick = { viewModel.onIntent(GameIntent.UndoMove) },
        onHintClick = { viewModel.onIntent(GameIntent.RequestHint) },
        onNewGameClick = { viewModel.onIntent(GameIntent.StartNewGame) },
        onSettingsClick = { /* Settings sheet */ }
    )
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
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null,
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
        onCardDropped = onCardDropped,
        onUndoClick = onUndoClick,
        onHintClick = onHintClick,
        onNewGameClick = onNewGameClick,
        onSettingsClick = onSettingsClick
    )
}

/**
 * Standard alias for stateful [SolitaireGameScreen].
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    SolitaireGameScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}
