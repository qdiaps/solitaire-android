package io.github.qdiaps.solitaire.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.repository.DataStoreSettingsRepository
import io.github.qdiaps.solitaire.data.repository.DataStoreStatsRepository
import io.github.qdiaps.solitaire.data.repository.SettingsRepository
import io.github.qdiaps.solitaire.data.repository.StatsRepository
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.engine.UndoManager
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteMove
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteResolver
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.HintResolver
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import io.github.qdiaps.solitaire.domain.rules.SmartTapResolver
import io.github.qdiaps.solitaire.domain.solver.DeadlockDetector
import io.github.qdiaps.solitaire.domain.solver.DealGenerator
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
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
 * coroutine stopwatch timer, auto-complete cascade execution, and intent dispatching for Klondike Solitaire.
 *
 * @param dealGenerator Optional background deal generator providing pre-verified solvable deals.
 * @param dealProvider Factory providing freshly shuffled deals when generator is absent or fallback is needed.
 * @param timerDispatcher Coroutine dispatcher managing the timer loop (default: [Dispatchers.Default]).
 * @param timerDelayMs Interval in milliseconds between timer ticks (default: 1000ms).
 * @param initialBoardState Optional board state injected directly on initialization (for testing or restoration).
 * @param autoStartTimer Whether the stopwatch timer starts ticking immediately upon creation (default: true).
 * @param drawMode Configures whether 1 card or 3 cards are drawn from the stock pile (default: [DrawMode.DRAW_ONE]).
 * @param coroutineScope Optional coroutine scope for managing background tasks and timer (defaults to [viewModelScope]).
 * @param autoCompleteDelayMs Interval in milliseconds between cascade moves during auto-complete (default: 120ms).
 * @param settingsRepository Optional repository persisting and broadcasting player preferences.
 */
