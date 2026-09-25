package io.github.qdiaps.solitaire.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.engine.UndoManager
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
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
 * @param timerDelayMs Interval in milliseconds between timer ticks (default: 1000ms).\n * @param initialBoardState Optional board state injected directly on initialization (for testing or restoration).
 * @param autoStartTimer Whether the stopwatch timer starts ticking immediately upon creation (default: true).
 * @param coroutineScope Optional coroutine scope for managing background tasks and timer (defaults to [viewModelScope]).
 */
class GameViewModel(
    private val dealGenerator: DealGenerator? = null,
    private val dealProvider: () -> BoardState = { KlondikeDealer.dealShuffled() },
    private val timerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timerDelayMs: Long = 1000L,
    initialBoardState: BoardState? = null,
    private val autoStartTimer: Boolean = true,
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
            is GameIntent.DrawStockCard -> { /* Handled in T-4.3 */ }
            is GameIntent.RecycleStock -> { /* Handled in T-4.3 */ }
            is GameIntent.OnCardTapped -> { /* Handled in T-4.4 */ }
            is GameIntent.OnCardDropped -> { /* Handled in T-4.8 */ }
            is GameIntent.UndoMove -> { /* Handled in T-4.3 */ }
            is GameIntent.RequestHint -> { /* Handled in T-5.x */ }
            is GameIntent.AutoComplete -> { /* Handled in T-5.x */ }
            is GameIntent.SkipWinAnimation -> { /* Handled in T-6 */ }
        }
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
