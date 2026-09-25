package io.github.qdiaps.solitaire.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.engine.UndoManager
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import io.github.qdiaps.solitaire.domain.rules.SmartTapResolver
import io.github.qdiaps.solitaire.domain.solver.DeadlockDetector
import io.github.qdiaps.solitaire.domain.solver.DealGenerator
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Presentation ViewModel managing the reactive MVI state, game session lifecycle,
 * coroutine stopwatch timer, and intent dispatching for Klondike Solitaire.
 *
 * @param dealGenerator Optional background deal generator providing pre-verified solvable deals.
 * @param dealProvider Factory providing freshly shuffled deals when generator is absent or fallback is needed.
 * @param timerDispatcher Coroutine dispatcher managing the timer loop (default: [Dispatchers.Default]).
 * @param timerDelayMs Interval in milliseconds between timer ticks (default: 1000ms).
 * @param initialBoardState Optional board state injected directly on initialization (for testing or restoration).
 * @param autoStartTimer Whether the stopwatch timer starts ticking immediately upon creation (default: true).
 * @param drawMode Configures whether 1 card or 3 cards are drawn from the stock pile (default: [DrawMode.DRAW_ONE]).
 * @param coroutineScope Optional coroutine scope for managing background tasks and timer (defaults to [viewModelScope]).
 */