class GameViewModel(
    private val dealGenerator: DealGenerator? = null,
    private val dealProvider: () -> BoardState = { KlondikeDealer.dealShuffled() },
    private val timerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timerDelayMs: Long = 1000L,
    initialBoardState: BoardState? = null,
    private val autoStartTimer: Boolean = true,
    private val drawMode: DrawMode = DrawMode.DRAW_ONE,
    coroutineScope: CoroutineScope? = null,
    private val autoCompleteDelayMs: Long = 120L,
    private val settingsRepository: SettingsRepository? = null,
    private val statsRepository: StatsRepository? = null,
    private val idleHintDelayMs: Long = DEFAULT_IDLE_HINT_DELAY_MS,
    initialAutoHintEnabled: Boolean = false
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private val undoManager = UndoManager()
    private var initialDealState: BoardState = initialBoardState ?: dealProvider()

    private val initialIsWon = KlondikeRules.isGameWon(initialDealState)
    private val _uiState = MutableStateFlow(
        GameUiState(
            boardState = initialDealState,
            isGameWon = initialIsWon,
            isAutoCompleteAvailable = if (initialIsWon) false else AutoCompleteResolver.isAutoCompleteReady(initialDealState),
            drawMode = drawMode,
            autoHintEnabled = initialAutoHintEnabled
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var autoCompleteJob: Job? = null
    private var idleHintJob: Job? = null

    /**
     * Indicates whether the idle auto-hint timer coroutine is currently active.
     */
    val isIdleHintActive: Boolean
        get() = idleHintJob?.isActive == true

    /**
     * Indicates whether the stopwatch timer coroutine is currently active.
     */
    val isTimerRunning: Boolean
        get() = timerJob?.isActive == true

    /**
     * Indicates whether the auto-complete cascade loop is currently running.
     */
    val isAutoCompleting: Boolean
        get() = autoCompleteJob?.isActive == true

    init {
        if (autoStartTimer && !_uiState.value.isGameWon) {
            startTimer()
        }
        if (initialAutoHintEnabled && !_uiState.value.isGameWon) {
            resetIdleHintTimer()
        }

        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.settingsFlow.collect { settings ->
                    val wasAutoHint = _uiState.value.autoHintEnabled
                    _uiState.update { current ->
                        current.copy(
                            drawMode = settings.drawMode,
                            isLeftHanded = settings.isLeftHanded,
                            feltTheme = settings.feltTheme,
                            cardBackStyle = settings.cardBackStyle,
                            cardFaceStyle = settings.cardFaceStyle,
                            soundEnabled = settings.soundEnabled,
                            hapticsEnabled = settings.hapticsEnabled,
                            autoHintEnabled = settings.autoHintEnabled
                        )
                    }
                    if (settings.autoHintEnabled != wasAutoHint) {
                        if (settings.autoHintEnabled) {
                            resetIdleHintTimer()
                        } else {
                            cancelIdleHintTimer()
                        }
                    }
                }
            }
        }

        if (statsRepository != null && !initialIsWon) {
            scope.launch { statsRepository.recordGameStarted() }
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
            is GameIntent.SetFeltTheme -> selectFeltTheme(intent.theme)
            is GameIntent.RequestHint -> requestHint()
            is GameIntent.DismissHint -> dismissHint()
            is GameIntent.DrawStockCard -> drawStockCard()
            is GameIntent.RecycleStock -> recycleStock()
            is GameIntent.OnCardTapped -> onCardTapped(intent.card, intent.location)
            is GameIntent.OnCardDropped -> onCardDropped(intent.cards, intent.source, intent.target)
            is GameIntent.UndoMove -> undoMove()
            is GameIntent.AutoComplete -> autoComplete()
            is GameIntent.StartAutoComplete -> startAutoComplete()
            is GameIntent.FinishAutoComplete -> finishAutoComplete()
            is GameIntent.ApplyAutoCompleteMove -> applyAutoCompleteMove(intent.move)
            is GameIntent.SkipWinAnimation -> { /* Handled in T-6 */ }
            is GameIntent.OpenSettings -> openSettings()
            is GameIntent.CloseSettings -> closeSettings()
            is GameIntent.SetDrawMode -> setDrawMode(intent.drawMode)
            is GameIntent.SetLeftHanded -> setLeftHanded(intent.isLeftHanded)
            is GameIntent.SetCardBackStyle -> setCardBackStyle(intent.cardBackStyle)
            is GameIntent.SetCardFaceStyle -> setCardFaceStyle(intent.cardFaceStyle)
            is GameIntent.SetSoundEnabled -> setSoundEnabled(intent.enabled)
            is GameIntent.SetHapticsEnabled -> setHapticsEnabled(intent.enabled)
            is GameIntent.SetAutoHintEnabled -> setAutoHintEnabled(intent.enabled)
            is GameIntent.ResetSettingsToDefaults -> resetSettingsToDefaults()
        }
    }

    private fun cancelIdleHintTimer() {
        idleHintJob?.cancel()
        idleHintJob = null
    }

    private fun resetIdleHintTimer() {
        cancelIdleHintTimer()
        if (!_uiState.value.autoHintEnabled ||
            _uiState.value.isGameWon ||
            _uiState.value.isAutoCompleting ||
            _uiState.value.isSettingsOpen ||
            _uiState.value.isHintActive
        ) {
            return
        }
        idleHintJob = scope.launch(timerDispatcher) {
            delay(idleHintDelayMs)
            if (isActive &&
                _uiState.value.autoHintEnabled &&
                !_uiState.value.isGameWon &&
                !_uiState.value.isAutoCompleting &&
                !_uiState.value.isSettingsOpen
            ) {
                requestHint()
            }
        }
    }

    /**
     * Opens the modal settings bottom sheet and pauses elapsed timer.
     */
    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
        pauseTimer()
        cancelIdleHintTimer()
    }

    /**
     * Closes the modal settings bottom sheet and resumes elapsed timer if active.
     */
    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
        if (autoStartTimer && !_uiState.value.isGameWon) {
            startTimer()
        }
        if (_uiState.value.autoHintEnabled && !_uiState.value.isGameWon) {
            resetIdleHintTimer()
        }
    }

    /**
     * Updates draw mode (Draw 1 or Draw 3) live and persists change to settings repository.
     */
    fun setDrawMode(newDrawMode: DrawMode) {
        _uiState.update { it.copy(drawMode = newDrawMode) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setDrawMode(newDrawMode) }
        }
    }

    /**
     * Updates left-handed layout mode and persists change to settings repository.
     */
    fun setLeftHanded(isLeftHanded: Boolean) {
        _uiState.update { it.copy(isLeftHanded = isLeftHanded) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setLeftHanded(isLeftHanded) }
        }
    }

    /**
     * Updates card back style and persists change to settings repository.
     */
    fun setCardBackStyle(cardBackStyle: CardBackStyle) {
        _uiState.update { it.copy(cardBackStyle = cardBackStyle) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setCardBackStyle(cardBackStyle) }
        }
    }

    /**
     * Updates card face style and persists change to settings repository.
     */
    fun setCardFaceStyle(cardFaceStyle: CardFaceStyle) {
        _uiState.update { it.copy(cardFaceStyle = cardFaceStyle) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setCardFaceStyle(cardFaceStyle) }
        }
    }

    /**
     * Toggles sound effects audio output and persists change to settings repository.
     */
    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setSoundEnabled(enabled) }
        }
    }

    /**
     * Toggles haptic feedback vibrations and persists change to settings repository.
     */
    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(hapticsEnabled = enabled) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setHapticsEnabled(enabled) }
        }
    }

    /**
     * Toggles idle auto-hinting and persists change to settings repository.
     */
    fun setAutoHintEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoHintEnabled = enabled) }
        if (enabled) {
            resetIdleHintTimer()
        } else {
            cancelIdleHintTimer()
        }
        settingsRepository?.let { repo ->
            scope.launch { repo.setAutoHintEnabled(enabled) }
        }
    }

    /**
     * Resets all game settings to default values.
     */
    fun resetSettingsToDefaults() {
        if (settingsRepository != null) {
            scope.launch { settingsRepository.resetToDefaults() }
        } else {
            cancelIdleHintTimer()
            _uiState.update {
                it.copy(
                    drawMode = DrawMode.DRAW_ONE,
                    isLeftHanded = false,
                    feltTheme = FeltTheme.CLASSIC_GREEN,
                    cardBackStyle = CardBackStyle.CLASSIC_LATTICE,
                    cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                    soundEnabled = true,
                    hapticsEnabled = true,
                    autoHintEnabled = false
                )
            }
        }
    }

    /**
     * Finds and sets a productive move hint for the current board state using [HintResolver].
     */
    fun requestHint() {
        cancelIdleHintTimer()
        val currentBoard = _uiState.value.boardState
        val hint = HintResolver.findHint(currentBoard, _uiState.value.drawMode)
        _uiState.update { it.copy(activeHint = hint) }
    }

    /**
     * Dismisses any active hint highlight on the board.
     */
    fun dismissHint() {
        _uiState.update { it.copy(activeHint = null) }
        resetIdleHintTimer()
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
        if (autoCompleteJob?.isActive == true) return
        resetIdleHintTimer()
        if (_uiState.value.activeHint != null) {
            dismissHint()
        }

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
        if (autoCompleteJob?.isActive == true) return
        resetIdleHintTimer()
        if (_uiState.value.activeHint != null) {
            dismissHint()
        }

        val currentBoard = _uiState.value.boardState
        val currentDrawMode = _uiState.value.drawMode
        if (KlondikeRules.canDraw(currentBoard)) {
            undoManager.record(currentBoard)
            val nextBoard = KlondikeRules.draw(currentBoard, currentDrawMode)
            updateBoardStateAfterMove(nextBoard)
        } else if (KlondikeRules.canRecycle(currentBoard)) {
            recycleStock()
        }
    }

    /**
     * Recycles the entire waste pile back into the stock pile face-down.
     */
    fun recycleStock() {
        if (autoCompleteJob?.isActive == true) return
        resetIdleHintTimer()
        if (_uiState.value.activeHint != null) {
            dismissHint()
        }

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
        cancelAutoComplete()
        resetIdleHintTimer()

        val currentBoard = _uiState.value.boardState
        val previousBoard = undoManager.undo(currentBoard) ?: return
        val wasWon = _uiState.value.isGameWon
        val isWonNow = KlondikeRules.isGameWon(previousBoard)
        val isDeadlocked = if (isWonNow) false else DeadlockDetector.detect(previousBoard, _uiState.value.drawMode).isDeadlocked
        val isAutoComplete = if (isWonNow) false else AutoCompleteResolver.isAutoCompleteReady(previousBoard)

        _uiState.update { current ->
            current.copy(
                boardState = previousBoard,
                canUndo = undoManager.canUndo,
                isGameWon = isWonNow,
                isDeadlocked = isDeadlocked,
                isAutoCompleteAvailable = isAutoComplete,
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
        cancelAutoComplete()
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
        cancelAutoComplete()
        stopTimer()
        undoManager.clear()
        val isWon = KlondikeRules.isGameWon(initialDealState)
        val isAutoComplete = if (isWon) false else AutoCompleteResolver.isAutoCompleteReady(initialDealState)
        _uiState.update { current ->
            current.copy(
                boardState = initialDealState,
                isGameWon = isWon,
                isDeadlocked = false,
                canUndo = false,
                elapsedTimeSeconds = 0L,
                activeHint = null,
                isLoading = false,
                isAutoCompleteAvailable = isAutoComplete,
                gameSessionId = current.gameSessionId + 1L
            )
        }
        if (autoStartTimer && !isWon) {
            startTimer()
        }
        if (_uiState.value.autoHintEnabled && !isWon) {
            resetIdleHintTimer()
        }
        if (statsRepository != null && !isWon) {
            scope.launch { statsRepository.recordGameStarted() }
        }
        _events.tryEmit(GameEvent.PlayDealSound)
    }

    /**
     * Toggles left-handed UI orientation mirroring top row foundations and stock piles.
     */
    fun toggleLeftHanded() {
        val next = !_uiState.value.isLeftHanded
        _uiState.update { it.copy(isLeftHanded = next) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setLeftHanded(next) }
        }
    }

    /**
     * Updates the felt surface theme of the card table.
     */
    fun selectFeltTheme(theme: FeltTheme) {
        _uiState.update { it.copy(feltTheme = theme) }
        settingsRepository?.let { repo ->
            scope.launch { repo.setFeltTheme(theme) }
        }
    }

    /**
     * Triggers the auto-complete cascade loop, sequentially moving remaining cards
     * to foundation piles with haptic snap feedback and timed intervals until victory.
     */
    fun autoComplete() {
        if (autoCompleteJob?.isActive == true || _uiState.value.isGameWon) return
        if (!_uiState.value.isAutoCompleteAvailable) return

        cancelIdleHintTimer()
        dismissHint()
        _uiState.update { it.copy(isAutoCompleting = true) }
        autoCompleteJob = scope.launch {
            try {
                while (isActive) {
                    val currentBoard = _uiState.value.boardState
                    val nextMove = AutoCompleteResolver.nextMove(currentBoard) ?: break
                    undoManager.record(currentBoard)
                    val nextBoard = nextMove.resultingState
                    val isWon = KlondikeRules.isGameWon(nextBoard)
                    val isAutoComplete = if (isWon) false else AutoCompleteResolver.isAutoCompleteReady(nextBoard)

                    _uiState.update { current ->
                        current.copy(
                            boardState = nextBoard,
                            canUndo = undoManager.canUndo,
                            isGameWon = isWon,
                            isDeadlocked = false,
                            isAutoCompleteAvailable = isAutoComplete,
                            isAutoCompleting = !isWon,
                            activeHint = null
                        )
                    }

                    if (isWon) {
                        stopTimer()
                        _events.tryEmit(GameEvent.TriggerWinCelebration)
                        recordVictoryInStats()
                        break
                    }

                    delay(autoCompleteDelayMs)
                }
            } finally {
                _uiState.update { it.copy(isAutoCompleting = false) }
            }
        }
    }

    /**
     * Marks the beginning of an animated auto-complete sequence in the UI.
     */
    fun startAutoComplete() {
        if (_uiState.value.isGameWon) return
        cancelIdleHintTimer()
        dismissHint()
        _uiState.update { it.copy(isAutoCompleting = true) }
    }

    /**
     * Marks the conclusion of an animated auto-complete sequence in the UI.
     */
    fun finishAutoComplete() {
        if (_uiState.value.isAutoCompleting) {
            _uiState.update { it.copy(isAutoCompleting = false) }
        }
    }

    /**
     * Applies a single animated auto-complete foundation promotion step.
     */
    fun applyAutoCompleteMove(move: AutoCompleteMove) {
        if (_uiState.value.isGameWon) return
        dismissHint()
        val currentBoard = _uiState.value.boardState
        undoManager.record(currentBoard)
        val nextBoard = move.resultingState
        val isWon = KlondikeRules.isGameWon(nextBoard)
        val isAutoComplete = if (isWon) false else AutoCompleteResolver.isAutoCompleteReady(nextBoard)

        _uiState.update { current ->
            current.copy(
                boardState = nextBoard,
                canUndo = undoManager.canUndo,
                isGameWon = isWon,
                isDeadlocked = false,
                isAutoCompleteAvailable = isAutoComplete,
                isAutoCompleting = !isWon,
                activeHint = null
            )
        }

        if (isWon) {
            stopTimer()
            _events.tryEmit(GameEvent.TriggerWinCelebration)
        }
    }

    private fun cancelAutoComplete() {
        autoCompleteJob?.cancel()
        autoCompleteJob = null
        if (_uiState.value.isAutoCompleting) {
            _uiState.update { it.copy(isAutoCompleting = false) }
        }
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
        cancelIdleHintTimer()
    }

    /**
     * Resumes the stopwatch timer if paused.
     */
    fun resumeTimer() {
        startTimer()
        if (_uiState.value.autoHintEnabled && !_uiState.value.isGameWon) {
            resetIdleHintTimer()
        }
    }

    /**
     * Stops and cancels the stopwatch timer.
     */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        cancelIdleHintTimer()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        cancelAutoComplete()
        cancelIdleHintTimer()
    }

    /**
     * Handles dropping cards from [source] onto [target].
     *
     * Validates destination via [KlondikeRules.canMoveCards]. If legal, records an undo snapshot,
     * applies the move with auto-expose, updates the board state, checks win/deadlock conditions,
     * and triggers [GameEvent.PlayHapticSnap].
     */
    fun onCardDropped(cards: List<Card>, source: CardLocation, target: CardLocation) {
        if (autoCompleteJob?.isActive == true) return
        resetIdleHintTimer()
        if (_uiState.value.activeHint != null) {
            dismissHint()
        }

        val currentBoard = _uiState.value.boardState
        if (!KlondikeRules.canMoveCards(currentBoard, cards, source, target)) return

        undoManager.record(currentBoard)
        val nextBoard = KlondikeRules.moveCards(currentBoard, cards, source, target, autoExpose = true)
        updateBoardStateAfterMove(nextBoard, isDrop = true)
    }

    private fun updateBoardStateAfterMove(nextBoard: BoardState, isDrop: Boolean = false) {
        val isWon = KlondikeRules.isGameWon(nextBoard)
        val isDeadlocked = if (isWon) false else DeadlockDetector.detect(nextBoard, _uiState.value.drawMode).isDeadlocked
        val isAutoComplete = if (isWon) false else AutoCompleteResolver.isAutoCompleteReady(nextBoard)
        _uiState.update { current ->
            current.copy(
                boardState = nextBoard,
                canUndo = undoManager.canUndo,
                isGameWon = isWon,
                isDeadlocked = isDeadlocked,
                isAutoCompleteAvailable = isAutoComplete,
                activeHint = null
            )
        }
        if (isWon) {
            stopTimer()
            cancelIdleHintTimer()
            _events.tryEmit(GameEvent.TriggerWinCelebration)
            recordVictoryInStats()
        } else {
            val event = if (isDrop) GameEvent.PlayHapticSnap else GameEvent.PlayHapticTick
            _events.tryEmit(event)
            resetIdleHintTimer()
        }
    }

    private fun applyNewDeal(board: BoardState) {
        cancelAutoComplete()
        initialDealState = board
        undoManager.clear()
        val isWon = KlondikeRules.isGameWon(board)
        val isAutoComplete = if (isWon) false else AutoCompleteResolver.isAutoCompleteReady(board)
        _uiState.update { current ->
            current.copy(
                boardState = board,
                isGameWon = isWon,
                isDeadlocked = false,
                canUndo = false,
                elapsedTimeSeconds = 0L,
                activeHint = null,
                isLoading = false,
                isAutoCompleteAvailable = isAutoComplete,
                gameSessionId = current.gameSessionId + 1L
            )
        }
        if (autoStartTimer && !isWon) {
            startTimer()
        }
        if (_uiState.value.autoHintEnabled && !isWon) {
            resetIdleHintTimer()
        }
        if (statsRepository != null && !isWon) {
            scope.launch { statsRepository.recordGameStarted() }
        }
        _events.tryEmit(GameEvent.PlayDealSound)
    }

    companion object {
        const val DEFAULT_IDLE_HINT_DELAY_MS: Long = 10_000L

        fun provideFactory(
            context: Context,
            dealGenerator: DealGenerator? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dataStoreManager = DataStoreManager.fromContext(context)
                val settingsRepo = DataStoreSettingsRepository(dataStoreManager)
                val statsRepo = DataStoreStatsRepository(dataStoreManager)
                return GameViewModel(
                    dealGenerator = dealGenerator,
                    settingsRepository = settingsRepo,
                    statsRepository = statsRepo
                ) as T
            }
        }
    }
    private fun recordVictoryInStats() {
        statsRepository?.let { repo ->
            val currentState = _uiState.value
            scope.launch {
                repo.recordGameWon(
                    timeSeconds = currentState.elapsedTimeSeconds.toInt(),
                    moves = currentState.boardState.movesCount,
                    score = currentState.boardState.score
                )
            }
        }
    }
}
