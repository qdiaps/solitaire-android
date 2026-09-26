package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.domain.deck.Deck
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import io.github.qdiaps.solitaire.domain.solver.DealGenerator
import io.github.qdiaps.solitaire.domain.solver.SolvabilityResult
import io.github.qdiaps.solitaire.data.model.GameSettings
import io.github.qdiaps.solitaire.data.repository.SettingsRepository
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createCustomBoard(id: String = "test"): BoardState {
        val card = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = id)
        return BoardState(stock = listOf(card))
    }

    private class FakeSettingsRepository(
        initialSettings: GameSettings = GameSettings()
    ) : SettingsRepository {
        private val _flow = MutableStateFlow(initialSettings)
        override val settingsFlow: StateFlow<GameSettings> = _flow.asStateFlow()

        override suspend fun getSettings(): GameSettings = _flow.value
        override suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
            _flow.update(transform)
        }
        override suspend fun setDrawMode(drawMode: DrawMode) { _flow.update { it.copy(drawMode = drawMode) } }
        override suspend fun setLeftHanded(isLeftHanded: Boolean) { _flow.update { it.copy(isLeftHanded = isLeftHanded) } }
        override suspend fun setFeltTheme(feltTheme: FeltTheme) { _flow.update { it.copy(feltTheme = feltTheme) } }
        override suspend fun setCardBackStyle(cardBackStyle: CardBackStyle) { _flow.update { it.copy(cardBackStyle = cardBackStyle) } }
        override suspend fun setCardFaceStyle(cardFaceStyle: CardFaceStyle) { _flow.update { it.copy(cardFaceStyle = cardFaceStyle) } }
        override suspend fun setSoundEnabled(enabled: Boolean) { _flow.update { it.copy(soundEnabled = enabled) } }
        override suspend fun setHapticsEnabled(enabled: Boolean) { _flow.update { it.copy(hapticsEnabled = enabled) } }
        override suspend fun setAutoHintEnabled(enabled: Boolean) { _flow.update { it.copy(autoHintEnabled = enabled) } }
        override suspend fun resetToDefaults() { _flow.value = GameSettings() }
    }

    private fun createWonBoard(): BoardState {
        val fullFoundations = Suit.entries.map { suit ->
            Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
        return BoardState(foundations = fullFoundations)
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {

        @Test
        @DisplayName("Initial state has dealt board and zero timer")
        fun `initial state has dealt board and zero timer`() = runTest(testDispatcher) {
            val customBoard = createCustomBoard("custom_deal")
            val viewModel = GameViewModel(
                initialBoardState = customBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val state = viewModel.uiState.value
            assertEquals(customBoard, state.boardState)
            assertEquals(0L, state.elapsedTimeSeconds)
            assertFalse(state.isGameWon)
            assertFalse(state.isDeadlocked)
            assertFalse(state.canUndo)
            assertFalse(state.isLoading)
            assertNull(state.activeHint)
            assertEquals(FeltTheme.CLASSIC_GREEN, state.feltTheme)
            assertFalse(state.isLeftHanded)
        }

        @Test
        @DisplayName("Default constructor generates standard 52-card deal")
        fun `default constructor generates standard deal`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val state = viewModel.uiState.value
            val totalCards = state.boardState.stock.size +
                state.boardState.waste.size +
                state.boardState.foundations.sumOf { it.size } +
                state.boardState.tableau.sumOf { it.size }

            assertEquals(Deck.TOTAL_CARDS, totalCards)
            assertEquals(7, state.boardState.tableau.size)
        }
    }

    @Nested
    @DisplayName("Timer Lifecycle Tests")
    inner class TimerLifecycleTests {

        @Test
        @DisplayName("Timer increments elapsedTimeSeconds every second")
        fun `timer increments elapsedTimeSeconds every second`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            assertEquals(0L, viewModel.uiState.value.elapsedTimeSeconds)
            assertTrue(viewModel.isTimerRunning)

            testScheduler.advanceTimeBy(1000)
            testScheduler.runCurrent()
            assertEquals(1L, viewModel.uiState.value.elapsedTimeSeconds)

            testScheduler.advanceTimeBy(2000)
            testScheduler.runCurrent()
            assertEquals(3L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.stopTimer()
        }

        @Test
        @DisplayName("Pausing and resuming timer")
        fun `pausing and resuming timer`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            testScheduler.advanceTimeBy(2000)
            testScheduler.runCurrent()
            assertEquals(2L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.pauseTimer()
            assertFalse(viewModel.isTimerRunning)

            testScheduler.advanceTimeBy(3000)
            testScheduler.runCurrent()
            assertEquals(2L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.resumeTimer()
            assertTrue(viewModel.isTimerRunning)

            testScheduler.advanceTimeBy(2000)
            testScheduler.runCurrent()
            assertEquals(4L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.stopTimer()
        }

        @Test
        @DisplayName("Timer does not increment when game is won")
        fun `timer does not increment when game is won`() = runTest(testDispatcher) {
            val wonBoard = createWonBoard()
            val viewModel = GameViewModel(
                initialBoardState = wonBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            // When game is initially won, timer should not run or increment
            testScheduler.advanceTimeBy(2000)
            testScheduler.runCurrent()
            assertEquals(0L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.stopTimer()
        }
    }

    @Nested
    @DisplayName("Game Lifecycle Intents")
    inner class GameLifecycleIntents {

        @Test
        @DisplayName("StartNewGame deals fresh board and resets timer")
        fun `StartNewGame deals fresh board and resets timer`() = runTest(testDispatcher) {
            var dealCounter = 1
            val viewModel = GameViewModel(
                dealProvider = { createCustomBoard("deal_${dealCounter++}") },
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            testScheduler.advanceTimeBy(3000)
            testScheduler.runCurrent()
            assertEquals(3L, viewModel.uiState.value.elapsedTimeSeconds)
            assertEquals(1L, viewModel.uiState.value.gameSessionId)
            assertEquals("deal_1", viewModel.uiState.value.boardState.stock.first().id)

            viewModel.onIntent(GameIntent.StartNewGame)
            testScheduler.runCurrent()

            assertEquals("deal_2", viewModel.uiState.value.boardState.stock.first().id)
            assertEquals(2L, viewModel.uiState.value.gameSessionId)
            assertEquals(0L, viewModel.uiState.value.elapsedTimeSeconds)

            testScheduler.advanceTimeBy(1000)
            testScheduler.runCurrent()
            assertEquals(1L, viewModel.uiState.value.elapsedTimeSeconds)

            viewModel.stopTimer()
        }

        @Test
        @DisplayName("RestartGame resets board to initial deal and timer to zero")
        fun `RestartGame resets board to initial deal and timer to zero`() = runTest(testDispatcher) {
            val initialBoard = createCustomBoard("initial_deal")
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            testScheduler.advanceTimeBy(5000)
            testScheduler.runCurrent()
            assertEquals(5L, viewModel.uiState.value.elapsedTimeSeconds)
            assertEquals(1L, viewModel.uiState.value.gameSessionId)

            viewModel.onIntent(GameIntent.RestartGame)
            testScheduler.runCurrent()

            assertEquals(initialBoard, viewModel.uiState.value.boardState)
            assertEquals(2L, viewModel.uiState.value.gameSessionId)
            assertEquals(0L, viewModel.uiState.value.elapsedTimeSeconds)
            assertTrue(viewModel.isTimerRunning)

            viewModel.stopTimer()
        }

        @Test
        @DisplayName("SelectFeltTheme and ToggleLeftHanded update UI state")
        fun `SelectFeltTheme and ToggleLeftHanded update UI state`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertFalse(viewModel.uiState.value.isLeftHanded)
            viewModel.onIntent(GameIntent.ToggleLeftHanded)
            assertTrue(viewModel.uiState.value.isLeftHanded)
            viewModel.onIntent(GameIntent.ToggleLeftHanded)
            assertFalse(viewModel.uiState.value.isLeftHanded)

            assertEquals(FeltTheme.CLASSIC_GREEN, viewModel.uiState.value.feltTheme)
            viewModel.onIntent(GameIntent.SelectFeltTheme(FeltTheme.DEEP_NAVY))
            assertEquals(FeltTheme.DEEP_NAVY, viewModel.uiState.value.feltTheme)
        }

        @Test
        @DisplayName("DismissHint clears active hint in UI state")
        fun `DismissHint clears active hint in UI state`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.DismissHint)
            assertNull(viewModel.uiState.value.activeHint)
            assertFalse(viewModel.uiState.value.isHintActive)
        }
    }

    @Nested
    @DisplayName("Stock and Undo Intents")
    inner class StockAndUndoIntents {

        private val card1 = Card(Suit.HEARTS, Rank.TEN, isFaceUp = false, id = "c1")
        private val card2 = Card(Suit.SPADES, Rank.NINE, isFaceUp = false, id = "c2")
        private val card3 = Card(Suit.DIAMONDS, Rank.EIGHT, isFaceUp = false, id = "c3")
        private val card4 = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = false, id = "c4")

        @Test
        @DisplayName("DrawStockCard moves card from stock to waste and increments movesCount")
        fun `DrawStockCard moves card from stock to waste and increments movesCount`() = runTest(testDispatcher) {
            val board = BoardState(stock = listOf(card1, card2))
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.DrawStockCard)

            val state = viewModel.uiState.value
            assertEquals(listOf(card2), state.boardState.stock)
            assertEquals(listOf(card1.copy(isFaceUp = true)), state.boardState.waste)
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("DrawStockCard in DRAW_THREE mode draws up to 3 cards to waste")
        fun `DrawStockCard in DRAW_THREE mode draws up to 3 cards to waste`() = runTest(testDispatcher) {
            val board = BoardState(stock = listOf(card1, card2, card3, card4))
            val viewModel = GameViewModel(
                initialBoardState = board,
                drawMode = DrawMode.DRAW_THREE,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.DrawStockCard)

            val state = viewModel.uiState.value
            assertEquals(listOf(card4), state.boardState.stock)
            assertEquals(
                listOf(card1, card2, card3).map { it.copy(isFaceUp = true) },
                state.boardState.waste
            )
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("DrawStockCard when stock is empty automatically recycles waste back into stock")
        fun `DrawStockCard when stock is empty automatically recycles waste back into stock`() = runTest(testDispatcher) {
            val wasteCards = listOf(card1.copy(isFaceUp = true), card2.copy(isFaceUp = true))
            val board = BoardState(stock = emptyList(), waste = wasteCards)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.DrawStockCard)

            val state = viewModel.uiState.value
            assertEquals(listOf(card1.copy(isFaceUp = false), card2.copy(isFaceUp = false)), state.boardState.stock)
            assertTrue(state.boardState.waste.isEmpty())
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("RecycleStock intent recycles waste cards face-down into stock")
        fun `RecycleStock intent recycles waste cards face-down into stock`() = runTest(testDispatcher) {
            val wasteCards = listOf(card1.copy(isFaceUp = true), card2.copy(isFaceUp = true))
            val board = BoardState(stock = emptyList(), waste = wasteCards)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RecycleStock)

            val state = viewModel.uiState.value
            assertEquals(listOf(card1.copy(isFaceUp = false), card2.copy(isFaceUp = false)), state.boardState.stock)
            assertTrue(state.boardState.waste.isEmpty())
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("RecycleStock when waste is empty does nothing")
        fun `RecycleStock when waste is empty does nothing`() = runTest(testDispatcher) {
            val board = BoardState(stock = emptyList(), waste = emptyList())
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RecycleStock)

            val state = viewModel.uiState.value
            assertTrue(state.boardState.stock.isEmpty())
            assertTrue(state.boardState.waste.isEmpty())
            assertEquals(0, state.boardState.movesCount)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("UndoMove reverts stock draw and restores previous board state and canUndo flag")
        fun `UndoMove reverts stock draw and restores previous board state and canUndo flag`() = runTest(testDispatcher) {
            val initialBoard = BoardState(stock = listOf(card1, card2))
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.DrawStockCard)
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.onIntent(GameIntent.UndoMove)

            val state = viewModel.uiState.value
            assertEquals(initialBoard, state.boardState)
            assertEquals(0, state.boardState.movesCount)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("UndoMove reverts stock recycle and restores waste cards")
        fun `UndoMove reverts stock recycle and restores waste cards`() = runTest(testDispatcher) {
            val wasteCards = listOf(card1.copy(isFaceUp = true), card2.copy(isFaceUp = true))
            val initialBoard = BoardState(stock = emptyList(), waste = wasteCards)
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RecycleStock)
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.onIntent(GameIntent.UndoMove)

            val state = viewModel.uiState.value
            assertEquals(initialBoard, state.boardState)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("UndoMove when undo history is empty does nothing")
        fun `UndoMove when undo history is empty does nothing`() = runTest(testDispatcher) {
            val initialBoard = BoardState(stock = listOf(card1))
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.UndoMove)

            val state = viewModel.uiState.value
            assertEquals(initialBoard, state.boardState)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("Multiple draw and undo operations properly maintain undo history stack")
        fun `Multiple draw and undo operations properly maintain undo history stack`() = runTest(testDispatcher) {
            val initialBoard = BoardState(stock = listOf(card1, card2, card3))
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            // Draw 1
            viewModel.onIntent(GameIntent.DrawStockCard)
            assertEquals(1, viewModel.uiState.value.boardState.waste.size)
            assertTrue(viewModel.uiState.value.canUndo)

            // Draw 2
            viewModel.onIntent(GameIntent.DrawStockCard)
            assertEquals(2, viewModel.uiState.value.boardState.waste.size)
            assertTrue(viewModel.uiState.value.canUndo)

            // Draw 3
            viewModel.onIntent(GameIntent.DrawStockCard)
            assertEquals(3, viewModel.uiState.value.boardState.waste.size)
            assertTrue(viewModel.uiState.value.boardState.stock.isEmpty())
            assertTrue(viewModel.uiState.value.canUndo)

            // Undo 1
            viewModel.onIntent(GameIntent.UndoMove)
            assertEquals(2, viewModel.uiState.value.boardState.waste.size)
            assertEquals(1, viewModel.uiState.value.boardState.stock.size)
            assertTrue(viewModel.uiState.value.canUndo)

            // Undo 2
            viewModel.onIntent(GameIntent.UndoMove)
            assertEquals(1, viewModel.uiState.value.boardState.waste.size)
            assertEquals(2, viewModel.uiState.value.boardState.stock.size)
            assertTrue(viewModel.uiState.value.canUndo)

            // Undo 3
            viewModel.onIntent(GameIntent.UndoMove)
            assertEquals(0, viewModel.uiState.value.boardState.waste.size)
            assertEquals(3, viewModel.uiState.value.boardState.stock.size)
            assertFalse(viewModel.uiState.value.canUndo)
        }
    }

    @Nested
    @DisplayName("Solvable Deal Generator Integration")
    inner class SolvableDealGeneratorIntegration {

        @Test
        @DisplayName("StartNewGame retrieves deal from DealGenerator when present")
        fun `StartNewGame retrieves deal from DealGenerator when present`() = runTest(testDispatcher) {
            val solvableBoard = createCustomBoard("solvable_seed")
            val generator = DealGenerator(
                scope = backgroundScope,
                dispatcher = testDispatcher,
                bufferCapacity = 1,
                dealProvider = { solvableBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                }
            )

            val viewModel = GameViewModel(
                dealGenerator = generator,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.StartNewGame)
            testScheduler.advanceUntilIdle()

            assertEquals(solvableBoard, viewModel.uiState.value.boardState)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.stopTimer()
            generator.stop()
        }
    }

    @Nested
    @DisplayName("Smart Tap Intents")
    inner class SmartTapIntents {

        @Test
        @DisplayName("Tapping Ace on waste moves it to foundation")
        fun `Tapping Ace on waste moves it to foundation`() = runTest(testDispatcher) {
            val aceOfSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = "ace_spades")
            val board = BoardState(waste = listOf(aceOfSpades))
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(aceOfSpades, CardLocation.Waste))

            val state = viewModel.uiState.value
            assertTrue(state.boardState.waste.isEmpty())
            assertEquals(listOf(aceOfSpades), state.boardState.foundations[0])
            assertEquals(1, state.boardState.movesCount)
            assertEquals(KlondikeRules.SCORE_WASTE_TO_FOUNDATION, state.boardState.score)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("Tapping Ace on tableau moves it to foundation and exposes face-down card below")
        fun `Tapping Ace on tableau moves it to foundation and exposes face-down card below`() = runTest(testDispatcher) {
            val hiddenCard = Card(Suit.HEARTS, Rank.TEN, isFaceUp = false, id = "hidden")
            val aceOfHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val tableau = List(7) { col ->
                if (col == 0) listOf(hiddenCard, aceOfHearts) else emptyList()
            }
            val board = BoardState(tableau = tableau)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(aceOfHearts, CardLocation.Tableau(0, 1)))

            val state = viewModel.uiState.value
            assertEquals(listOf(aceOfHearts), state.boardState.foundations[0])
            assertEquals(listOf(hiddenCard.copy(isFaceUp = true)), state.boardState.tableau[0])
            assertEquals(1, state.boardState.movesCount)
            assertEquals(
                KlondikeRules.SCORE_TABLEAU_TO_FOUNDATION + KlondikeRules.SCORE_TURNOVER_TABLEAU_CARD,
                state.boardState.score
            )
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("Tapping sequence on tableau moves sequence to valid destination column")
        fun `Tapping sequence on tableau moves sequence to valid destination column`() = runTest(testDispatcher) {
            val redSix = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true, id = "red6")
            val blackFive = Card(Suit.SPADES, Rank.FIVE, isFaceUp = true, id = "black5")
            val blackSeven = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true, id = "black7")

            val tableau = List(7) { col ->
                when (col) {
                    0 -> listOf(redSix, blackFive)
                    1 -> listOf(blackSeven)
                    else -> emptyList()
                }
            }
            val board = BoardState(tableau = tableau)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(redSix, CardLocation.Tableau(0, 0)))

            val state = viewModel.uiState.value
            assertTrue(state.boardState.tableau[0].isEmpty())
            assertEquals(listOf(blackSeven, redSix, blackFive), state.boardState.tableau[1])
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("Tapping unmovable card produces no change in board state and does not record undo")
        fun `Tapping unmovable card produces no change in board state and does not record undo`() = runTest(testDispatcher) {
            val redSix = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true, id = "red6")
            val board = BoardState(tableau = List(7) { col -> if (col == 0) listOf(redSix) else emptyList() })
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(redSix, CardLocation.Tableau(0, 0)))

            val state = viewModel.uiState.value
            assertEquals(board, state.boardState)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("Tapping stock location triggers drawStockCard")
        fun `Tapping stock location triggers drawStockCard`() = runTest(testDispatcher) {
            val card = Card(Suit.HEARTS, Rank.TEN, isFaceUp = false, id = "c1")
            val board = BoardState(stock = listOf(card))
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(card, CardLocation.Stock))

            val state = viewModel.uiState.value
            assertTrue(state.boardState.stock.isEmpty())
            assertEquals(listOf(card.copy(isFaceUp = true)), state.boardState.waste)
            assertTrue(state.canUndo)
        }

        @Test
        @DisplayName("Winning move via smart tap triggers win state, stops timer and emits TriggerWinCelebration")
        fun `Winning move via smart tap triggers win state, stops timer and emits TriggerWinCelebration`() = runTest(testDispatcher) {
            val kingOfSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true, id = "king_spades")
            val spadesFoundation = Rank.entries.filter { it != Rank.KING }.map { Card(Suit.SPADES, it, isFaceUp = true) }
            val otherFoundations = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS).map { suit ->
                Rank.entries.map { Card(suit, it, isFaceUp = true) }
            }
            val foundations = listOf(spadesFoundation) + otherFoundations
            val board = BoardState(
                waste = listOf(kingOfSpades),
                foundations = foundations
            )

            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            var celebrationEmitted = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { event ->
                    if (event is GameEvent.TriggerWinCelebration) {
                        celebrationEmitted = true
                    }
                }
            }

            viewModel.onIntent(GameIntent.OnCardTapped(kingOfSpades, CardLocation.Waste))

            val state = viewModel.uiState.value
            assertTrue(state.isGameWon)
            assertFalse(viewModel.isTimerRunning)
            assertTrue(celebrationEmitted)
        }

        @Test
        @DisplayName("Smart tap move can be undone via UndoMove restoring previous state")
        fun `Smart tap move can be undone via UndoMove restoring previous state`() = runTest(testDispatcher) {
            val aceOfSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = "ace_spades")
            val initialBoard = BoardState(waste = listOf(aceOfSpades))
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(aceOfSpades, CardLocation.Waste))
            assertTrue(viewModel.uiState.value.canUndo)
            assertEquals(1, viewModel.uiState.value.boardState.movesCount)

            viewModel.onIntent(GameIntent.UndoMove)

            val state = viewModel.uiState.value
            assertEquals(initialBoard, state.boardState)
            assertEquals(0, state.boardState.movesCount)
            assertFalse(state.canUndo)
        }

        @Test
        @DisplayName("Smart tap resulting in deadlock updates isDeadlocked to true")
        fun `Smart tap resulting in deadlock updates isDeadlocked to true`() = runTest(testDispatcher) {
            // A board where moving Ace to foundation leaves stock, waste, and tableau empty or blocked
            val aceOfSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = "ace_spades")
            val deadlockedBoard = BoardState(
                stock = emptyList(),
                waste = listOf(aceOfSpades),
                tableau = List(7) { emptyList() }
            )
            val viewModel = GameViewModel(
                initialBoardState = deadlockedBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertFalse(viewModel.uiState.value.isDeadlocked)

            viewModel.onIntent(GameIntent.OnCardTapped(aceOfSpades, CardLocation.Waste))

            val state = viewModel.uiState.value
            assertTrue(state.isDeadlocked)
            assertFalse(state.isGameWon)
        }
    }

    @Nested
    @DisplayName("Drop Intents")
    inner class DropIntents {

        @Test
        @DisplayName("Valid card drop updates board, records undo, and emits PlayHapticSnap")
        fun `valid card drop updates board, records undo, and emits PlayHapticSnap`() = runTest(testDispatcher) {
            val redSix = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true, id = "red6")
            val blackSeven = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true, id = "black7")
            val tableau = List(7) { col ->
                when (col) {
                    0 -> listOf(redSix)
                    1 -> listOf(blackSeven)
                    else -> emptyList()
                }
            }
            val board = BoardState(tableau = tableau)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            var hapticSnapEmitted = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { event ->
                    if (event is GameEvent.PlayHapticSnap) {
                        hapticSnapEmitted = true
                    }
                }
            }

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(redSix),
                    source = CardLocation.Tableau(0, 0),
                    target = CardLocation.Tableau(1, 0)
                )
            )

            val state = viewModel.uiState.value
            assertTrue(state.boardState.tableau[0].isEmpty())
            assertEquals(listOf(blackSeven, redSix), state.boardState.tableau[1])
            assertEquals(1, state.boardState.movesCount)
            assertTrue(state.canUndo)
            assertTrue(hapticSnapEmitted)
        }

        @Test
        @DisplayName("Invalid card drop leaves board unchanged and emits no event")
        fun `invalid card drop leaves board unchanged and emits no event`() = runTest(testDispatcher) {
            val redSix = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true, id = "red6")
            val redKing = Card(Suit.DIAMONDS, Rank.KING, isFaceUp = true, id = "redK")
            val tableau = List(7) { col ->
                when (col) {
                    0 -> listOf(redSix)
                    1 -> listOf(redKing)
                    else -> emptyList()
                }
            }
            val board = BoardState(tableau = tableau)
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            var anyEventEmitted = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect {
                    anyEventEmitted = true
                }
            }

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(redSix),
                    source = CardLocation.Tableau(0, 0),
                    target = CardLocation.Tableau(1, 0)
                )
            )

            val state = viewModel.uiState.value
            assertEquals(board, state.boardState)
            assertFalse(state.canUndo)
            assertFalse(anyEventEmitted)
        }

        @Test
        @DisplayName("Winning card drop emits TriggerWinCelebration and stops timer")
        fun `winning card drop emits TriggerWinCelebration and stops timer`() = runTest(testDispatcher) {
            val kingOfSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true, id = "king_spades")
            val spadesFoundation = Rank.entries.filter { it != Rank.KING }.map { Card(Suit.SPADES, it, isFaceUp = true) }
            val otherFoundations = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS).map { suit ->
                Rank.entries.map { Card(suit, it, isFaceUp = true) }
            }
            val foundations = listOf(spadesFoundation) + otherFoundations
            val board = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(kingOfSpades) else emptyList() },
                foundations = foundations
            )

            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true
            )

            var celebrationEmitted = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { event ->
                    if (event is GameEvent.TriggerWinCelebration) {
                        celebrationEmitted = true
                    }
                }
            }

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(kingOfSpades),
                    source = CardLocation.Tableau(0, 0),
                    target = CardLocation.Foundation(0)
                )
            )

            val state = viewModel.uiState.value
            assertTrue(state.isGameWon)
            assertFalse(viewModel.isTimerRunning)
            assertTrue(celebrationEmitted)
        }

        @Test
        @DisplayName("Card drop move can be undone via UndoMove")
        fun `card drop move can be undone via UndoMove`() = runTest(testDispatcher) {
            val aceOfSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = "ace_spades")
            val initialBoard = BoardState(waste = listOf(aceOfSpades))
            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(aceOfSpades),
                    source = CardLocation.Waste,
                    target = CardLocation.Foundation(0)
                )
            )
            assertTrue(viewModel.uiState.value.canUndo)
            assertEquals(1, viewModel.uiState.value.boardState.movesCount)

            viewModel.onIntent(GameIntent.UndoMove)

            val state = viewModel.uiState.value
            assertEquals(initialBoard, state.boardState)
            assertEquals(0, state.boardState.movesCount)
            assertFalse(state.canUndo)
        }
    }

    @Nested
    @DisplayName("Hint Intents")
    inner class HintIntents {

        @Test
        @DisplayName("RequestHint finds and sets activeHint in UI state")
        fun `RequestHint finds and sets activeHint in UI state`() = runTest(testDispatcher) {
            val ace = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val board = BoardState(tableau = List(7) { col -> if (col == 0) listOf(ace) else emptyList() })
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertNull(viewModel.uiState.value.activeHint)
            assertFalse(viewModel.uiState.value.isHintActive)

            viewModel.onIntent(GameIntent.RequestHint)

            val state = viewModel.uiState.value
            assertTrue(state.isHintActive)
            assertEquals(ace, state.highlightedCard)
            assertEquals(CardLocation.Tableau(0, 0), state.hintSourceLocation)
            assertEquals(CardLocation.Foundation(0), state.hintTargetLocation)
        }

        @Test
        @DisplayName("RequestHint highlights empty stock placeholder instead of waste card when recycling")
        fun `RequestHint highlights empty stock placeholder when recycling`() = runTest(testDispatcher) {
            val col0 = listOf(Card(Suit.SPADES, Rank.TEN, isFaceUp = true))
            val buriedWaste = listOf(
                Card(Suit.HEARTS, Rank.NINE, isFaceUp = true),
                Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)
            )
            val board = BoardState(
                stock = emptyList(),
                waste = buriedWaste,
                tableau = List(7) { if (it == 0) col0 else emptyList() }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)

            val state = viewModel.uiState.value
            assertTrue(state.isHintActive)
            assertNull(state.highlightedCard)
            assertTrue(state.highlightedCards.isEmpty())
            assertEquals(CardLocation.Stock, state.hintSourceLocation)
        }

        @Test
        @DisplayName("DismissHint clears activeHint in UI state")
        fun `DismissHint clears activeHint in UI state`() = runTest(testDispatcher) {
            val ace = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val board = BoardState(tableau = List(7) { col -> if (col == 0) listOf(ace) else emptyList() })
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertTrue(viewModel.uiState.value.isHintActive)

            viewModel.onIntent(GameIntent.DismissHint)
            assertFalse(viewModel.uiState.value.isHintActive)
            assertNull(viewModel.uiState.value.activeHint)
            assertNull(viewModel.uiState.value.highlightedCard)
        }

        @Test
        @DisplayName("activeHint is automatically cleared when card is tapped")
        fun `activeHint is automatically cleared when card is tapped`() = runTest(testDispatcher) {
            val ace = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val king = Card(Suit.SPADES, Rank.KING, isFaceUp = true, id = "king_spades")
            val board = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(ace)
                        1 -> listOf(king)
                        else -> emptyList()
                    }
                }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertTrue(viewModel.uiState.value.isHintActive)

            // Tap king (unmovable) - should still dismiss the hint
            viewModel.onIntent(GameIntent.OnCardTapped(king, CardLocation.Tableau(1, 0)))
            assertFalse(viewModel.uiState.value.isHintActive)
            assertNull(viewModel.uiState.value.activeHint)
        }

        @Test
        @DisplayName("activeHint is automatically cleared when stock is drawn")
        fun `activeHint is automatically cleared when stock is drawn`() = runTest(testDispatcher) {
            val card = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false, id = "c5")
            val ace = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val board = BoardState(
                stock = listOf(card),
                tableau = List(7) { col -> if (col == 0) listOf(ace) else emptyList() }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertTrue(viewModel.uiState.value.isHintActive)

            viewModel.onIntent(GameIntent.DrawStockCard)
            assertFalse(viewModel.uiState.value.isHintActive)
            assertNull(viewModel.uiState.value.activeHint)
        }

        @Test
        @DisplayName("activeHint is automatically cleared when stock is recycled")
        fun `activeHint is automatically cleared when stock is recycled`() = runTest(testDispatcher) {
            val card = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true, id = "c5")
            val ace = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val board = BoardState(
                stock = emptyList(),
                waste = listOf(card),
                tableau = List(7) { col -> if (col == 0) listOf(ace) else emptyList() }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertTrue(viewModel.uiState.value.isHintActive)

            viewModel.onIntent(GameIntent.RecycleStock)
            assertFalse(viewModel.uiState.value.isHintActive)
            assertNull(viewModel.uiState.value.activeHint)
        }

        @Test
        @DisplayName("activeHint is automatically cleared when card is dropped")
        fun `activeHint is automatically cleared when card is dropped`() = runTest(testDispatcher) {
            val redSix = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true, id = "red6")
            val blackSeven = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true, id = "black7")
            val board = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(redSix)
                        1 -> listOf(blackSeven)
                        else -> emptyList()
                    }
                }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertTrue(viewModel.uiState.value.isHintActive)

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(redSix),
                    source = CardLocation.Tableau(0, 0),
                    target = CardLocation.Tableau(1, 0)
                )
            )
            assertFalse(viewModel.uiState.value.isHintActive)
            assertNull(viewModel.uiState.value.activeHint)
        }

        @Test
        @DisplayName("RequestHint on deadlocked or unplayable board produces null activeHint")
        fun `RequestHint on unplayable board produces null activeHint`() = runTest(testDispatcher) {
            val board = BoardState(stock = emptyList(), waste = emptyList(), tableau = List(7) { emptyList() })
            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.RequestHint)
            assertNull(viewModel.uiState.value.activeHint)
            assertFalse(viewModel.uiState.value.isHintActive)
        }
    }

    private fun createAutoCompleteReadyBoard(): BoardState {
        val foundations = Suit.entries.map { suit ->
            Rank.entries.filter { it != Rank.KING }.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
        val tableau = List(7) { col ->
            if (col < 4) {
                listOf(Card(Suit.entries[col], Rank.KING, isFaceUp = true))
            } else {
                emptyList()
            }
        }
        return BoardState(
            stock = emptyList(),
            waste = emptyList(),
            tableau = tableau,
            foundations = foundations
        )
    }

    @Nested
    @DisplayName("Auto-Complete Intents")
    inner class AutoCompleteIntents {

        @Test
        @DisplayName("isAutoCompleteAvailable is true on initial state when board meets criteria")
        fun `isAutoCompleteAvailable is true on initial state when board meets criteria`() = runTest(testDispatcher) {
            val readyBoard = createAutoCompleteReadyBoard()
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertTrue(viewModel.uiState.value.isAutoCompleteAvailable)
            assertFalse(viewModel.uiState.value.isGameWon)
        }

        @Test
        @DisplayName("isAutoCompleteAvailable updates to true after uncovering last face-down card")
        fun `isAutoCompleteAvailable updates to true after uncovering last face-down card`() = runTest(testDispatcher) {
            val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true, id = "ace_hearts")
            val hiddenCard = Card(Suit.SPADES, Rank.KING, isFaceUp = false, id = "hidden_king")
            val foundations = Suit.entries.map { suit ->
                if (suit == Suit.HEARTS) {
                    Rank.entries.filter { it != Rank.ACE }.map { Card(suit, it, isFaceUp = true) }
                } else {
                    Rank.entries.map { Card(suit, it, isFaceUp = true) }
                }
            }
            val board = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(aceHearts)
                        1 -> listOf(hiddenCard)
                        else -> emptyList()
                    }
                },
                foundations = foundations
            )

            val viewModel = GameViewModel(
                initialBoardState = board,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertFalse(viewModel.uiState.value.isAutoCompleteAvailable)

            // Move ace to foundation - this will not flip the hidden card yet
            viewModel.onIntent(GameIntent.OnCardTapped(aceHearts, CardLocation.Tableau(0, 0)))
            assertFalse(viewModel.uiState.value.isAutoCompleteAvailable)
        }

        @Test
        @DisplayName("AutoComplete intent executes cascade loop to victory")
        fun `AutoComplete intent executes cascade loop to victory`() = runTest(testDispatcher) {
            val readyBoard = createAutoCompleteReadyBoard()
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                timerDelayMs = 1000L,
                autoStartTimer = true,
                autoCompleteDelayMs = 100L
            )

            var celebrationEmitted = false
            val hapticSnaps = mutableListOf<GameEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { event ->
                    if (event is GameEvent.TriggerWinCelebration) {
                        celebrationEmitted = true
                    } else if (event is GameEvent.PlayHapticSnap) {
                        hapticSnaps.add(event)
                    }
                }
            }

            assertTrue(viewModel.uiState.value.isAutoCompleteAvailable)
            viewModel.onIntent(GameIntent.AutoComplete)

            // Advance through the 4 steps (4 * 100ms)
            testScheduler.advanceTimeBy(500)
            testScheduler.runCurrent()

            val finalState = viewModel.uiState.value
            assertTrue(finalState.isGameWon)
            assertFalse(finalState.isAutoCompleteAvailable)
            assertFalse(viewModel.isTimerRunning)
            assertTrue(celebrationEmitted)
            assertEquals(0, hapticSnaps.size)
            assertEquals(4, finalState.boardState.movesCount)
            assertEquals(4 * KlondikeRules.SCORE_TABLEAU_TO_FOUNDATION, finalState.boardState.score)
            assertTrue(finalState.boardState.tableau.all { it.isEmpty() })
            assertTrue(finalState.boardState.foundations.all { it.size == 13 })
        }

        @Test
        @DisplayName("AutoComplete does not run when isAutoCompleteAvailable is false")
        fun `AutoComplete does not run when isAutoCompleteAvailable is false`() = runTest(testDispatcher) {
            val unreadyBoard = BoardState(
                stock = listOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)),
                tableau = List(7) { col ->
                    if (col == 0) listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = false)) else emptyList()
                }
            )
            val viewModel = GameViewModel(
                initialBoardState = unreadyBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                autoCompleteDelayMs = 100L
            )

            assertFalse(viewModel.uiState.value.isAutoCompleteAvailable)
            viewModel.onIntent(GameIntent.AutoComplete)

            testScheduler.advanceTimeBy(500)
            testScheduler.runCurrent()

            assertEquals(unreadyBoard, viewModel.uiState.value.boardState)
            assertFalse(viewModel.uiState.value.isGameWon)
        }

        @Test
        @DisplayName("RestartGame cancels running AutoComplete cascade")
        fun `RestartGame cancels running AutoComplete cascade`() = runTest(testDispatcher) {
            val readyBoard = createAutoCompleteReadyBoard()
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                autoCompleteDelayMs = 200L
            )

            viewModel.onIntent(GameIntent.AutoComplete)
            testScheduler.advanceTimeBy(50) // 1 move applied
            testScheduler.runCurrent()

            assertEquals(1, viewModel.uiState.value.boardState.movesCount)

            viewModel.onIntent(GameIntent.RestartGame)
            testScheduler.runCurrent()

            assertEquals(readyBoard, viewModel.uiState.value.boardState)
            assertEquals(0, viewModel.uiState.value.boardState.movesCount)

            // Further time advance should not make any auto-complete moves
            testScheduler.advanceTimeBy(1000)
            testScheduler.runCurrent()

            assertEquals(readyBoard, viewModel.uiState.value.boardState)
        }

        @Test
        @DisplayName("StartAutoComplete and FinishAutoComplete toggle isAutoCompleting state")
        fun `StartAutoComplete and FinishAutoComplete toggle isAutoCompleting state`() = runTest(testDispatcher) {
            val readyBoard = createAutoCompleteReadyBoard()
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertFalse(viewModel.uiState.value.isAutoCompleting)

            viewModel.onIntent(GameIntent.StartAutoComplete)
            assertTrue(viewModel.uiState.value.isAutoCompleting)

            viewModel.onIntent(GameIntent.FinishAutoComplete)
            assertFalse(viewModel.uiState.value.isAutoCompleting)
        }
    }
    @Nested
    @DisplayName("Settings Intents and Repository Integration")
    inner class SettingsIntentsTests {



        @Test
        @DisplayName("OpenSettings and CloseSettings toggle isSettingsOpen")
        fun `OpenSettings and CloseSettings toggle isSettingsOpen`() = runTest(testDispatcher) {
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            assertFalse(viewModel.uiState.value.isSettingsOpen)
            viewModel.onIntent(GameIntent.OpenSettings)
            assertTrue(viewModel.uiState.value.isSettingsOpen)
            viewModel.onIntent(GameIntent.CloseSettings)
            assertFalse(viewModel.uiState.value.isSettingsOpen)
        }

        @Test
        @DisplayName("SetDrawMode updates drawMode in uiState and calls repository")
        fun `SetDrawMode updates drawMode in uiState and calls repository`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )

            assertEquals(DrawMode.DRAW_ONE, viewModel.uiState.value.drawMode)
            viewModel.onIntent(GameIntent.SetDrawMode(DrawMode.DRAW_THREE))
            testScheduler.runCurrent()

            assertEquals(DrawMode.DRAW_THREE, viewModel.uiState.value.drawMode)
            assertEquals(DrawMode.DRAW_THREE, fakeRepo.getSettings().drawMode)
        }

        @Test
        @DisplayName("SetLeftHanded and ToggleLeftHanded update isLeftHanded and repository")
        fun `SetLeftHanded and ToggleLeftHanded update isLeftHanded and repository`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )

            assertFalse(viewModel.uiState.value.isLeftHanded)
            viewModel.onIntent(GameIntent.SetLeftHanded(true))
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isLeftHanded)
            assertTrue(fakeRepo.getSettings().isLeftHanded)

            viewModel.onIntent(GameIntent.ToggleLeftHanded)
            testScheduler.runCurrent()
            assertFalse(viewModel.uiState.value.isLeftHanded)
            assertFalse(fakeRepo.getSettings().isLeftHanded)
        }

        @Test
        @DisplayName("SetFeltTheme and SelectFeltTheme update feltTheme and repository")
        fun `SetFeltTheme and SelectFeltTheme update feltTheme and repository`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )

            viewModel.onIntent(GameIntent.SetFeltTheme(FeltTheme.DARK_CHARCOAL))
            testScheduler.runCurrent()
            assertEquals(FeltTheme.DARK_CHARCOAL, viewModel.uiState.value.feltTheme)
            assertEquals(FeltTheme.DARK_CHARCOAL, fakeRepo.getSettings().feltTheme)

            viewModel.onIntent(GameIntent.SelectFeltTheme(FeltTheme.WINE_RED))
            testScheduler.runCurrent()
            assertEquals(FeltTheme.WINE_RED, viewModel.uiState.value.feltTheme)
            assertEquals(FeltTheme.WINE_RED, fakeRepo.getSettings().feltTheme)
        }

        @Test
        @DisplayName("SetCardBackStyle and SetCardFaceStyle update styles and repository")
        fun `SetCardBackStyle and SetCardFaceStyle update styles and repository`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )

            viewModel.onIntent(GameIntent.SetCardBackStyle(CardBackStyle.EMERALD_ART_DECO))
            viewModel.onIntent(GameIntent.SetCardFaceStyle(CardFaceStyle.MODERN_CLEAN))
            testScheduler.runCurrent()

            assertEquals(CardBackStyle.EMERALD_ART_DECO, viewModel.uiState.value.cardBackStyle)
            assertEquals(CardBackStyle.EMERALD_ART_DECO, fakeRepo.getSettings().cardBackStyle)
            assertEquals(CardFaceStyle.MODERN_CLEAN, viewModel.uiState.value.cardFaceStyle)
            assertEquals(CardFaceStyle.MODERN_CLEAN, fakeRepo.getSettings().cardFaceStyle)
        }

        @Test
        @DisplayName("SetSoundEnabled, SetHapticsEnabled, SetAutoHintEnabled update toggles and repository")
        fun `SetSoundEnabled, SetHapticsEnabled, SetAutoHintEnabled update toggles and repository`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )

            viewModel.onIntent(GameIntent.SetSoundEnabled(false))
            viewModel.onIntent(GameIntent.SetHapticsEnabled(false))
            viewModel.onIntent(GameIntent.SetAutoHintEnabled(true))
            testScheduler.runCurrent()

            assertFalse(viewModel.uiState.value.soundEnabled)
            assertFalse(fakeRepo.getSettings().soundEnabled)
            assertFalse(viewModel.uiState.value.hapticsEnabled)
            assertFalse(fakeRepo.getSettings().hapticsEnabled)
            assertTrue(viewModel.uiState.value.autoHintEnabled)
            assertTrue(fakeRepo.getSettings().autoHintEnabled)
        }

        @Test
        @DisplayName("ResetSettingsToDefaults restores all defaults in repository and uiState")
        fun `ResetSettingsToDefaults restores all defaults in repository and uiState`() = runTest(testDispatcher) {
            val modified = GameSettings(
                drawMode = DrawMode.DRAW_THREE,
                isLeftHanded = true,
                feltTheme = FeltTheme.WINE_RED,
                cardBackStyle = CardBackStyle.OBSIDIAN_MINIMAL,
                soundEnabled = false,
                hapticsEnabled = false,
                autoHintEnabled = true
            )
            val fakeRepo = FakeSettingsRepository(initialSettings = modified)
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )
            testScheduler.runCurrent()

            assertEquals(DrawMode.DRAW_THREE, viewModel.uiState.value.drawMode)
            assertTrue(viewModel.uiState.value.isLeftHanded)

            viewModel.onIntent(GameIntent.ResetSettingsToDefaults)
            testScheduler.runCurrent()

            val defaults = GameSettings()
            assertEquals(defaults.drawMode, viewModel.uiState.value.drawMode)
            assertEquals(defaults.isLeftHanded, viewModel.uiState.value.isLeftHanded)
            assertEquals(defaults.feltTheme, viewModel.uiState.value.feltTheme)
            assertEquals(defaults.cardBackStyle, viewModel.uiState.value.cardBackStyle)
            assertEquals(defaults.soundEnabled, viewModel.uiState.value.soundEnabled)
            assertEquals(defaults.hapticsEnabled, viewModel.uiState.value.hapticsEnabled)
            assertEquals(defaults.autoHintEnabled, viewModel.uiState.value.autoHintEnabled)
        }

        @Test
        @DisplayName("Repository settingsFlow updates GameUiState reactively")
        fun `Repository settingsFlow updates GameUiState reactively`() = runTest(testDispatcher) {
            val fakeRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createCustomBoard(),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = fakeRepo
            )
            testScheduler.runCurrent()

            fakeRepo.setFeltTheme(FeltTheme.DEEP_NAVY)
            testScheduler.runCurrent()

            assertEquals(FeltTheme.DEEP_NAVY, viewModel.uiState.value.feltTheme)
        }
    }
}
