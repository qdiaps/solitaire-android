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
    }
}
