package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SolitaireGameScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    private val cardAceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val cardTwoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)
    private val cardThreeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("End-to-End Game Screen & ViewModel Wiring")
    inner class EndToEndGameplayTests {

        @Test
        fun `draw card intent moves card from stock to waste and emits haptic event`() = runTest(testDispatcher) {
            val board = BoardState(
                stock = listOf(cardAceHearts, cardTwoClubs),
                waste = emptyList()
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val emittedEvents = mutableListOf<GameEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { emittedEvents.add(it) }
            }

            viewModel.onIntent(GameIntent.DrawStockCard)
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(1, uiState.boardState.stock.size)
            assertEquals(1, uiState.boardState.waste.size)
            assertEquals(cardAceHearts, uiState.boardState.waste.last())
            assertTrue(uiState.canUndo)

            assertEquals(listOf(GameEvent.PlayHapticTick), emittedEvents)
            eventJob.cancel()
        }

        @Test
        fun `smart tap on waste card promotes to foundation`() = runTest(testDispatcher) {
            val board = BoardState(
                waste = listOf(cardAceHearts),
                foundations = List(4) { emptyList() }
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.OnCardTapped(cardAceHearts, CardLocation.Waste))
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertTrue(uiState.boardState.waste.isEmpty())
            assertEquals(listOf(cardAceHearts), uiState.boardState.foundations[0])
            assertEquals(10, uiState.boardState.score)
            assertTrue(uiState.canUndo)
        }

        @Test
        fun `drag and drop card from waste to tableau moves stack and emits haptic snap`() = runTest(testDispatcher) {
            val board = BoardState(
                waste = listOf(cardTwoClubs),
                tableau = listOf(
                    listOf(cardThreeHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val emittedEvents = mutableListOf<GameEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { emittedEvents.add(it) }
            }

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(cardTwoClubs),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertTrue(uiState.boardState.waste.isEmpty())
            assertEquals(listOf(cardThreeHearts, cardTwoClubs), uiState.boardState.tableau[0])
            assertEquals(5, uiState.boardState.score)
            assertTrue(uiState.canUndo)

            assertEquals(listOf(GameEvent.PlayHapticSnap), emittedEvents)
            eventJob.cancel()
        }

        @Test
        fun `undo after drag-and-drop restores previous board and decrements score`() = runTest(testDispatcher) {
            val board = BoardState(
                waste = listOf(cardTwoClubs),
                tableau = listOf(
                    listOf(cardThreeHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )
            val viewModel = GameViewModel(
                initialBoardState = board,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(
                GameIntent.OnCardDropped(
                    cards = listOf(cardTwoClubs),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(0)
                )
            )
            testScheduler.advanceUntilIdle()
            assertEquals(5, viewModel.uiState.value.boardState.score)

            viewModel.onIntent(GameIntent.UndoMove)
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(listOf(cardTwoClubs), uiState.boardState.waste)
            assertEquals(listOf(cardThreeHearts), uiState.boardState.tableau[0])
            assertEquals(0, uiState.boardState.score)
            assertFalse(uiState.canUndo)
        }

        @Test
        fun `new game intent deals fresh shuffled board, resets timer, and emits deal sound event`() = runTest(testDispatcher) {
            val initialBoard = KlondikeDealer.dealShuffled(Random(1))
            val freshBoard = KlondikeDealer.dealShuffled(Random(2))

            val viewModel = GameViewModel(
                initialBoardState = initialBoard,
                dealProvider = { freshBoard },
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val emittedEvents = mutableListOf<GameEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { emittedEvents.add(it) }
            }

            assertEquals(initialBoard, viewModel.uiState.value.boardState)

            viewModel.onIntent(GameIntent.StartNewGame)
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(freshBoard, uiState.boardState)
            assertEquals(0L, uiState.elapsedTimeSeconds)
            assertFalse(uiState.canUndo)
            assertEquals(listOf(GameEvent.PlayDealSound), emittedEvents)
            eventJob.cancel()
        }
    
        @Test
        fun `auto-complete intent cascades moves to foundations and triggers win celebration event`() = runTest(testDispatcher) {
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
            val readyBoard = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = foundations,
                tableau = tableau
            )
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                autoCompleteDelayMs = 50L
            )

            val emittedEvents = mutableListOf<GameEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { emittedEvents.add(it) }
            }

            assertTrue(viewModel.uiState.value.isAutoCompleteAvailable)

            viewModel.onIntent(GameIntent.AutoComplete)
            testScheduler.advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertTrue(uiState.isGameWon)
            assertFalse(uiState.isAutoCompleteAvailable)
            assertEquals(4, uiState.boardState.movesCount)
            assertTrue(uiState.boardState.tableau.all { it.isEmpty() })

            // 4 PlayHapticSnap + 1 TriggerWinCelebration
            val snapCount = emittedEvents.count { it is GameEvent.PlayHapticSnap }
            val celebrationCount = emittedEvents.count { it is GameEvent.TriggerWinCelebration }
            assertEquals(0, snapCount)
            assertEquals(1, celebrationCount)

            eventJob.cancel()
        }

        @Test
        fun `step by step auto complete loop advances state sequentially with unique cards`() = runTest(testDispatcher) {
            val suits = Suit.entries
            val foundations = suits.map { suit ->
                Rank.entries.filter { it != Rank.KING }.map { rank ->
                    Card(suit, rank, isFaceUp = true)
                }
            }
            val kings = suits.map { suit -> Card(suit, Rank.KING, isFaceUp = true) }
            val tableau = List(7) { index ->
                if (index < 4) listOf(kings[index]) else emptyList()
            }
            val readyBoard = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = foundations,
                tableau = tableau
            )
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            viewModel.onIntent(GameIntent.StartAutoComplete)
            assertTrue(viewModel.uiState.value.isAutoCompleting)

            var current = viewModel.uiState.value.boardState
            val animatedCards = mutableListOf<Card>()

            while (true) {
                val move = io.github.qdiaps.solitaire.domain.rules.AutoCompleteResolver.nextMove(current) ?: break
                animatedCards.add(move.card)
                current = move.resultingState
                viewModel.onIntent(GameIntent.ApplyAutoCompleteMove(move))
            }

            viewModel.onIntent(GameIntent.FinishAutoComplete)

            // Exactly 4 kings moved, each card animated exactly once
            assertEquals(4, animatedCards.size)
            assertEquals(animatedCards.size, animatedCards.distinct().size)
            assertTrue(viewModel.uiState.value.isGameWon)
            assertFalse(viewModel.uiState.value.isAutoCompleting)
        }

        @Test
        fun `victory sequence activates animation, supports skip to dialog, and handles dialog intents`() = runTest(testDispatcher) {
            val suits = Suit.entries
            val foundations = suits.map { suit ->
                Rank.entries.filter { it != Rank.KING }.map { rank ->
                    Card(suit, rank, isFaceUp = true)
                }
            }
            val kings = suits.map { suit -> Card(suit, Rank.KING, isFaceUp = true) }
            val tableau = List(7) { index ->
                if (index < 4) listOf(kings[index]) else emptyList()
            }
            val readyBoard = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = foundations,
                tableau = tableau,
                score = 700,
                movesCount = 80
            )
            val viewModel = GameViewModel(
                initialBoardState = readyBoard,
                coroutineScope = this,
                timerDispatcher = testDispatcher,
                autoStartTimer = false
            )

            val emittedEvents = mutableListOf<GameEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { emittedEvents.add(it) }
            }

            viewModel.onIntent(GameIntent.AutoComplete)
            testScheduler.advanceUntilIdle()

            val wonState = viewModel.uiState.value
            assertTrue(wonState.isGameWon)
            assertTrue(wonState.isVictoryAnimationActive)
            assertNotNull(wonState.victorySummary)
            assertTrue(emittedEvents.any { it is GameEvent.TriggerWinCelebration })

            viewModel.onIntent(GameIntent.SkipWinAnimation)
            testScheduler.advanceUntilIdle()

            val skippedState = viewModel.uiState.value
            assertFalse(skippedState.isVictoryAnimationActive)
            assertNotNull(skippedState.victorySummary)

            viewModel.onIntent(GameIntent.DismissVictorySummary)
            testScheduler.advanceUntilIdle()

            val dismissedState = viewModel.uiState.value
            assertTrue(dismissedState.isGameWon)
            assertFalse(dismissedState.isVictoryAnimationActive)
            assertEquals(null, dismissedState.victorySummary)

            viewModel.onIntent(GameIntent.RestartGame)
            testScheduler.advanceUntilIdle()

            val restartedState = viewModel.uiState.value
            assertFalse(restartedState.isGameWon)
            assertFalse(restartedState.isVictoryAnimationActive)
            assertEquals(null, restartedState.victorySummary)

            eventJob.cancel()
        }
    }
}
