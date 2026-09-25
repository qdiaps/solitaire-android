package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.domain.deck.Deck
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.solver.DealGenerator
import io.github.qdiaps.solitaire.domain.solver.SolvabilityResult
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
            assertEquals("deal_1", viewModel.uiState.value.boardState.stock.first().id)

            viewModel.onIntent(GameIntent.StartNewGame)
            testScheduler.runCurrent()

            assertEquals("deal_2", viewModel.uiState.value.boardState.stock.first().id)
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

            viewModel.onIntent(GameIntent.RestartGame)
            testScheduler.runCurrent()

            assertEquals(initialBoard, viewModel.uiState.value.boardState)
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
    @DisplayName("Solvable Deal Generator Integration")
    inner class SolvableDealGeneratorIntegration {

        @Test
        @DisplayName("StartNewGame retrieves deal from DealGenerator when present")
        fun `StartNewGame retrieves deal from DealGenerator when present`() = runTest(testDispatcher) {
            val solvableBoard = createCustomBoard("solvable_seed")
            val generator = DealGenerator(
                scope = backgroundScope,
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
}