class GameViewModel(
    private val dealGenerator: DealGenerator? = null,
    private val dealProvider: () -> BoardState = { KlondikeDealer.dealShuffled() },
    private val timerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timerDelayMs: Long = 1000L,
    initialBoardState: BoardState? = null,
    private val autoStartTimer: Boolean = true,
    private val drawMode: DrawMode = DrawMode.DRAW_ONE,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private val undoManager = UndoManager()
    private var initialDealState: BoardState = initialBoardState ?: dealProvider()

    private val _uiState = MutableStateFlow(
        GameUiState(
            boardState = initialDealState,
            isGameWon = KlondikeRules.isGameWon(initialDealState)
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null

    /**
     * Indicates whether the stopwatch timer coroutine is currently active.
     */
    val isTimerRunning: Boolean
        get() = timerJob?.isActive == true

    init {
        if (autoStartTimer && !_uiState.value.isGameWon) {
            startTimer()
        }
    }

    /**
     * Dispatches player actions and UI events to the ViewModel.
     *
     * @param intent The [GameIntent] to process.
     */
    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.StartNewGame -> startNewGame()
            is GameIntent.RestartGame -> restartGame()
            is GameIntent.ToggleLeftHanded -> toggleLeftHanded()
            is GameIntent.SelectFeltTheme -> selectFeltTheme(intent.theme)
            is GameIntent.DismissHint -> dismissHint()
            is GameIntent.DrawStockCard -> drawStockCard()
            is GameIntent.RecycleStock -> recycleStock()
            is GameIntent.OnCardTapped -> onCardTapped(intent.card, intent.location)
            is GameIntent.OnCardDropped -> { /* Handled in T-4.8 */ }
            is GameIntent.UndoMove -> undoMove()
            is GameIntent.RequestHint -> { /* Handled in T-5.x */ }
            is GameIntent.AutoComplete -> { /* Handled in T-5.x */ }
            is GameIntent.SkipWinAnimation -> { /* Handled in T-6 */ }
        }
    }

    /**
     * Handles tapping a card or slot, triggering smart-tap auto-move resolution.
     *
     * If the tapped location is the stock pile, delegates to [drawStockCard].
     * Otherwise, uses [SmartTapResolver] to determine the highest-priority legal move
     * (Foundations -> Hidden Card Revealing Tableau -> Leftmost Tableau), auto-exposes
     * newly uncovered tableau cards, checks win and deadlock states, and records undo history.
     */
    fun onCardTapped(card: Card, location: CardLocation) {
        if (location is CardLocation.Stock) {
            drawStockCard()
            return
        }

        val currentBoard = _uiState.value.boardState
        val nextBoard = SmartTapResolver.resolveAndApply(currentBoard, location, autoExpose = true)
            ?: SmartTapResolver.resolveAndApply(currentBoard, card, autoExpose = true)

        if (nextBoard != null) {
            undoManager.record(currentBoard)
            updateBoardStateAfterMove(nextBoard)
        }
    }

    /**
     * Draws the next card(s) from the stock pile into the waste pile.
     * If the stock pile is empty and waste contains cards, automatically recycles the waste pile.
     */
    fun drawStockCard() {
        val currentBoard = _uiState.value.boardState
        if (KlondikeRules.canDraw(currentBoard)) {
            undoManager.record(currentBoard)
            val nextBoard = KlondikeRules.draw(currentBoard, drawMode)
            updateBoardStateAfterMove(nextBoard)
        } else if (KlondikeRules.canRecycle(currentBoard)) {
            recycleStock()
        }
    }

    /**
     * Recycles the entire waste pile back into the stock pile face-down.
     */
    fun recycleStock() {
        val currentBoard = _uiState.value.boardState
        if (!KlondikeRules.canRecycle(currentBoard)) return
        undoManager.record(currentBoard)
        val nextBoard = KlondikeRules.recycle(currentBoard)
        updateBoardStateAfterMove(nextBoard)
    }

    /**
     * Reverts the most recent game action using [undoManager].
     */
    fun undoMove() {
        val currentBoard = _uiState.value.boardState
        val previousBoard = undoManager.undo(currentBoard) ?: return
        val wasWon = _uiState.value.isGameWon
        val isWonNow = KlondikeRules.isGameWon(previousBoard)
        val isDeadlocked = if (isWonNow) false else DeadlockDetector.detect(previousBoard, drawMode).isDeadlocked
        _uiState.update { current ->
            current.copy(
                boardState = previousBoard,
                canUndo = undoManager.canUndo,
                isGameWon = isWonNow,
                isDeadlocked = isDeadlocked,
                activeHint = null
            )
        }
        if (wasWon && !isWonNow && autoStartTimer) {
            startTimer()
        }
        _events.tryEmit(GameEvent.PlayHapticTick)
    }

    /**
     * Deals a fresh game board and resets game counters.
     *
     * If a [dealGenerator] is configured, asynchronously fetches the next verified solvable deal.
     * Otherwise, immediately deals a new shuffled board via [dealProvider].
     */
    fun startNewGame() {
        stopTimer()
        if (dealGenerator != null) {
            _uiState.update { it.copy(isLoading = true) }
            scope.launch {
                val newBoard = try {
                    dealGenerator.getSolvableDeal()
                } catch (_: Exception) {
                    dealProvider()
                }
                applyNewDeal(newBoard)
            }
        } else {
            applyNewDeal(dealProvider())
        }
    }

    /**
     * Restarts the current game layout from its initial dealt state, resetting moves, score, and timer.
     */
    fun restartGame() {
        stopTimer()
        undoManager.clear()
        val isWon = KlondikeRules.isGameWon(initialDealState)
        _uiState.update { current ->
            current.copy(
                boardState = initialDealState,
                isGameWon = isWon,
                isDeadlocked = false,
                canUndo = false,
                elapsedTimeSeconds = 0L,
                activeHint = null,
                isLoading = false,
                isAutoCompleteAvailable = false
            )
        }
        if (autoStartTimer && !isWon) {
            startTimer()
        }
    }

    /**
     * Toggles left-handed UI orientation mirroring top row foundations and stock piles.
     */
    fun toggleLeftHanded() {
        _uiState.update { it.copy(isLeftHanded = !it.isLeftHanded) }
    }

    /**
     * Updates the felt surface theme of the card table.
     */
    fun selectFeltTheme(theme: FeltTheme) {
        _uiState.update { it.copy(feltTheme = theme) }
    }

    /**
     * Dismisses any active hint highlight on the board.
     */
    fun dismissHint() {
        _uiState.update { it.copy(activeHint = null) }
    }

    /**
     * Starts or resumes the elapsed play time stopwatch coroutine loop.
     */
    fun startTimer() {
        if (timerJob?.isActive == true || _uiState.value.isGameWon) return
        timerJob = scope.launch(timerDispatcher) {
            while (isActive) {
                delay(timerDelayMs)
                _uiState.update { current ->
                    if (!current.isGameWon) {
                        current.copy(elapsedTimeSeconds = current.elapsedTimeSeconds + 1)
                    } else {
                        current
                    }
                }
            }
        }
    }

    /**
     * Pauses the elapsed play time stopwatch coroutine loop.
     */
    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Resumes the stopwatch timer if paused.
     */
    fun resumeTimer() {
        startTimer()
    }

    /**
     * Stops and cancels the stopwatch timer.
     */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    private fun updateBoardStateAfterMove(nextBoard: BoardState) {
        val isWon = KlondikeRules.isGameWon(nextBoard)
        val isDeadlocked = if (isWon) false else DeadlockDetector.detect(nextBoard, drawMode).isDeadlocked
        _uiState.update { current ->
            current.copy(
                boardState = nextBoard,
                canUndo = undoManager.canUndo,
                isGameWon = isWon,
                isDeadlocked = isDeadlocked,
                activeHint = null
            )
        }
        if (isWon) {
            stopTimer()
            _events.tryEmit(GameEvent.TriggerWinCelebration)
        } else {
            _events.tryEmit(GameEvent.PlayHapticTick)
        }
    }

    private fun applyNewDeal(board: BoardState) {
        initialDealState = board
        undoManager.clear()
        val isWon = KlondikeRules.isGameWon(board)
        _uiState.update { current ->
            current.copy(
                boardState = board,
                isGameWon = isWon,
                isDeadlocked = false,
                canUndo = false,
                elapsedTimeSeconds = 0L,
                activeHint = null,
                isLoading = false,
                isAutoCompleteAvailable = false
            )
        }
        if (autoStartTimer && !isWon) {
            startTimer()
        }
    }
}
