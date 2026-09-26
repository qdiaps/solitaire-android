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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteMove
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteResolver
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.SmartTapResolver
import io.github.qdiaps.solitaire.ui.game.animation.AnimatedMoveOverlay
import io.github.qdiaps.solitaire.ui.game.animation.LocalCardFlightState
import io.github.qdiaps.solitaire.ui.game.animation.rememberCardFlightState
import io.github.qdiaps.solitaire.ui.game.audio.LocalSolitaireAudio
import io.github.qdiaps.solitaire.ui.game.audio.rememberSolitaireAudio
import io.github.qdiaps.solitaire.ui.game.components.AutoCompleteBannerView
import io.github.qdiaps.solitaire.ui.game.components.BottomActionBarView
import io.github.qdiaps.solitaire.data.model.GameStats
import io.github.qdiaps.solitaire.ui.game.components.SettingsBottomSheet
import io.github.qdiaps.solitaire.ui.game.components.StatsDialog
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.game.components.LocalGameSessionId
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
import kotlinx.coroutines.isActive
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
 * @param highlightedCard Optional source card with active hint highlight.
 * @param isHintActive Whether a hint is currently being displayed.
 * @param hintSourceLocation Optional source location of active hint.
 * @param hintTargetLocation Optional destination location of active hint.
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
    highlightedCards: List<Card> = emptyList(),
    isHintActive: Boolean = false,
    hintSourceLocation: CardLocation? = null,
    hintTargetLocation: CardLocation? = null,
    gameSessionId: Long = 1L,
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    cardBackStyle: CardBackStyle = CardBackStyle.DEFAULT,
    cardFaceStyle: CardFaceStyle = CardFaceStyle.DEFAULT,
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    autoHintEnabled: Boolean = false,
    isSettingsOpen: Boolean = false,
    isAutoCompleteAvailable: Boolean = false,
    isAutoCompleting: Boolean = false,
    isGameWon: Boolean = false,
    onAutoCompleteClick: () -> Unit = {},
    onAutoCompleteStep: (AutoCompleteMove) -> Unit = {},
    onAutoCompleteFinished: () -> Unit = {},
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onTableauCardClick: (columnIndex: Int, card: Card) -> Unit = { _, _ -> },
    onTableauEmptyClick: (columnIndex: Int) -> Unit = {},
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null,
    onUndoClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onNewGameClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    isStatsDialogOpen: Boolean = false,
    stats: GameStats = GameStats(),
    onStatsClick: () -> Unit = {},
    onDismissStats: () -> Unit = {},
    onResetStats: () -> Unit = {},
    onDrawModeChange: (DrawMode) -> Unit = {},
    onLeftHandedChange: (Boolean) -> Unit = {},
    onAutoHintChange: (Boolean) -> Unit = {},
    onFeltThemeChange: (FeltTheme) -> Unit = {},
    onCardBackStyleChange: (CardBackStyle) -> Unit = {},
    onCardFaceStyleChange: (CardFaceStyle) -> Unit = {},
    onSoundChange: (Boolean) -> Unit = {},
    onHapticsChange: (Boolean) -> Unit = {},
    onResetSettingsToDefaults: () -> Unit = {}
) {
    val dragDropState = rememberDragDropState()
    val dropTargetRegistry = rememberDropTargetRegistry()
    val solitaireHaptics = rememberSolitaireHaptics(enabled = hapticsEnabled)
    val solitaireAudio = rememberSolitaireAudio(enabled = soundEnabled)
    val cardFlightState = rememberCardFlightState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(gameSessionId) {
        cardFlightState.cancelFlight()
        dragDropState.reset()
    }

    CompositionLocalProvider(
        LocalDragDropState provides dragDropState,
        LocalDropTargetRegistry provides dropTargetRegistry,
        LocalSolitaireHaptics provides solitaireHaptics,
        LocalSolitaireAudio provides solitaireAudio,
        LocalCardFlightState provides cardFlightState,
        LocalGameSessionId provides gameSessionId
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(SolitaireColors(feltTheme = feltTheme).tableBackground)
        ) {
            val dimensions = remember(maxWidth) {
                val minSpacing = CardDimensions.DEFAULT_COLUMN_SPACING * (CardDimensions.NUM_COLUMNS + 1)
                val safeWidth = if (maxWidth <= minSpacing) 393.dp else maxWidth
                CardDimensions.calculate(availableWidth = safeWidth)
            }

            // Animated Smart Tap and Stock Draw handlers
            val currentBoardState by rememberUpdatedState(boardState)
            val latestOnAutoCompleteStep by rememberUpdatedState(onAutoCompleteStep)
            val latestOnAutoCompleteFinished by rememberUpdatedState(onAutoCompleteFinished)

            val animatedAutoCompleteClick: () -> Unit = {
                if (!isAutoCompleting) {
                    dragDropState.reset()
                    onAutoCompleteClick()
                    coroutineScope.launch {
                        try {
                            while (isActive) {
                                val current = currentBoardState
                                val move = AutoCompleteResolver.nextMove(current) ?: break
                                val startOffset = calculateFlightSourceOffset(
                                    source = move.from,
                                    boardState = current,
                                    registry = dropTargetRegistry,
                                    dimensions = dimensions,
                                    density = density
                                )
                                val targetOffset = calculateFlightTargetOffset(
                                    target = move.to,
                                    boardState = current,
                                    registry = dropTargetRegistry,
                                    dimensions = dimensions,
                                    density = density
                                )
                                if (startOffset != null && targetOffset != null) {
                                    cardFlightState.startFlight(
                                        cards = listOf(move.card),
                                        startOffset = startOffset,
                                        targetOffset = targetOffset,
                                        isStockFlip = (move.from is CardLocation.Stock),
                                        durationMillis = 130
                                    ) {
                                        latestOnAutoCompleteStep(move)
                                    }
                                } else {
                                    latestOnAutoCompleteStep(move)
                                }
                            }
                        } finally {
                            latestOnAutoCompleteFinished()
                        }
                    }
                }
            }

            val animatedStockClick: () -> Unit = animatedStockClick@ { if (isAutoCompleting) return@animatedStockClick
                if (cardFlightState.activeFlight == null && boardState.stock.isNotEmpty()) {
                    val stockBounds = dropTargetRegistry.getBounds(CardLocation.Stock)
                    val wasteBounds = dropTargetRegistry.getBounds(CardLocation.Waste)

                    if (stockBounds != null && wasteBounds != null) {
                        val drawCount = minOf(if (drawMode == DrawMode.DRAW_ONE) 1 else 3, boardState.stock.size)
                        val movingCard = boardState.stock.take(drawCount).last()
                        val newVisibleCount = minOf(if (drawMode == DrawMode.DRAW_THREE) 3 else 1, boardState.waste.size + drawCount)
                        val fanOffsetPx = with(density) { (dimensions.cardWidth * 0.32f).coerceIn(14.dp, 20.dp).toPx() }
                        val sign = if (isLeftHanded) -1f else 1f
                        val targetOffset = if (drawMode == DrawMode.DRAW_THREE && newVisibleCount > 1) {
                            Offset(wasteBounds.topLeft.x + sign * (newVisibleCount - 1) * fanOffsetPx, wasteBounds.topLeft.y)
                        } else {
                            wasteBounds.topLeft
                        }

                        coroutineScope.launch {
                            cardFlightState.startFlight(
                                cards = listOf(movingCard),
                                startOffset = stockBounds.topLeft,
                                targetOffset = targetOffset,
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

            val animatedWasteClick: () -> Unit = animatedWasteClick@ { if (isAutoCompleting) return@animatedWasteClick
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
                            val visibleCount = minOf(if (drawMode == DrawMode.DRAW_THREE) 3 else 1, boardState.waste.size)
                            val fanOffsetPx = with(density) { (dimensions.cardWidth * 0.32f).coerceIn(14.dp, 20.dp).toPx() }
                            val sign = if (isLeftHanded) -1f else 1f
                            val startOffset = if (drawMode == DrawMode.DRAW_THREE && visibleCount > 1) {
                                Offset(wasteBounds.topLeft.x + sign * (visibleCount - 1) * fanOffsetPx, wasteBounds.topLeft.y)
                            } else {
                                wasteBounds.topLeft
                            }

                            coroutineScope.launch {
                                cardFlightState.startFlight(
                                    cards = listOf(wasteCard),
                                    startOffset = startOffset,
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

            val animatedTableauCardClick: (columnIndex: Int, card: Card) -> Unit = animatedTableauCardClick@ { columnIndex, card -> if (isAutoCompleting) return@animatedTableauCardClick
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

            SolitaireTheme(feltTheme = feltTheme, cardBackStyle = cardBackStyle, cardFaceStyle = cardFaceStyle, cardDimensions = dimensions) {
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
                                timeSeconds = timeSeconds,
                                onStatsClick = onStatsClick
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val isStockHighlighted = isHintActive && (hintSourceLocation is CardLocation.Stock)
                            val isWasteHighlighted = isHintActive && (hintSourceLocation is CardLocation.Waste)
                            val highlightedFoundationIndex = if (isHintActive) {
                                when {
                                    hintTargetLocation is CardLocation.Foundation -> hintTargetLocation.index
                                    hintSourceLocation is CardLocation.Foundation -> hintSourceLocation.index
                                    highlightedCard != null -> boardState.foundations.indexOfFirst { it.lastOrNull() == highlightedCard }.takeIf { it >= 0 }
                                    else -> null
                                }
                            } else null

                            val destinationTableauCard = if (isHintActive && hintTargetLocation is CardLocation.Tableau) {
                                boardState.tableau.getOrNull(hintTargetLocation.columnIndex)?.lastOrNull()
                            } else null

                            val highlightedEmptyTableauColumnIndex = if (isHintActive && hintTargetLocation is CardLocation.Tableau) {
                                if (boardState.tableau.getOrNull(hintTargetLocation.columnIndex).isNullOrEmpty()) {
                                    hintTargetLocation.columnIndex
                                } else null
                            } else null

                            TopRowView(
                                boardState = boardState,
                                drawMode = drawMode,
                                isLeftHanded = isLeftHanded,
                                isStockHighlighted = isStockHighlighted,
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
                                highlightedCard = if (hintSourceLocation is CardLocation.Tableau) highlightedCard else null,
                                highlightedCards = if (hintSourceLocation is CardLocation.Tableau) highlightedCards else emptyList(),
                                destinationCard = destinationTableauCard,
                                highlightedEmptyColumnIndex = highlightedEmptyTableauColumnIndex,
                                gameSessionId = gameSessionId,
                                onCardClick = animatedTableauCardClick,
                                onEmptyColumnClick = onTableauEmptyClick,
                                onCardDropped = onCardDropped
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AutoCompleteBannerView(
                                isVisible = isAutoCompleteAvailable && !isGameWon && !isAutoCompleting,
                                onClick = animatedAutoCompleteClick,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            BottomActionBarView(
                                canUndo = canUndo,
                                onUndoClick = onUndoClick,
                                onHintClick = onHintClick,
                                onNewGameClick = onNewGameClick,
                                onSettingsClick = onSettingsClick,
                                isHintActive = isHintActive,
                                onStatsClick = onStatsClick,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Floating animated card flight overlay (Smart Tap & Stock Flip)
                    AnimatedMoveOverlay(flightState = cardFlightState)

                    // Floating drag-and-drop overlay layer in root window coordinates (ADR 003)
                    DragOverlay(dragDropState = dragDropState)

                    // Touch interceptor barrier during auto-complete cascade:
                    // Consumes all pointer events so cards cannot be tapped, held, or dragged
                    if (isAutoCompleting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                        )
                    }

                    // Modal Statistics Dialog
                    if (isStatsDialogOpen) {
                        StatsDialog(
                            stats = stats,
                            onResetStats = onResetStats,
                            onDismiss = onDismissStats
                        )
                    }

                    // Modal Settings Bottom Sheet
                    if (isSettingsOpen) {
                        SettingsBottomSheet(
                            drawMode = drawMode,
                            isLeftHanded = isLeftHanded,
                            autoHintEnabled = autoHintEnabled,
                            feltTheme = feltTheme,
                            cardBackStyle = cardBackStyle,
                            cardFaceStyle = cardFaceStyle,
                            soundEnabled = soundEnabled,
                            hapticsEnabled = hapticsEnabled,
                            onDrawModeChange = onDrawModeChange,
                            onLeftHandedChange = onLeftHandedChange,
                            onAutoHintChange = onAutoHintChange,
                            onFeltThemeChange = onFeltThemeChange,
                            onCardBackStyleChange = onCardBackStyleChange,
                            onCardFaceStyleChange = onCardFaceStyleChange,
                            onSoundChange = onSoundChange,
                            onHapticsChange = onHapticsChange,
                            onResetToDefaults = onResetSettingsToDefaults,
                            onDismiss = onDismissSettings
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates absolute root screen coordinates for destination slot [target].
 */
/**
 * Calculates absolute root screen coordinates for source slot [source].
 */
private fun calculateFlightSourceOffset(
    source: CardLocation,
    boardState: BoardState,
    registry: DropTargetRegistry,
    dimensions: CardDimensions,
    density: Density
): Offset? {
    return when (source) {
        is CardLocation.Tableau -> {
            val colBounds = registry.getBounds(CardLocation.Tableau(source.columnIndex)) ?: return null
            val colCards = boardState.tableau.getOrNull(source.columnIndex).orEmpty()
            val cardIndex = if (source.cardIndex >= 0) source.cardIndex else (colCards.size - 1).coerceAtLeast(0)
            if (colCards.isEmpty()) {
                colBounds.topLeft
            } else {
                val yOffsets = calculateTableauOffsets(
                    cards = colCards,
                    faceDownPeek = dimensions.faceDownPeek,
                    faceUpPeek = dimensions.faceUpPeek
                )
                val cardTopPx = with(density) {
                    yOffsets.getOrElse(cardIndex) { 0.dp }.toPx()
                }
                Offset(colBounds.left, colBounds.top + cardTopPx)
            }
        }
        is CardLocation.Waste -> {
            registry.getBounds(CardLocation.Waste)?.topLeft
        }
        is CardLocation.Stock -> {
            registry.getBounds(CardLocation.Stock)?.topLeft
        }
        is CardLocation.Foundation -> {
            registry.getBounds(source)?.topLeft
        }
    }
}

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
 * Observes single-shot [GameEvent]s to trigger tactile haptic and audio feedback.
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
    val solitaireHaptics = rememberSolitaireHaptics(enabled = uiState.hapticsEnabled)
    val solitaireAudio = rememberSolitaireAudio(enabled = uiState.soundEnabled)

    LaunchedEffect(viewModel, solitaireHaptics, solitaireAudio) {
        viewModel.events.collect { event ->
            val currentState = viewModel.uiState.value
            when (event) {
                is GameEvent.PlayHapticTick -> {
                    if (currentState.hapticsEnabled) solitaireHaptics.playTick()
                    if (currentState.soundEnabled) solitaireAudio.playFlip()
                }
                is GameEvent.PlayHapticSnap -> {
                    if (currentState.hapticsEnabled) solitaireHaptics.playSnap()
                    if (currentState.soundEnabled) solitaireAudio.playSnap()
                }
                is GameEvent.PlayDealSound -> {
                    if (currentState.soundEnabled) solitaireAudio.playDeal()
                }
                is GameEvent.TriggerWinCelebration -> {
                    if (currentState.hapticsEnabled) solitaireHaptics.playSnap()
                    if (currentState.soundEnabled) solitaireAudio.playSnap()
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
        highlightedCards = uiState.highlightedCards,
        isHintActive = uiState.isHintActive,
        hintSourceLocation = uiState.hintSourceLocation,
        hintTargetLocation = uiState.hintTargetLocation,
        gameSessionId = uiState.gameSessionId,
        drawMode = uiState.drawMode,
        isAutoCompleteAvailable = uiState.isAutoCompleteAvailable,
        isAutoCompleting = uiState.isAutoCompleting,
        isGameWon = uiState.isGameWon,
        onAutoCompleteClick = { viewModel.onIntent(GameIntent.StartAutoComplete) },
        onAutoCompleteStep = { viewModel.onIntent(GameIntent.ApplyAutoCompleteMove(it)) },
        onAutoCompleteFinished = { viewModel.onIntent(GameIntent.FinishAutoComplete) },
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
        onHintClick = {
            if (uiState.isHintActive) {
                viewModel.onIntent(GameIntent.DismissHint)
            } else {
                viewModel.onIntent(GameIntent.RequestHint)
            }
        },
        cardBackStyle = uiState.cardBackStyle,
        cardFaceStyle = uiState.cardFaceStyle,
        soundEnabled = uiState.soundEnabled,
        hapticsEnabled = uiState.hapticsEnabled,
        autoHintEnabled = uiState.autoHintEnabled,
        isSettingsOpen = uiState.isSettingsOpen,
        isStatsDialogOpen = uiState.isStatsDialogOpen,
        stats = uiState.stats,
        onNewGameClick = { viewModel.onIntent(GameIntent.StartNewGame) },
        onSettingsClick = { viewModel.onIntent(GameIntent.OpenSettings) },
        onDismissSettings = { viewModel.onIntent(GameIntent.CloseSettings) },
        onDrawModeChange = { viewModel.onIntent(GameIntent.SetDrawMode(it)) },
        onLeftHandedChange = { viewModel.onIntent(GameIntent.SetLeftHanded(it)) },
        onAutoHintChange = { viewModel.onIntent(GameIntent.SetAutoHintEnabled(it)) },
        onFeltThemeChange = { viewModel.onIntent(GameIntent.SetFeltTheme(it)) },
        onCardBackStyleChange = { viewModel.onIntent(GameIntent.SetCardBackStyle(it)) },
        onCardFaceStyleChange = { viewModel.onIntent(GameIntent.SetCardFaceStyle(it)) },
        onSoundChange = { viewModel.onIntent(GameIntent.SetSoundEnabled(it)) },
        onHapticsChange = { viewModel.onIntent(GameIntent.SetHapticsEnabled(it)) },
        onResetSettingsToDefaults = { viewModel.onIntent(GameIntent.ResetSettingsToDefaults) },
        onStatsClick = { viewModel.onIntent(GameIntent.OpenStats) },
        onDismissStats = { viewModel.onIntent(GameIntent.CloseStats) },
        onResetStats = { viewModel.onIntent(GameIntent.ResetStats) }
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
    highlightedCards: List<Card> = emptyList(),
    isHintActive: Boolean = false,
    hintSourceLocation: CardLocation? = null,
    hintTargetLocation: CardLocation? = null,
    gameSessionId: Long = 1L,
    drawMode: DrawMode = DrawMode.DRAW_ONE,
    cardBackStyle: CardBackStyle = CardBackStyle.DEFAULT,
    cardFaceStyle: CardFaceStyle = CardFaceStyle.DEFAULT,
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    autoHintEnabled: Boolean = false,
    isSettingsOpen: Boolean = false,
    isAutoCompleteAvailable: Boolean = false,
    isAutoCompleting: Boolean = false,
    isGameWon: Boolean = false,
    onAutoCompleteClick: () -> Unit = {},
    onAutoCompleteStep: (AutoCompleteMove) -> Unit = {},
    onAutoCompleteFinished: () -> Unit = {},
    onStockClick: () -> Unit = {},
    onWasteClick: () -> Unit = {},
    onFoundationClick: (foundationIndex: Int) -> Unit = {},
    onTableauCardClick: (columnIndex: Int, card: Card) -> Unit = { _, _ -> },
    onTableauEmptyClick: (columnIndex: Int) -> Unit = {},
    onCardDropped: ((cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit)? = null,
    onUndoClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onNewGameClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onDrawModeChange: (DrawMode) -> Unit = {},
    onLeftHandedChange: (Boolean) -> Unit = {},
    onAutoHintChange: (Boolean) -> Unit = {},
    onFeltThemeChange: (FeltTheme) -> Unit = {},
    onCardBackStyleChange: (CardBackStyle) -> Unit = {},
    onCardFaceStyleChange: (CardFaceStyle) -> Unit = {},
    onSoundChange: (Boolean) -> Unit = {},
    onHapticsChange: (Boolean) -> Unit = {},
    onResetSettingsToDefaults: () -> Unit = {},
    isStatsDialogOpen: Boolean = false,
    stats: GameStats = GameStats(),
    onStatsClick: () -> Unit = {},
    onDismissStats: () -> Unit = {},
    onResetStats: () -> Unit = {}
) {
    SolitaireGameScreen(
        boardState = boardState,
        modifier = modifier,
        timeSeconds = timeSeconds,
        canUndo = canUndo,
        isLeftHanded = isLeftHanded,
        feltTheme = feltTheme,
        highlightedCard = highlightedCard,
        highlightedCards = highlightedCards,
        isHintActive = isHintActive,
        hintSourceLocation = hintSourceLocation,
        hintTargetLocation = hintTargetLocation,
        gameSessionId = gameSessionId,
        drawMode = drawMode,
        cardBackStyle = cardBackStyle,
        cardFaceStyle = cardFaceStyle,
        soundEnabled = soundEnabled,
        hapticsEnabled = hapticsEnabled,
        autoHintEnabled = autoHintEnabled,
        isSettingsOpen = isSettingsOpen,
        isAutoCompleteAvailable = isAutoCompleteAvailable,
        isAutoCompleting = isAutoCompleting,
        isGameWon = isGameWon,
        onAutoCompleteClick = onAutoCompleteClick,
        onAutoCompleteStep = onAutoCompleteStep,
        onAutoCompleteFinished = onAutoCompleteFinished,
        onStockClick = onStockClick,
        onWasteClick = onWasteClick,
        onFoundationClick = onFoundationClick,
        onTableauCardClick = onTableauCardClick,
        onTableauEmptyClick = onTableauEmptyClick,
        onCardDropped = onCardDropped,
        onUndoClick = onUndoClick,
        onHintClick = onHintClick,
        onNewGameClick = onNewGameClick,
        onSettingsClick = onSettingsClick,
        onDismissSettings = onDismissSettings,
        onDrawModeChange = onDrawModeChange,
        onLeftHandedChange = onLeftHandedChange,
        onAutoHintChange = onAutoHintChange,
        onFeltThemeChange = onFeltThemeChange,
        onCardBackStyleChange = onCardBackStyleChange,
        onCardFaceStyleChange = onCardFaceStyleChange,
        onSoundChange = onSoundChange,
        onHapticsChange = onHapticsChange,
        onResetSettingsToDefaults = onResetSettingsToDefaults,
        isStatsDialogOpen = isStatsDialogOpen,
        stats = stats,
        onStatsClick = onStatsClick,
        onDismissStats = onDismissStats,
        onResetStats = onResetStats
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
